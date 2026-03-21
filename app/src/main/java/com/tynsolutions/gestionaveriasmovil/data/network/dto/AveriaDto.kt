package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Agrupación de Data Transfer Objects (DTOs) para el dominio de Averías.
 * Mapean la estructura JSON exacta devuelta por el servidor NetBeans.
 */

// ==========================================
// RESPUESTAS (GET)
// ==========================================

/**
 * DTO principal que envuelve la lista de averías.
 * Respeta el estándar REST del backend que encapsula el array en un nodo "data".
 */
data class AveriaTecnicoResponse(
    @SerializedName("data") val data: List<AveriaItemDTO>? = emptyList()
)

/**
 * Representación de cada avería devuelta por el servidor.
 * Usamos tipos anulables (String?) para prevenir NullPointerExceptions en caso de
 * que la base de datos devuelva campos vacíos.
 */
data class AveriaItemDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("descripcionAveria") val descripcionAveria: String?,
    @SerializedName("fechaAsigTecnico") val fechaAsigTecnico: String?,
    @SerializedName("fechaAcepTecnico") val fechaAcepTecnico: String?,
    @SerializedName("fechaFinalizTecnico") val fechaFinalizTecnico: String?,
    @SerializedName("procesoTecnico") val procesoTecnico: String?,
    @SerializedName("maquinaria") val maquinaria: MaquinariaResumenDTO?,
    @SerializedName("tipoAveria") val tipoAveria: TipoAveriaResumenDTO?
)

/**
 * Mapeo de la entidad anidada de maquinaria.
 */
data class MaquinariaResumenDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?
)

/**
 * Mapeo de la entidad anidada del tipo de avería.
 */
data class TipoAveriaResumenDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String?
)

// ==========================================
// PETICIONES (PUT / POST)
// ==========================================

/**
 * DTO para el endpoint PUT /averias/{id}/intervenciones
 */
data class IntervencionRequest(
    @SerializedName("procRealizadoTecnico") val procesoRealizado: String
)