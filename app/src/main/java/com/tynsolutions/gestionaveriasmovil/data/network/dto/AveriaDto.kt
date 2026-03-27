package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Wrapper genérico para las respuestas de red que devuelven colecciones de averías.
 * Proporciona una lista vacía por defecto para mitigar NullPointerExceptions
 * si el backend omite el nodo 'data'.
 */
data class AveriaTecnicoResponse(
    @SerializedName("data") val data: List<AveriaItemDTO>? = emptyList()
)

/**
 * Wrapper para respuestas de red que devuelven una única entidad.
 */
data class AveriaDetalleResponse(
    @SerializedName("data") val data: AveriaItemDTO?
)

/**
 * DTO (Data Transfer Object) principal de la entidad Avería.
 * Implementa mecanismos de resiliencia (@alternate y JsonElement) para manejar
 * inconsistencias en la serialización del backend originadas por Jackson/GSON.
 */
data class AveriaItemDTO(
    @SerializedName(value = "id", alternate = ["codigoAveria"])
    val id: Int,

    @SerializedName(value = "descripcionAveria", alternate = ["desInicAveria"])
    val descripcionAveria: String?,

    /*
     * USO DE JsonElement: Estrategia defensiva ante mutaciones de formato de fecha.
     */
    @SerializedName("fechaAsigTecnico") val fechaAsigTecnico: JsonElement?,
    @SerializedName("fechaAcepTecnico") val fechaAcepTecnico: JsonElement?,
    @SerializedName("fechaFinalizTecnico") val fechaFinalizTecnico: JsonElement?,

    @SerializedName(value = "procesoTecnico", alternate = ["procRealizadoTecnico"])
    val procesoTecnico: String?,

    // DTOs anidados: Soportan tanto la entidad completa (MaquinariaResumen)
    // como su identificador relacional, dependiendo de la profundidad del endpoint.
    @SerializedName("maquinaria") val maquinaria: MaquinariaResumen? = null,
    @SerializedName(value = "maquinariaFK", alternate = ["codigoMaquinaria", "idMaquinaria"])
    val maquinariaId: Int? = null,

    @SerializedName("tipoAveria") val tipoAveria: TipoAveriaResumenDTO? = null,
    @SerializedName("tipoAveriaFK") val tipoAveriaId: Int? = null
)

/**
 * DTOs anidados para representación de relaciones internas (Mantenidos en su contexto).
 */
data class TipoAveriaResumenDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String?
)

/**
 * DTO para el envío (PUT/POST) del registro de intervenciones técnicas.
 */
data class IntervencionRequestDTO(
    @SerializedName("procesoTecnico") val procesoRealizado: String
)