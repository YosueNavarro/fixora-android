package com.tynsolutions.gestionaveriasmovil.data.repository

import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.data.network.dto.IntervencionRequestDTO

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
     * * @param tipo Filtro de estado ("nuevas", "en_curso", "historico").
     * @return Result encapsulando una lista de [AveriaItemDTO] en caso de éxito,
     * o una excepción detallada lista para ser consumida por la UI.
     */
    suspend fun getAveriasAsignadas(tipo: String = "nuevas"): Result<List<AveriaItemDTO>> {
        return try {
            // 1. Extraemos las credenciales seguras de la sesión local
            val token = sessionManager.fetchAuthToken()?.trim()
            val userId = sessionManager.fetchUserId()

            android.util.Log.d("API_DEBUG", "Llamando a red... ID: $userId, Token: ${token?.take(10)}...")

            // 2. Validación estricta de sesión activa (Fail-fast prevention)
            if (token.isNullOrEmpty() || userId == -1) {
                return Result.failure(Exception("Sesión de seguridad inválida o expirada. Es necesario reautenticarse."))
            }

            val bearerToken = "Bearer $token"
            android.util.Log.d("API_DEBUG", "Preparando petición segura. ID: $userId")

            // 3. Llamada de red al endpoint privado
            val response = apiService.getAveriasTecnico(userId, tipo)

            android.util.Log.d("API_DEBUG", "Código HTTP de respuesta: ${response.code()}")

            // 4. Procesamiento de la respuesta HTTP
            if (response.isSuccessful) {
                val listaAverias = response.body()?.data ?: emptyList()
                android.util.Log.d("API_DEBUG", "¡Éxito! El servidor ha devuelto un JSON con ${listaAverias.size} averías.")
                Result.success(listaAverias)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("API_DEBUG", "Error del servidor. Cuerpo del error: $errorBody")
                Result.failure(Exception("Fallo de sincronización. Código HTTP: ${response.code()}"))
            }

        } catch (e: Exception) {
            android.util.Log.e("API_DEBUG", "Craso error de red o de parseo JSON: ${e.message}")
            Result.failure(Exception("Error crítico de conectividad con el centro de datos."))
        }
    }

    // ==========================================
    // FASE 4: TRANSACCIONES Y MODIFICACIONES (UPDATE)
    // ==========================================

    /**
     * Transiciona el estado de la avería a "En curso" (PUT).
     * La seguridad y autorización (JWT) se delegan al Interceptor de OkHttp configurado en el cliente.
     * * @param idAveria Identificador único de la avería a modificar.
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
            Result.failure(Exception("Interrupción de red durante la firma de aceptación."))
        }
    }

    /**
     * Persiste el informe de intervención técnica en la base de datos (PUT).
     * * @param idAveria Identificador único de la avería.
     * @param proceso Descripción detallada de las acciones realizadas por el técnico.
     * @return Result indicando el éxito de la escritura.
     */
    suspend fun registrarIntervencion(idAveria: Int, proceso: String): Result<String> {
        return try {
            val request = IntervencionRequestDTO(proceso)
            val response = apiService.registrarIntervencion(idAveria, request)

            if (response.isSuccessful) {
                Result.success("El informe de intervención ha sido persistido con éxito.")
            } else {
                Result.failure(Exception("Error al registrar la intervención. Código: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Pérdida de paquetes al intentar guardar el informe técnico."))
        }
    }

    /**
     * Sella y cierra definitivamente el ciclo de vida de la avería (PUT).
     * Requiere que el servidor valide la existencia de una intervención previa.
     * * @param idAveria Identificador único de la avería a clausurar.
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
            Result.failure(Exception("Timeout de red al ejecutar la orden de finalización."))
        }
    }
}