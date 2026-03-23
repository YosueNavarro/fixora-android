package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ==========================================
// RESPUESTAS (GET)
// ==========================================

/**
 * Envoltorio para respuestas de listas.
 * Mapea el array 'data' del JSON a una lista de objetos AveriaItemDTO.
 */
data class AveriaTecnicoResponse(
    @SerializedName("data") val data: List<AveriaItemDTO>? = emptyList()
)

/**
 * Contrato de red principal para la entidad Avería.
 * Implementa alias (@alternate) y tipos genéricos (JsonElement) como mecanismo
 * de defensa ante un backend con estructuras de serialización inestables.
 */
data class AveriaItemDTO(
    @SerializedName(value = "id", alternate = ["codigoAveria"])
    val id: Int,

    @SerializedName(value = "descripcionAveria", alternate = ["desInicAveria"])
    val descripcionAveria: String?,

    // USO CRÍTICO DE JsonElement: Absorbe mutaciones de formato de la API.
    // Permite recibir tanto Strings ISO-8601 ("2026-03-23T12:52:16")
    // como Arrays de enteros de Jackson ([2026, 3, 23, 12, 52, 16]).
    @SerializedName("fechaAsigTecnico") val fechaAsigTecnico: JsonElement?,
    @SerializedName("fechaAcepTecnico") val fechaAcepTecnico: JsonElement?,
    @SerializedName("fechaFinalizTecnico") val fechaFinalizTecnico: JsonElement?,

    @SerializedName(value = "procesoTecnico", alternate = ["procRealizadoTecnico"])
    val procesoTecnico: String?,

    // Separación de responsabilidades: DTO soporta objeto completo o clave foránea.
    @SerializedName("maquinaria")
    val maquinaria: MaquinariaResumen? = null,

    @SerializedName("maquinariaFK")
    val maquinariaId: Int? = null,

    @SerializedName("tipoAveria")
    val tipoAveria: TipoAveriaResumenDTO? = null,

    @SerializedName("tipoAveriaFK")
    val tipoAveriaId: Int? = null
)

data class MaquinariaResumen(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("estado") val estado: EstadoMaquinariaResumen? = null
)

data class EstadoMaquinariaResumen(
    @SerializedName("codigoEstado") val codigo: Int,
    @SerializedName("descripcionEstado") val descripcion: String
)

data class TipoAveriaResumenDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String?
)

/**
 * Envoltorio para respuestas de una única entidad.
 */
data class AveriaDetalleResponse(
    @SerializedName("data") val data: AveriaItemDTO?
)

// ==========================================
// PETICIONES (PUT / POST)
// ==========================================

/**
 * DTO para el envío de intervenciones.
 */
data class IntervencionRequestDTO(
    @SerializedName("procesoTecnico") val procesoRealizado: String
)