package com.tynsolutions.gestionaveriasmovil.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Middleware de red para la orquestación global de la seguridad HTTP.
 * Implementa el esquema de autenticación estandarizado Bearer (RFC 6750), inyectando
 * el token JWT en la cabecera 'Authorization' de todas las peticiones salientes.
 *
 * Aislar esta lógica en la capa de OkHttp previene la fuga de responsabilidades
 * hacia los Repositorios y garantiza una política de "Zero Trust" interna,
 * donde ninguna mutación de red abandona el dispositivo sin ser evaluada para su firma.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    /**
     * Intercepta la cadena de ejecución (Chain) para mutar la petición original
     * antes de su emisión hacia el servidor perimetral.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // Clonamos la petición original para respetar la inmutabilidad de la cadena base
        val requestBuilder = chain.request().newBuilder()

        // Recuperación síncrona del criptograma desde el almacenamiento local
        val token = sessionManager.fetchAuthToken()

        // Inyección de la credencial: Si el token es válido (sesión activa), se firma la cabecera
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // Se retoma el flujo de red delegando la petición mutada al siguiente interceptor o al backend
        return chain.proceed(requestBuilder.build())
    }
}