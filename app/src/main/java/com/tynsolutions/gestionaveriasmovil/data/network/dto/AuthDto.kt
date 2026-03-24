package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

/*
 * Contratos de red (DTOs) para el flujo de autenticación y autorización.
 * Agrupados por dominio funcional para maximizar la cohesión del módulo.
 */

/**
 * Payload para la petición de inicio de sesión.
 * Encapsula las credenciales del técnico para su transmisión.
 * NOTA DE SEGURIDAD: Este DTO solo debe transmitirse a través de canales cifrados (HTTPS).
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * Wrapper para la respuesta exitosa del servicio de autenticación.
 * Contiene el token de sesión (JWT) y la identidad del usuario.
 * Implementa propiedades anulables como mecanismo de defensa ante respuestas
 * malformadas o posibles evoluciones del contrato de la API, evitando crashes por NPE.
 */
data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("usuario") val usuario: UsuarioLoginDTO?
)

/**
 * DTO anidado que representa la entidad del usuario en el contexto de login.
 * Mantiene paridad estricta con el nodo 'usuario' devuelto por el servidor.
 */
data class UsuarioLoginDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("rol") val rol: String
)