package com.tynsolutions.gestionaveriasmovil.data.repository

import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.LoginRequest

/**
 * Repositorio de Dominio para la orquestación del flujo de Autenticación.
 * Implementa el patrón Repository aislando los orígenes de datos (Network/Local)
 * de la capa de presentación (UI/ViewModels), actuando como única fuente de la verdad.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    /**
     * Ejecuta la transacción de autenticación contra el servidor perimetral.
     * En caso de éxito (HTTP 200), delega la persistencia de las credenciales
     * al [SessionManager]. En caso de fallo, mapea los códigos HTTP a excepciones de Dominio.
     *
     * @param email Credencial de identidad del técnico.
     * @param pass Credencial de acceso (Plaintext, delegando el cifrado de transporte al túnel TLS/HTTPS).
     * @return [Result] encapsulando un mensaje de éxito o una excepción detallada y amigable para la UI.
     */
    suspend fun realizarLogin(email: String, pass: String): Result<String> {
        return try {
            val request = LoginRequest(email, pass)
            val response = apiService.login(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                val token = responseBody?.token
                val userId = responseBody?.usuario?.id

                // Auditoría de integridad de payload: Rechazamos respuestas HTTP 200
                // que no contengan los artefactos criptográficos o relacionales necesarios.
                if (!token.isNullOrEmpty() && userId != null) {
                    sessionManager.saveAuthToken(token)
                    sessionManager.saveUserId(userId)

                    Result.success("Autenticación completada con éxito")
                } else {
                    Result.failure(Exception("Violación del contrato de red: Payload de autorización incompleto."))
                }
            } else {
                // Mapeo de errores de dominio basado en el contrato del backend
                val errorBodyString = response.errorBody()?.string() ?: ""

                // Evaluación de reglas de negocio específicas (Control de Acceso Basado en Roles - RBAC)
                if (errorBodyString.contains("Tipo de usuario incorrecto", ignoreCase = true)) {
                    Result.failure(Exception("Acceso denegado: Aplicación restringida a perfil Técnico/Mecánico."))
                }
                // Mapeo estándar de errores HTTP 4xx (Unauthorized/Forbidden)
                else if (response.code() == 401 || response.code() == 403) {
                    Result.failure(Exception("Credenciales de acceso inválidas."))
                }
                // Fallback para errores de servidor (HTTP 5xx) o no tipificados
                else {
                    Result.failure(Exception("Excepción en el servidor perimetral. Código HTTP: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            // Intercepción de fallos de infraestructura (Timeouts, DNS, pérdida de señal)
            Result.failure(Exception("Fallo en la resolución de red. Verifique la conectividad del dispositivo."))
        }
    }
}