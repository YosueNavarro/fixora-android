package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Agrupación de Data Transfer Objects (DTOs) para el flujo de autenticación.
 * Mantenerlos en un solo archivo mejora la cohesión del módulo de red.
 */

/**
 * DTO para enviar las credenciales al servidor.
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * DTO que mapea la respuesta de éxito (HTTP 200) del servidor.
 * Contiene el token JWT criptográfico y los metadatos del técnico.
 * REGLA DE SEGURIDAD: Usamos tipos anulables (?) para evitar NullPointerExceptions
 * durante la deserialización de Gson si el JSON llega modificado o incompleto.
 */
data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("usuario") val usuario: UsuarioLoginDTO?
)

/**
 * Mapeo del objeto anidado "usuario" que devuelve la API de NetBeans.
 * Añadido el campo 'rol' para mantener la paridad estricta con el JSON del servidor.
 */
data class UsuarioLoginDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("rol") val rol: String
)