package com.tynsolutions.gestionaveriasmovil.data.network.mapper

import com.tynsolutions.gestionaveriasmovil.data.network.dto.MaquinariaResumen
import com.tynsolutions.gestionaveriasmovil.domain.model.Maquinaria

/**
 * Transformador de capa de datos a capa de dominio para el hardware del taller.
 * Aísla la lógica de negocio de las peculiaridades del contrato JSON del backend.
 */
object MaquinariaMapper {

    /**
     * Convierte el DTO de red (MaquinariaResumen) en una entidad de negocio (Maquina).
     * @param dto El objeto parseado por GSON desde la API.
     * @param fallbackId ID de respaldo por si el endpoint devuelve la FK pero no el objeto anidado completo.
     */
    fun toDomain(dto: MaquinariaResumen?, fallbackId: Int? = null): Maquinaria {
        return Maquinaria(
            id = dto?.id ?: fallbackId ?: -1, // -1 indica pérdida de integridad relacional
            nombre = dto?.nombre ?: "Máquina no especificada",
            codigoEstado = dto?.estado?.codigo ?: 802
        )
    }
}