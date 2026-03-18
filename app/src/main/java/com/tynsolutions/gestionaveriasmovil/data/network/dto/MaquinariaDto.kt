package com.tynsolutions.gestionaveriasmovil.data.network.dto

import com.google.gson.annotations.SerializedName

// Para pedir el cambio de estado de una máquina
data class CambiarEstadoMaquinaRequest(
    @SerializedName("codigoEstadoFK") val codigoEstado: Int
)

// Lo que nos devuelve la lista de estados posibles (GET /estado/situacion)
data class EstadoSituacionResponse(
    @SerializedName("codigoEstado") val codigoEstado: Int,
    @SerializedName("descripcionEstado") val descripcion: String
)