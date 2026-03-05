package com.tynsolutions.gestionaveriasmovil.domain.model

data class Averia(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val estado: String,
    val fechaAsignacion: String,
    val maquinaria: String
)