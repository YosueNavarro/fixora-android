package com.tynsolutions.gestionaveriasmovil.domain.model

/**
 * Entidad central del dominio que representa una Avería.
 * Refactorizada para coincidir estrictamente con el contrato del backend actual,
 * omitiendo el estado físico de la maquinaria para evitar inconsistencias de datos.
 */
data class Averia(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val maquinaria: String, // Solo conservamos el nombre de la máquina
    val fechaInforme: String,
    val fechaAsignacion: String?,
    val fechaAceptacion: String?,
    val fechaFinalizacion: String?,
    val intervenciones: MutableList<String> = mutableListOf()
) {
    // Calculamos el estado administrativo de la avería sobre la marcha
    val estadoAveriaCalculado: String
        get() = when {
            fechaFinalizacion != null -> "Finalizada"
            fechaAceptacion != null -> "Recibida"
            fechaAsignacion != null -> "Nueva"
            else -> "Pendiente"
        }
}