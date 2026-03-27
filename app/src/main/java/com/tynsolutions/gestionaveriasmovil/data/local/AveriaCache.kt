package com.tynsolutions.gestionaveriasmovil.data.local

import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Almacén de persistencia volátil para el intercambio de estados entre módulos.
 * Implementa el patrón Singleton mediante un 'object' de Kotlin para actuar como
 * Single Source of Truth (SSoT) temporal.
 */
object AveriaCache {

    /**
     * Referencia a la entidad de dominio actualmente en foco.
     * Se sobrescribe constantemente al navegar desde el listado.
     */
    var averiaSeleccionada: Averia? = null

    /**
     * Diccionario de Reconciliación de Estado (State Reconciliation Dictionary).
     * Mapea [ID de Máquina] -> [Código de Estado].
     * Actúa como capa de resiliencia (L1 Cache) para suplir la carencia de datos
     * operativos en los payloads del endpoint de listado del servidor.
     */
    val estadoMaquinasGlobal = mutableMapOf<Int, Int>()

    /**
     * Invalida la referencia almacenada.
     * Nota de seguridad: No limpiamos 'estadoMaquinasGlobal' aquí para que el historial
     * sobreviva a las navegaciones entre el listado y el detalle.
     */
    fun limpiarCache() {
        averiaSeleccionada = null
    }
}