package com.tynsolutions.gestionaveriasmovil.data.repository

import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.network.dto.LoginRequest

/**
 * Repositorio centralizado para gestionar la lógica de autenticación.
 * Actúa como única fuente de la verdad para el login, aislando a la UI
 * de la implementación de red.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    /**
     * Realiza la petición de login al servidor.
     * Retorna un Result<String> que contendrá "Éxito" si va bien,
     * o una excepción con el mensaje de error si falla.
     */
    suspend fun realizarLogin(email: String, pass: String): Result<String> {
        return try {
            val request = LoginRequest(email, pass)
            val response = apiService.login(request)

            if (response.isSuccessful) {
                // Extraemos el cuerpo (Body) usando el DTO que acabamos de crear
                val responseBody = response.body()
                val token = responseBody?.token
                val userId = responseBody?.usuario?.id

                // Verificación de seguridad estricta: No damos por válido el login
                // si el servidor no nos entrega las dos piezas clave.
                if (!token.isNullOrEmpty() && userId != null) {
                    // Guardamos el token y el ID de forma persistente y segura
                    sessionManager.saveAuthToken(token)
                    sessionManager.saveUserId(userId)

                    Result.success("Login completado con éxito")
                } else {
                    Result.failure(Exception("Vulnerabilidad o error de API: El servidor devolvió 200 OK, pero el token o el ID están vacíos."))
                }
            } else {
                // ==========================================
                // CAPTURA DE ERRORES DE NEGOCIO (UX)
                // ==========================================

                // Leemos el mensaje de error que nos manda NetBeans en el cuerpo de la respuesta
                val errorBodyString = response.errorBody()?.string() ?: ""

                // Buscamos la frase exacta que programó Nereida en su AuthService
                if (errorBodyString.contains("Tipo de usuario incorrecto", ignoreCase = true)) {
                    Result.failure(Exception("Acceso denegado: Esta aplicación es exclusiva para el personal técnico (Mecánicos)."))
                }
                // Fallback clásico por si cambia el texto pero mantiene el código HTTP 401/403
                else if (response.code() == 401 || response.code() == 403) {
                    Result.failure(Exception("Email o contraseña incorrectos."))
                }
                // Cualquier otro error (500, 404...)
                else {
                    Result.failure(Exception("Error en el servidor. Código: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            // Error de red (sin internet, servidor caído, timeout de OkHttp)
            Result.failure(Exception("Error de conexión: Verifica tu acceso a internet o contacta con soporte."))
        }
    }
}