package com.tynsolutions.gestionaveriasmovil.data.repository

import android.util.Log
import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.CambiarEstadoMaquinaRequest
import com.tynsolutions.gestionaveriasmovil.data.network.dto.IntervencionRequestDTO
import com.tynsolutions.gestionaveriasmovil.data.network.mapper.toDomain
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Repositorio de Dominio para la orquestación del ciclo de vida de las averías.
 * Actúa como única fuente de la verdad (Single Source of Truth), encapsulando
 * la complejidad de la red y garantizando el cumplimiento de las políticas
 * de seguridad transaccional antes de emitir datos a la capa de Presentación.
 */
class AveriasRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "AveriasRepository_Sec"
    }

    /**
     * Consulta el catálogo de averías asignadas al operario actual.
     * Implementa un mecanismo Fail-fast para validar la integridad de la sesión
     * localmente, previniendo peticiones anómalas al servidor perimetral.
     *
     * @param tipo Filtro de estado operativo ("nuevas", "en_curso", "historico").
     * @return [Result] encapsulando la colección de dominio [Averia] o un error sanitizado.
     */
    suspend fun getAveriasAsignadas(tipo: String = "nuevas"): Result<List<Averia>> {
        return try {
            val token = sessionManager.fetchAuthToken()?.trim()
            val userId = sessionManager.fetchUserId()

            // Fail-fast: Auditoría de estado de sesión
            if (token.isNullOrEmpty() || userId == -1) {
                return Result.failure(Exception("Sesión de seguridad inválida o expirada. Es necesario reautenticarse."))
            }

            val response = apiService.getAveriasTecnico(userId, tipo)

            if (response.isSuccessful) {
                val dtoList = response.body()?.data ?: emptyList()
                val domainList = dtoList.map { it.toDomain() }
                Result.success(domainList)
            } else {
                Log.w(TAG, "Denegación de acceso o error remoto. HTTP: ${response.code()} - ${response.errorBody()?.string()}")
                Result.failure(Exception("Fallo de sincronización con el servidor. Código HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo catastrófico de red/parseo al recuperar lista", e)
            Result.failure(Exception("Error crítico de conectividad. Verifique su acceso a la red."))
        }
    }

    /**
     * Recupera y transforma la entidad completa de una avería específica.
     *
     * @param id Identificador primario de la avería.
     * @return [Result] con el modelo de dominio [Averia] rehidratado.
     */
    suspend fun getAveriaPorId(id: Int): Result<Averia> {
        return try {
            val response = apiService.getAveriaDetalle(id)
            val dto = response.body()?.data

            if (response.isSuccessful && dto != null) {
                Result.success(dto.toDomain())
            } else {
                Log.w(TAG, "Payload inválido o vacío para la avería ID: $id")
                Result.failure(Exception("Inconsistencia de datos: El servidor no proporcionó una entidad válida."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Interrupción de red al consultar detalle de avería $id", e)
            Result.failure(Exception("Fallo de red al intentar recuperar los detalles de la avería."))
        }
    }

    /**
     * Transición de estado (PUT): Marca formalmente la incidencia como "En curso".
     *
     * @param idAveria Identificador de la avería objetivo.
     * @return [Result] indicando la resolución de la transacción.
     */
    suspend fun aceptarAveria(idAveria: Int): Result<String> {
        return try {
            val response = apiService.aceptarAveria(idAveria)
            if (response.isSuccessful) {
                Result.success("La avería ha sido aceptada formalmente.")
            } else {
                Log.w(TAG, "Rechazo de servidor al aceptar avería. HTTP: ${response.code()}")
                Result.failure(Exception("Operación denegada por el servidor. Código: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Timeout/Error de red durante la firma de aceptación", e)
            Result.failure(Exception("Interrupción de red durante la firma de aceptación."))
        }
    }

    /**
     * Mutación de datos (PUT): Anexa un registro descriptivo al historial de la avería.
     *
     * @param idAveria Identificador de la avería.
     * @param proceso Descripción detallada del procedimiento técnico.
     * @return [Result] Unit indicando éxito transaccional.
     */
    suspend fun registrarIntervencion(idAveria: Int, proceso: String): Result<Unit> {
        return try {
            val request = IntervencionRequestDTO(proceso)
            val response = apiService.registrarIntervencion(idAveria, request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.w(TAG, "Fallo al registrar intervención. HTTP: ${response.code()}")
                Result.failure(Exception("Error de servidor al registrar la intervención. Código: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red al registrar intervención", e)
            Result.failure(Exception("Fallo de conectividad al enviar el registro de intervención."))
        }
    }

    /**
     * Cierre de ciclo de vida (PUT): Clausura la incidencia de forma irreversible.
     * Implementa parseo de reglas de negocio para auditoría de requisitos previos.
     *
     * @param idAveria Identificador de la avería a finalizar.
     * @return [Result] confirmando el archivo del registro o detallando el incumplimiento.
     */
    suspend fun finalizarAveria(idAveria: Int): Result<String> {
        return try {
            val response = apiService.finalizarAveria(idAveria)

            if (response.isSuccessful) {
                Result.success("Ciclo de vida de la avería finalizado y archivado.")
            } else {
                if (response.code() == 400) {
                    Result.failure(Exception("Requisito incumplido: Es obligatorio registrar una intervención antes del cierre."))
                } else {
                    Log.w(TAG, "Excepción remota al finalizar avería. HTTP: ${response.code()}")
                    Result.failure(Exception("Fallo en el servidor al intentar finalizar. Código: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de conectividad en finalización de avería", e)
            Result.failure(Exception("Timeout de red al ejecutar la orden de finalización."))
        }
    }

    /**
     * Sincronización de estado físico (PUT): Actualiza el estatus operativo del hardware.
     *
     * @param idMaquinaria Identificador del equipo físico.
     * @param codigoEstado Código de catálogo representativo del nuevo estado.
     * @return [Result] confirmando la mutación en base de datos.
     */
    suspend fun cambiarEstadoMaquinaria(idMaquinaria: Int, codigoEstado: Int): Result<String> {
        return try {
            val request = CambiarEstadoMaquinaRequest(codigoEstado)
            val response = apiService.cambiarEstadoMaquina(idMaquinaria, request)

            if (response.isSuccessful) {
                Result.success("El estado de la maquinaria ha sido sincronizado correctamente.")
            } else {
                Log.w(TAG, "Denegación de mutación de estado de maquinaria. HTTP: ${response.code()}")
                Result.failure(Exception("Operación denegada por el servidor. Código HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Caída de red sincronizando maquinaria", e)
            Result.failure(Exception("Error de conectividad al intentar sincronizar el estado de la máquina."))
        }
    }
}