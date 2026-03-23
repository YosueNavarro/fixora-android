package com.tynsolutions.gestionaveriasmovil.data.local

import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Memoria temporal para pasar la avería seleccionada desde el Listado al Detalle
 * sin necesidad de hacer llamadas a la API ni serializar objetos complejos.
 */
object AveriaCache {
    var averiaSeleccionada: Averia? = null
}