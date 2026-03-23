package com.tynsolutions.gestionaveriasmovil.data.repository

import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.IntervencionRequestDTO
import com.tynsolutions.gestionaveriasmovil.data.network.mapper.toDomain
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Repositorio centralizado para la gestión del ciclo de vida de las averías.
 * Actúa como única fuente de la verdad (Single Source of Truth) para la capa de dominio,
 * abstrayendo a los ViewModels de la complejidad asíncrona de la red y garantizando
 * el cumplimiento de las normativas de seguridad en cada transacción.
 */
class AveriasRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    // ==========================================
    // FASE 3: CONSULTA DE DATOS (READ)
    // ==========================================

    /**
     * Solicita al servidor la lista de averías asignadas al técnico logueado.
     * Gestiona internamente la verificación local de la sesión antes de consumir ancho de banda.
     * @param tipo Filtro de estado ("nuevas", "en_curso", "historico").
     * @return Result encapsulando una lista de [Averia] (Dominio) en caso de éxito,
     * o una excepción detallada lista para ser consumida por la UI.
     */
    suspend fun getAveriasAsignadas(tipo: String = "nuevas"): Result<List<Averia>> {
        return try {
            val token = sessionManager.fetchAuthToken()?.trim()
            val userId = sessionManager.fetchUserId()

            // Fail-fast: Rechazamos la ejecución localmente si la identidad está comprometida
            if (token.isNullOrEmpty() || userId == -1) {
                return Result.failure(Exception("Sesión de seguridad inválida o expirada. Es necesario reautenticarse."))
            }

            val response = apiService.getAveriasTecnico(userId, tipo)

            if (response.isSuccessful) {
                // Extraemos el payload inestable
                val dtoList = response.body()?.data ?: emptyList()

                // Mapeo crítico: Transformamos el contrato de red en modelos de negocio seguros.
                val domainList = dtoList.map { it.toDomain() }

                Result.success(domainList)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("API_SECURITY", "Denegación de acceso o error remoto. HTTP: ${response.code()} - $errorBody")
                Result.failure(Exception("Fallo de sincronización. Código HTTP: ${response.code()}"))
            }

        } catch (e: Exception) {
            android.util.Log.e("API_CRITICAL", "Fallo catastrófico de red/parseo: ${e.message}")
            Result.failure(Exception("Error crítico de conectividad con el centro de datos."))
        }
    }

    // ==========================================
    // FASE 4: TRANSACCIONES Y MODIFICACIONES (UPDATE)
    // ==========================================

    /**
     * Solicita los datos frescos de una avería única y los transforma al modelo de dominio seguro.
     * @param id Identificador único de la avería a consultar.
     * @return Result con la entidad [Averia] procesada o excepción de red.
     */
    suspend fun getAveriaPorId(id: Int): Result<Averia> {
        return try {
            val response = apiService.getAveriaDetalle(id)
            // Extraemos la avería del interior del campo 'data'
            val dto = response.body()?.data

            if (response.isSuccessful && dto != null) {
                // Mapeo seguro al modelo de dominio para la UI
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception("Error de sincronización: El servidor no envió datos válidos."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transiciona el estado de la avería a "En curso" (PUT).
     * La seguridad y autorización (JWT) se delegan al Interceptor de OkHttp configurado en el cliente.
     * @param idAveria Identificador único de la avería a modificar.
     * @return Result con mensaje de éxito o detalle del error de negocio (ej. 409 Conflict).
     */
    suspend fun aceptarAveria(idAveria: Int): Result<String> {
        return try {
            val response = apiService.aceptarAveria(idAveria)
            if (response.isSuccessful) {
                Result.success("La avería ha sido aceptada formalmente.")
            } else {
                Result.failure(Exception("Operación denegada por el servidor. Código: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error de red", e)
            Result.failure(Exception("Interrupción de red durante la firma de aceptación."))
        }
    }

    /**
     * Persiste el informe de intervención técnica en la base de datos (PUT).
     * @param idAveria Identificador único de la avería.
     * @param proceso Descripción detallada de las acciones realizadas por el técnico.
     * @return Result indicando el éxito de la escritura.
     */
    suspend fun registrarIntervencion(idAveria: Int, proceso: String): Result<Unit> {
        return try {
            val request = IntervencionRequestDTO(proceso)
            val response = apiService.registrarIntervencion(idAveria, request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al registrar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sella y cierra definitivamente el ciclo de vida de la avería (PUT).
     * Requiere que el servidor valide la existencia de una intervención previa.
     * @param idAveria Identificador único de la avería a clausurar.
     * @return Result con la confirmación de cierre o la advertencia de requisitos faltantes.
     */
    suspend fun finalizarAveria(idAveria: Int): Result<String> {
        return try {
            val response = apiService.finalizarAveria(idAveria)

            if (response.isSuccessful) {
                Result.success("Ciclo de vida de la avería finalizado y archivado.")
            } else {
                // Parseo específico de reglas de negocio para mejorar la UX del técnico
                if (response.code() == 400) {
                    Result.failure(Exception("Requisito incumplido: Es obligatorio registrar una intervención antes del cierre."))
                } else {
                    Result.failure(Exception("Excepción en el servidor al intentar finalizar. Código: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error de red", e)
            Result.failure(Exception("Timeout de red al ejecutar la orden de finalización."))
        }
    }

    /**
     * Sincroniza el cambio de estado físico de la maquinaria con el backend (PUT).
     * @param idMaquinaria Identificador único de la máquina afectada.
     * @param codigoEstado Código numérico del nuevo estado físico.
     * @return Result con la confirmación de la base de datos o detalle del error.
     */
    suspend fun cambiarEstadoMaquinaria(idMaquinaria: Int, codigoEstado: Int): Result<String> {
        return try {
            // Empaquetamos el entero en el DTO esperado por el contrato de red
            val request = com.tynsolutions.gestionaveriasmovil.data.network.dto.CambiarEstadoMaquinaRequest(codigoEstado)

            // Invocamos el endpoint definido en ApiService
            val response = apiService.cambiarEstadoMaquina(idMaquinaria, request)

            if (response.isSuccessful) {
                Result.success("El estado de la maquinaria ha sido sincronizado correctamente.")
            } else {
                android.util.Log.e("API_SECURITY", "Error al cambiar estado. HTTP: ${response.code()}")
                Result.failure(Exception("Operación denegada por el servidor. Código HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("API_CRITICAL", "Fallo de red al cambiar estado: ${e.message}")
            Result.failure(Exception("Error de conectividad al intentar sincronizar el estado."))
        }
    }
}