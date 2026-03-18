package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

data class AveriaResponse(
    @SerializedName("codigoAveria") val codigoAveria: Int,
    @SerializedName("descInicAveria") val descripcionInicial: String?,
    @SerializedName("fechaInicioAver") val fechaInicio: String?,
    @SerializedName("fechaAsigTecnico") val fechaAsignacion: String?,
    @SerializedName("fechaAcepTecnico") val fechaAceptacion: String?,
    @SerializedName("fechaFinalizTecnico") val fechaFinalizacion: String?,
    @SerializedName("procRealizadoTecnico") val procesoRealizado: String?,

    // Claves foráneas que probablemente la API nos devuelva como IDs o como objetos anidados.
    // Por ahora las preparamos como IDs tal como marca la base de datos de Laura.
    @SerializedName("usuarioReportaFK") val idUsuarioReporta: Int?,
    @SerializedName("usuarioTecnicoFK") val idUsuarioTecnico: Int?,
    @SerializedName("maquinariaFK") val idMaquinaria: Int?,
    @SerializedName("tipoAveriaFK") val idTipoAveria: Int?
)

// DTO para el endpoint PUT /averias/{id}/intervenciones
data class IntervencionRequest(
    @SerializedName("procRealizadoTecnico") val procesoRealizado: String
)