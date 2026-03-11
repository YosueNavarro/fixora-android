package com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion

import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource

/**
 * Controlador lógico (ViewModel) para el caso de uso CU04: Registrar Intervención.
 * Actúa como intermediario entre la vista y el origen de datos, garantizando
 * el cumplimiento del principio de responsabilidad única (SRP) dentro de la arquitectura MVVM.
 */
class IntervencionViewModel : ViewModel() {

    /**
     * Ejecuta la lógica del Bloque 7.1 correspondiente a la Fase 1:
     * agregar la intervención a la lista local.
     * * Nota Arquitectónica (Fase 2): Este método será refactorizado para consumir
     * el endpoint POST /averias/{id}/intervenciones mediante Retrofit.
     *
     * @param averiaId Identificador primario de la avería sobre la que se opera.
     * @param texto Descripción detallada de las tareas ejecutadas por el técnico.
     * @return true si la persistencia en memoria fue exitosa, false si la entidad no fue localizada.
     */
    fun guardarIntervencion(averiaId: Int, texto: String): Boolean {
        // Único punto de acceso autorizado al Mock Data Source para esta transacción
        val averia = FakeDataSource.averias.find { it.id == averiaId }

        return if (averia != null) {
            // Mutación controlada del estado interno de la entidad
            averia.intervenciones.add(texto)
            true // Transacción local completada con éxito
        } else {
            false // Operación abortada: Entidad no encontrada en el origen de datos
        }
    }
}