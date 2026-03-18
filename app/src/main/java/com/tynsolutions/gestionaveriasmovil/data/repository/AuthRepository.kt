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
                // Si el servidor responde 200 OK, extraemos el token de la respuesta
                val token = response.body()?.token
                if (!token.isNullOrEmpty()) {
                    // Guardamos el token de forma persistente
                    sessionManager.saveAuthToken(token)
                    Result.success("Login completado con éxito")
                } else {
                    Result.failure(Exception("El servidor no devolvió un token válido."))
                }
            } else {
                // Si el servidor responde 401 (No autorizado), 404, 500...
                Result.failure(Exception("Credenciales incorrectas o error en el servidor. Código: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Error de red (sin internet, servidor caído, timeout de OkHttp)
            Result.failure(Exception("Error de conexión: Verifica tu acceso a internet o contacta con soporte."))
        }
    }
}