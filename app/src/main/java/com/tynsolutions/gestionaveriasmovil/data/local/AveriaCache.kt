package com.tynsolutions.gestionaveriasmovil.data.local

import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Almacén de persistencia volátil para el intercambio de estados entre módulos.
 * * Implementa el patrón Singleton mediante un 'object' de Kotlin para actuar como
 * Single Source of Truth (SSoT) temporal, eliminando la latencia de red en las
 * transiciones de navegación y reduciendo la carga del servidor.
 */
object AveriaCache {

    /**
     * Referencia a la entidad de dominio actualmente en foco.
     * Se mantiene en memoria mientras el proceso de la aplicación esté activo.
     */
    var averiaSeleccionada: Averia? = null

    /**
     * Invalida la referencia almacenada.
     * Debe invocarse al cerrar sesión o al finalizar el ciclo de vida del detalle
     * para garantizar la higiene de la memoria y evitar estados inconsistentes.
     */
    fun limpiarCache() {
        averiaSeleccionada = null
    }
}