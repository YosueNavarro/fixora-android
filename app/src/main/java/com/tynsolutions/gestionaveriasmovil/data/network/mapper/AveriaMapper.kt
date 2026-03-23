package com.tynsolutions.gestionaveriasmovil.data.network.mapper

import com.google.gson.JsonElement
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Capa Anti-Corrupción (ACL - Anti-Corruption Layer).
 * Transforma el DTO inestable de la red en una entidad de Dominio inmutable y segura.
 * Implementa rehidratación de estados usando la memoria volátil (Caché).
 */
fun AveriaItemDTO.toDomain(): Averia {
    // Cerramos el scope de la caché estrictamente al ID actual para evitar "Cache Bleed" (Fuga de datos).
    val cache = AveriaCache.averiaSeleccionada.takeIf { it?.id == this.id }

    return Averia(
        id = this.id,
        // Fallback en cascada: API -> Caché -> Valor por defecto seguro.
        titulo = this.tipoAveria?.descripcion ?: cache?.titulo ?: "Incidencia #${this.id}",
        descripcion = this.descripcionAveria ?: cache?.descripcion ?: "Sin descripción proporcionada",
        maquinaria = this.maquinaria?.nombre ?: cache?.maquinaria ?: "Maquinaria sin especificar",

        // Procesamiento seguro de polimorfismo JSON en fechas
        fechaInforme = extraerFechaSegura(this.fechaAsigTecnico) ?: cache?.fechaInforme ?: "Pendiente de asignación",
        fechaAsignacion = extraerFechaSegura(this.fechaAsigTecnico),
        fechaAceptacion = extraerFechaSegura(this.fechaAcepTecnico) ?: cache?.fechaAceptacion,
        fechaFinalizacion = extraerFechaSegura(this.fechaFinalizTecnico) ?: cache?.fechaFinalizacion,

        intervenciones = if (!this.procesoTecnico.isNullOrBlank()) {
            // Saneamiento de input: Generamos una lista estructurada a partir del bloque de texto crudo.
            this.procesoTecnico.split("\n").filter { it.isNotBlank() }.toMutableList()
        } else {
            cache?.intervenciones ?: mutableListOf()
        }
    )
}

/**
 * Algoritmo determinista para la extracción y sanitización de marcas de tiempo.
 * Diseñado para soportar inconsistencias severas en el motor de serialización del Backend.
 * * @param jsonElement Elemento GSON crudo (puede ser null, String o JsonArray).
 * @return String formateado a "dd/MM/yyyy HH:mm" o null si la integridad del dato está comprometida.
 */
private fun extraerFechaSegura(jsonElement: JsonElement?): String? {
    // 1. Descarte rápido de valores nulos o vacíos a nivel de nodo JSON
    if (jsonElement == null || jsonElement.isJsonNull) return null

    return try {
        if (jsonElement.isJsonArray) {
            // Escenario A: Jackson serializó el LocalDateTime como un array [YYYY, MM, DD, hh, mm, ss]
            val array = jsonElement.asJsonArray

            // Garantizamos la lectura sin OutOfBounds incluso si omiten hora/minutos
            val year = array.get(0).asInt
            val month = array.get(1).asInt
            val day = array.get(2).asInt
            val hour = if (array.size() > 3) array.get(3).asInt else 0
            val minute = if (array.size() > 4) array.get(4).asInt else 0

            String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d", day, month, year, hour, minute)

        } else {
            // Escenario B: El servidor serializó en formato de texto plano (ISO-8601 o SQL Timestamp)
            val textoOriginal = jsonElement.asString
            if (textoOriginal.isBlank()) return null

            // Sanitización del payload: unificamos espacios por la 'T' del estándar ISO y truncamos milisegundos
            val fechaLimpia = textoOriginal.replace(" ", "T").split(".")[0]

            // Restringimos el parseo a los primeros 16 caracteres (yyyy-MM-ddTHH:mm) descartando los segundos
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val fechaParseada = formatoEntrada.parse(fechaLimpia.substring(0, 16))
            if (fechaParseada != null) formatoSalida.format(fechaParseada) else textoOriginal
        }
    } catch (e: Exception) {
        // En un entorno de alta seguridad, nunca lanzamos excepciones no controladas por errores de formato externos.
        // Registramos la anomalía para auditoría, pero no penalizamos la experiencia del usuario (Graceful Degradation).
        android.util.Log.e("API_SECURITY_PARSER", "Interrupción al procesar nodo de fecha temporal: ${e.message}")
        null
    }
}