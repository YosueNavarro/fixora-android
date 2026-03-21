package com.tynsolutions.gestionaveriasmovil.data.network.dto

/**
 * Data Transfer Object (DTO) exclusivo para aislar el envío de datos de una intervención.
 * Mapea directamente contra la clase IntervencionRequest del backend en Spring Boot,
 * garantizando que el serializador JSON (Gson/Moshi) construya la clave "procesoTecnico" exacta.
 */
data class IntervencionRequestDTO(
    val procesoTecnico: String
)