package com.tynsolutions.gestionaveriasmovil.domain.model

data class Usuario(
    val email: String,
    val password: String,
    val activo: Boolean
)