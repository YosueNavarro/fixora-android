package com.tynsolutions.gestionaveriasmovil.ui.detalle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

class DetalleViewModel : ViewModel() {

    // Backing property: Estado reactivo encapsulado que representa la avería actual.
    private val _averia = MutableLiveData<Averia>()

    // Exposición inmutable del estado para que la Vista se suscriba (Observer Pattern).
    val averia: LiveData<Averia> get() = _averia

    /**
     * Ejecuta la consulta al origen de datos (Data Source / Repository) para obtener
     * la entidad correspondiente al ID proporcionado.
     *
     * @param id Identificador primario de la avería solicitada.
     */
    fun cargarAveria(id: Int) {
        // En una implementación Clean Architecture, esto invocaría un UseCase (ej: GetAveriaByIdUseCase).
        val averiaEncontrada = FakeDataSource.averias.find { it.id == id }

        // Actualizamos el estado reactivo solo si la entidad existe (null safety).
        averiaEncontrada?.let {
            _averia.value = it
        }
    }
}