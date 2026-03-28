package com.tynsolutions.gestionaveriasmovil.domain.model

/**
 * Entidad de dominio que representa un activo físico (hardware) del taller.
 */
data class Maquinaria(
    val id: Int,
    val nombre: String,
    val codigoEstado: Int = 0
)