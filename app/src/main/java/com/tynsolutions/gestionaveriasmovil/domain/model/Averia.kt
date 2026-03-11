package com.tynsolutions.gestionaveriasmovil.domain.model

data class Averia(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val maquinaria: String,
    val estadoMaquinaria: String, // "Operativa", "Averiada", "En mantenimiento", "Fuera de servicio"
    val fechaInforme: String,
    val fechaAsignacion: String?, // Son con '?' porque pueden ser nulas (aún no ha pasado)
    val fechaAceptacion: String?,
    val fechaFinalizacion: String?,

    /*
     Solución momentanea (fase 1) para las intervenciones,
     En BBDD es un Varchar, habrá que buscar una solución para la conversión
     o convertir este campo a String.
    */
    val intervenciones: MutableList<String> = mutableListOf()
) {
    // Magia de Kotlin: Calculamos el estado de la avería sobre la marcha para la interfaz
    val estadoAveriaCalculado: String
        get() = when {
            fechaFinalizacion != null -> "Finalizada"
            fechaAceptacion != null -> "Recibida" // Ya la ha aceptado el técnico
            fechaAsignacion != null -> "Nueva"    // Se le ha asignado pero no la ha aceptado
            else -> "Pendiente"
        }
}