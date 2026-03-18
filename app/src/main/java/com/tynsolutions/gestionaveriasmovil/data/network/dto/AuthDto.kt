package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

// Lo que enviamos al servidor
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// Lo que nos responde el servidor
data class LoginResponse(
    @SerializedName("token") val token: String
)