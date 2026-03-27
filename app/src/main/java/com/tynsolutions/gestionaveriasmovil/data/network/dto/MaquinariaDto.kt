package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload para la petición de actualización del estado operativo de una maquinaria.
 * Encapsula el identificador de estado garantizando un contrato estricto.
 */
data class CambiarEstadoMaquinaRequest(
    @SerializedName("codigoEstado") val codigoEstado: Int
)

/**
 * DTO que representa las entidades del catálogo de estados de situación.
 */
data class EstadoSituacionResponse(
    @SerializedName("codigoEstado") val codigoEstado: Int,
    @SerializedName("descripcionEstado") val descripcion: String
)

/**
 * DTO que representa los datos de la maquinaria anidada en las respuestas de la API.
 * Centralizamos su definición en este módulo para respetar la cohesión del dominio físico.
 */
data class MaquinariaResumen(
    @SerializedName(value = "id", alternate = ["codigoMaquinaria", "maquinariaFK", "codigo_maquinaria"])
    val id: Int?,
    @SerializedName(value = "nombre", alternate = ["nombreMaquinaria", "descripcionMaquinaria"])
    val nombre: String?,

    @SerializedName("estado") val estado: EstadoMaquinariaResumen? = null
)

/**
 * Representación del estado actual anidado dentro de una maquinaria específica.
 */
data class EstadoMaquinariaResumen(
    @SerializedName("codigoEstado") val codigo: Int,
    @SerializedName("descripcionEstado") val descripcion: String
)

/**
 * Wrapper para la respuesta de éxito al actualizar el estado de una máquina.
 */
data class CambiarEstadoMaquinaResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: CambiarEstadoMaquinaData?
)

data class CambiarEstadoMaquinaData(
    @SerializedName("codigoEstado") val codigoEstado: Int,
    @SerializedName("idMaquinaria") val idMaquinaria: Int
)