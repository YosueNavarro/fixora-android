package com.tynsolutions.gestionaveriasmovil.data.network.mapper

import com.google.gson.JsonElement
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.domain.model.Maquinaria
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Capa Anti-Corrupción (ACL) del sistema de averías.
 * Transforma los DTOs inestables provenientes de la red en entidades de Dominio inmutables.
 * Implementa estrategias de rehidratación mediante caché local para garantizar la persistencia
 * de datos en transiciones de UI donde la red no devuelve objetos completos.
 */
fun AveriaItemDTO.toDomain(): Averia {
    // Validamos la correspondencia de la caché para evitar sobreescritura cruzada (Cache Bleed)
    val cacheActiva = AveriaCache.averiaSeleccionada?.takeIf { it.id == this.id }

    return Averia(
        id = this.id,
        // Fallback en cascada: Prioriza datos nuevos de red -> Datos cacheados -> Valores por defecto seguros.
        titulo = this.tipoAveria?.descripcion ?: cacheActiva?.titulo ?: "Incidencia #${this.id}",
        descripcion = this.descripcionAveria ?: cacheActiva?.descripcion ?: "Sin descripción proporcionada",

        maquinaria = when {
            this.maquinaria != null -> MaquinariaMapper.toDomain(this.maquinaria, null)
            this.maquinariaId != null && this.maquinariaId > 0 -> MaquinariaMapper.toDomain(null, this.maquinariaId)
            cacheActiva?.maquinaria != null -> cacheActiva.maquinaria
            else -> Maquinaria(-1, "Máquina no especificada")
        },

        fechaInforme = extraerFechaSegura(this.fechaAsigTecnico) ?: cacheActiva?.fechaInforme ?: "Pendiente de asignación",
        fechaAsignacion = extraerFechaSegura(this.fechaAsigTecnico),
        fechaAceptacion = extraerFechaSegura(this.fechaAcepTecnico) ?: cacheActiva?.fechaAceptacion,
        fechaFinalizacion = extraerFechaSegura(this.fechaFinalizTecnico) ?: cacheActiva?.fechaFinalizacion,

        intervenciones = if (!this.procesoTecnico.isNullOrBlank()) {
            this.procesoTecnico.split("\n").filter { it.isNotBlank() }.toMutableList()
        } else {
            cacheActiva?.intervenciones ?: mutableListOf()
        }
    )
}

/**
 * Motor determinista de extracción y sanitización de marcas de tiempo.
 * Diseñado para soportar inconsistencias en el parser (Jackson/Gson) del Backend.
 * Absorbe mutaciones polimórficas del JSON devolviendo un formato unificado.
 *
 * @param jsonElement Nodo GSON crudo sujeto a evaluación (puede ser null, String o JsonArray).
 * @return Cadena formateada a "dd/MM/yyyy HH:mm", o null si la integridad del dato está comprometida.
 */
private fun extraerFechaSegura(jsonElement: JsonElement?): String? {
    if (jsonElement == null || jsonElement.isJsonNull) return null

    return try {
        if (jsonElement.isJsonArray) {
            // Escenario A: Deserialización de Jackson LocalDatetime a JsonArray [YYYY, MM, DD, hh, mm, ss]
            val array = jsonElement.asJsonArray

            val year = array.get(0).asInt
            val month = array.get(1).asInt
            val day = array.get(2).asInt
            val hour = if (array.size() > 3) array.get(3).asInt else 0
            val minute = if (array.size() > 4) array.get(4).asInt else 0

            String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d", day, month, year, hour, minute)

        } else {
            // Escenario B: Deserialización de String ISO-8601 o SQL Timestamp ("2026-03-24 13:31:23.0")
            val textoOriginal = jsonElement.asString

            if (textoOriginal.isBlank() || textoOriginal.startsWith("0000-00-00")) return null

            // Sanitización del payload: Estandarización a ISO y truncado de milisegundos
            val fechaLimpia = textoOriginal.replace(" ", "T").split(".")[0]

            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val fechaParseada = formatoEntrada.parse(fechaLimpia.substring(0, 16))
            if (fechaParseada != null) formatoSalida.format(fechaParseada) else textoOriginal
        }
    } catch (e: Exception) {
        // Implementación de Graceful Degradation: Evitamos crashear la UI por errores de formato externos.
        // Registramos el evento de fallo para futura trazabilidad y auditoría.
        android.util.Log.e("API_SECURITY_PARSER", "Fallo de parseo temporal. Nodo comprometido: ${e.message}")
        null
    }
}