package com.tynsolutions.gestionaveriasmovil.data.repository

import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO

/**
 * Repositorio centralizado para la gestión de averías.
 * Actúa como única fuente de la verdad para el dominio de Averías, abstrayendo
 * a las capas superiores (ViewModels) de la complejidad de la red y la seguridad.
 */
class AveriasRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    /**
     * Solicita al servidor la lista de averías asignadas al técnico logueado.
     * Gestiona internamente la inyección del token JWT y el formato Bearer.
     * * @return Result encapsulando una lista de AveriaItemDTO en caso de éxito,
     * o una excepción detallada en caso de fallo.
     */
    suspend fun getAveriasAsignadas(tipo: String = "nuevas"): Result<List<AveriaItemDTO>> {
        return try {
            // 1. Extraemos las credenciales seguras de la sesión local
            val token = sessionManager.fetchAuthToken()?.trim()
            val userId = sessionManager.fetchUserId()

            android.util.Log.d("API_DEBUG", "Llamando a red... ID: $userId, Token: ${token?.take(10)}...")

            // 2. Validación estricta de sesión activa
            if (token.isNullOrEmpty() || userId == -1) {
                return Result.failure(Exception("Sesión inválida o expirada. Por favor, vuelve a iniciar sesión."))
            }

            // 3. Formateo estándar de seguridad JWT (RFC 6750)
            val bearerToken = "Bearer $token"

            // Justo antes de llamar a la red
            android.util.Log.d("API_DEBUG", "Llamando a red... ID: $userId, Token: ${token.take(10)}...")

            // 4. Llamada de red al endpoint privado
            // Nota: Pasamos null al filtro temporalmente para traernos todas las averías.
            val response = apiService.getAveriasTecnico(userId, tipo)

            // === EL MICRÓFONO OCULTO ===
            android.util.Log.d("API_DEBUG", "Código HTTP de respuesta: ${response.code()}")

            // 5. Procesamiento de la respuesta HTTP
            if (response.isSuccessful) {
                // Extraemos la lista que viene dentro del nodo "data"
                val listaAverias = response.body()?.data ?: emptyList()
                android.util.Log.d("API_DEBUG", "¡Éxito! El servidor ha devuelto un JSON con ${listaAverias.size} averías.")
                Result.success(listaAverias)
            } else {
                // Manejo de códigos de error (401, 403, 404, 500)
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("API_DEBUG", "Error del servidor. Cuerpo del error: $errorBody")
                Result.failure(Exception("Error al cargar las averías. Código de servidor: ${response.code()}"))
            }

        } catch (e: Exception) {
            // Captura de excepciones de red (Timeout, sin internet, DNS no resuelto)
            android.util.Log.e("API_DEBUG", "Craso error de red o de parseo JSON: ${e.message}")
            Result.failure(Exception("Error de conexión: No se pudo contactar con el servidor del taller."))
        }
    }
}