package com.tynsolutions.gestionaveriasmovil.domain.model

/**
 * Entidad core del Dominio (Domain Entity) que modela el ciclo de vida de una Avería.
 * Representa el concepto de negocio puro, agnóstico a la infraestructura de red o persistencia.
 *
 * Nota Arquitectónica: Los atributos físicos del hardware (estado de la maquinaria)
 * se han segregado intencionadamente para respetar el Principio de Responsabilidad Única (SRP),
 * garantizando que esta entidad mute exclusivamente por la gestión técnica de la incidencia.
 */
data class Averia(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val maquinaria: String,
    val fechaInforme: String,
    val fechaAsignacion: String?,
    val fechaAceptacion: String?,
    val fechaFinalizacion: String?,
    val intervenciones: MutableList<String> = mutableListOf()
) {
    /**
     * Propiedad computada (Derived State) que actúa como Máquina de Estados (State Machine).
     * Infiere la fase administrativa de la incidencia evaluando la presencia secuencial
     * de las marcas de tiempo (Timestamps).
     * * Este enfoque elimina la redundancia de datos y previene estados anómalos
     * (ej. una avería marcada como "Finalizada" pero sin fecha de finalización).
     */
    val estadoAveriaCalculado: String
        get() = when {
            fechaFinalizacion != null -> "Finalizada"
            fechaAceptacion != null -> "Recibida"
            fechaAsignacion != null -> "Nueva"
            else -> "Pendiente"
        }
}