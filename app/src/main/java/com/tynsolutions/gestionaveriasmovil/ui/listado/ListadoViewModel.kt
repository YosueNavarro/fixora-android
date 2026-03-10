package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

class ListadoViewModel : ViewModel() {

    // Backing property pattern: Mantenemos el estado mutable privado para garantizar el encapsulamiento
    // y evitar modificaciones directas desde la capa de la Vista.
    private val _averias = MutableLiveData<List<Averia>>()

    // Exponemos el estado de forma inmutable a través de la interfaz genérica LiveData.
    val averias: LiveData<List<Averia>> get() = _averias

    /**
     * Simula la petición asíncrona de datos a un repositorio.
     * En fases posteriores, esta llamada se delegará a un caso de uso (Clean Architecture)
     * o directamente a una interfaz de Retrofit/Room.
     */
    fun cargarAverias() {
        _averias.value = FakeDataSource.averias
    }

    /**
     * Aplica la regla de negocio para filtrar la colección de averías en base a su estado.
     * La actualización de _averias.value notifica automáticamente a los observadores activos.
     *
     * @param estado Estado objetivo por el cual filtrar (ej: "Nueva", "Recibida").
     */
    fun filtrarPorEstado(estado: String) {
        // Nota técnica: En un entorno de producción, delegaríamos este filtrado a la base de datos (SQL)
        // o al Backend para no penalizar la memoria del dispositivo con listas masivas.
        val listaFiltrada = FakeDataSource.averias.filter { it.estadoAveriaCalculado == estado }
        _averias.value = listaFiltrada
    }
}