package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Máquina de estados inmutable para la vista del listado.
 * Garantiza que la UI solo pueda estar en uno de estos tres estados,
 * evitando bugs visuales de concurrencia.
 */
sealed class ListadoUiState {
    object Loading : ListadoUiState()
    data class Success(val averias: List<AveriaItemDTO>) : ListadoUiState()
    data class Error(val message: String) : ListadoUiState()
}

/**
 * ViewModel central para la pantalla de listado de averías.
 * Actúa como orquestador entre el repositorio de red y la interfaz de usuario,
 * gestionando el caché local en memoria para no saturar la red con peticiones repetidas.
 */
class ListadoViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    // ==========================================
    // ESTADO REACTIVO (UI)
    // ==========================================

    // StateFlow privado que modificamos internamente de forma segura
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Loading)

    // StateFlow público, inmutable, expuesto a la vista (Fragment)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    // Caché en memoria (SSOT temporal) para retener las averías de la API
    // y permitir el filtrado por pestañas de forma instantánea y sin latencia de red.
    private var cachéAverias: List<AveriaItemDTO> = emptyList()

    /**
     * Carga el set de datos inicial desde el servidor.
     * Ejecutado asíncronamente en el contexto del viewModelScope.
     */
    fun cargarAverias(tipoFiltro: String = "nuevas") {
        viewModelScope.launch {
            _uiState.value = ListadoUiState.Loading

            // 1. Le pedimos a la base de datos el paquete exacto
            val result = repository.getAveriasAsignadas(tipoFiltro)

            // 2. Procesamos la respuesta
            result.fold(
                onSuccess = { listaAverias ->
                    _uiState.value = ListadoUiState.Success(listaAverias)
                },
                onFailure = { exception ->
                    _uiState.value = ListadoUiState.Error(exception.message ?: "Fallo de conexión.")
                }
            )
        }
    }

    /**
     * Aplica el filtro de estado según la pestaña seleccionada en la UI.
     * Soporta los estados: "Nueva", "Recibida" y "Finalizada".
     *
     * @param estado Nombre del estado administrativo de la avería.
     */
    fun filtrarPorEstado(estado: String) {
        // En lugar de usar un campo pre-calculado, inferimos el estado de forma segura
        // analizando los timestamps de la base de datos que nos envía el DTO.
        val listaFiltrada = cachéAverias.filter { averia ->
            when (estado) {
                "Nueva" -> averia.fechaAcepTecnico.isNullOrEmpty() && averia.fechaFinalizTecnico.isNullOrEmpty()
                "Recibida" -> !averia.fechaAcepTecnico.isNullOrEmpty() && averia.fechaFinalizTecnico.isNullOrEmpty()
                "Finalizada" -> !averia.fechaFinalizTecnico.isNullOrEmpty()
                else -> true // Fallback de seguridad: mostrar todas si el filtro no coincide
            }
        }

        // Emitimos la lista filtrada a la interfaz gráfica
        _uiState.value = ListadoUiState.Success(listaFiltrada)
    }

    /**
     * Patrón Factory para permitir la inyección de dependencias (AveriasRepository)
     * en el constructor del ViewModel.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ListadoViewModel::class.java)) {
                return ListadoViewModel(repository) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida o mal mapeada")
        }
    }
}