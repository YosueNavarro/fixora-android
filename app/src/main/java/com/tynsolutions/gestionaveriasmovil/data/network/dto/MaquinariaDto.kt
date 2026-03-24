package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload para la petición de actualización del estado operativo de una maquinaria.
 * Encapsula el identificador de estado (Clave Foránea) garantizando un contrato estricto
 * y tipado para la mutación de datos en el servidor.
 */
data class CambiarEstadoMaquinaRequest(
    @SerializedName("codigoEstadoFK") val codigoEstado: Int
)

/**
 * DTO que representa las entidades del catálogo de estados de situación.
 * Mapea la respuesta del endpoint de consulta proporcionando el diccionario
 * de códigos y descripciones permitidos por el sistema.
 */
data class EstadoSituacionResponse(
    @SerializedName("codigoEstado") val codigoEstado: Int,
    @SerializedName("descripcionEstado") val descripcion: String
)