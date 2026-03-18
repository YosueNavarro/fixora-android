package com.tynsolutions.gestionaveriasmovil.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de red que inyecta automáticamente el token de seguridad en las cabeceras.
 * Esto centraliza la seguridad y evita tener que pasar el token manualmente en cada llamada a la API.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Obtenemos el token almacenado
        val token = sessionManager.fetchAuthToken()

        // Si existe el token, lo inyectamos con el formato Bearer exigido por el servidor
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // Ejecutamos la petición con las cabeceras modificadas
        return chain.proceed(requestBuilder.build())
    }
}