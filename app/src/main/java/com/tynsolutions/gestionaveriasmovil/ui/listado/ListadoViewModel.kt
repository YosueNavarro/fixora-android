package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * ViewModel encargado de la lógica de negocio para la pantalla de listado.
 * Implementa el patrón de observación para actualizar la UI de forma reactiva.
 */
class ListadoViewModel : ViewModel() {

    private val _averias = MutableLiveData<List<Averia>>()
    val averias: LiveData<List<Averia>> get() = _averias

    /**
     * Carga el set de datos inicial.
     * Por defecto, podríamos querer mostrar las "Nuevas" al iniciar la app.
     */
    fun cargarAverias() {
        // Opcional: Puedes decidir si cargar todas o filtrar por "Nueva" por defecto.
        filtrarPorEstado("Nueva")
    }

    /**
     * Aplica el filtro de estado según la pestaña seleccionada en la UI.
     * Soporta los estados: "Nueva", "Recibida" y "Finalizada".
     *
     * @param estado Nombre del estado administrativo de la avería.
     */
    fun filtrarPorEstado(estado: String) {
        // Accedemos a la fuente de datos única (SSOT - Single Source of Truth)
        val todas = FakeDataSource.averias

        // Realizamos el filtrado.
        // Importante: Asegúrate de que 'estadoAveriaCalculado' sea el campo que cambia en el DetalleViewModel.
        val listaFiltrada = todas.filter { it.estadoAveriaCalculado == estado }

        // Notificamos a la vista con la nueva lista
        _averias.value = listaFiltrada
    }
}