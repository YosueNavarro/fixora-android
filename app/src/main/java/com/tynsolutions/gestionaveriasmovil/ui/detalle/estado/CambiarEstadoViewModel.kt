package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Controlador lógico (ViewModel) para el caso de uso CU05: Cambiar estado maquinaria.
 * Aísla la capa de presentación de la capa de acceso a datos, cumpliendo estrictamente
 * con el patrón arquitectónico MVVM.
 */
class CambiarEstadoViewModel : ViewModel() {

    /**
     * Recupera la entidad de dominio asociada al identificador proporcionado.
     *
     * @param averiaId Identificador único de la avería.
     * @return Objeto [Averia] si se encuentra en el origen de datos en memoria, o null en caso contrario.
     */
    fun obtenerAveria(averiaId: Int): Averia? {
        // Único punto de acceso autorizado al origen de datos simulado
        return FakeDataSource.averias.find { it.id == averiaId }
    }

    /**
     * Ejecuta la mutación del estado físico de la maquinaria (Atributo local - Fase 1)[cite: 200].
     * En la Fase 2, este método será refactorizado para invocar el endpoint:
     * PUT /maquinaria/{id}/estado mediante Retrofit[cite: 201].
     *
     * @param averiaId Identificador primario de la avería a actualizar.
     * @param nuevoEstado El nuevo estado físico seleccionado por el usuario (ej. "En mantenimiento").
     * @return true si la operación de persistencia simulada fue exitosa, false si falló la localización.
     */
    fun actualizarEstadoMaquinaria(averiaId: Int, nuevoEstado: String): Boolean {
        val averia = obtenerAveria(averiaId)

        return if (averia != null) {
            val index = FakeDataSource.averias.indexOf(averia)
            // Actualización inmutable del objeto dentro de la colección mutable para mantener la coherencia
            FakeDataSource.averias[index] = averia.copy(estadoMaquinaria = nuevoEstado)
            true // Transacción local completada con éxito
        } else {
            false // Operación abortada: Entidad no encontrada
        }
    }
}