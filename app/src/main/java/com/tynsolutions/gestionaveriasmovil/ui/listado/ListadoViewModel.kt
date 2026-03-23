package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// Importación corregida: Ahora trabajamos estrictamente con el Modelo de Dominio seguro
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Máquina de estados inmutable para la vista del listado.
 * Garantiza que la UI solo pueda estar en uno de estos tres estados,
 * evitando bugs visuales de concurrencia y aislando la Vista de la capa de Red.
 */
sealed class ListadoUiState {
    object Loading : ListadoUiState()
    // CORRECCIÓN ARQUITECTÓNICA: Exigimos el modelo de dominio puro (Averia)
    data class Success(val averias: List<Averia>) : ListadoUiState()
    data class Error(val message: String) : ListadoUiState()
}

/**
 * ViewModel central para la pantalla de listado de averías.
 * Actúa como orquestador entre el repositorio y la interfaz de usuario.
 * Mantiene la Single Source of Truth (SSOT) en memoria para el filtrado reactivo.
 */
class ListadoViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    // ==========================================
    // ESTADO REACTIVO (UI)
    // ==========================================

    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Loading)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    // Caché en memoria (SSOT temporal) de tipo Dominio (Averia)
    // Permite el filtrado por pestañas de forma instantánea y sin latencia de red.
    private var cacheAverias: List<Averia> = emptyList()

    /**
     * Carga el set de datos inicial desde el servidor.
     * Ejecutado asíncronamente en el hilo principal delegando la E/S al repositorio.
     */
    fun cargarAverias(tipoFiltro: String = "nuevas") {
        viewModelScope.launch {
            _uiState.value = ListadoUiState.Loading

            // 1. Delegamos la obtención segura al repositorio
            val result = repository.getAveriasAsignadas(tipoFiltro)

            // 2. Procesamos el resultado encapsulado
            result.fold(
                onSuccess = { listaAverias ->
                    // BUG CORREGIDO: Poblar la caché en memoria antes de emitir el estado
                    // Si no guardamos esto aquí, los filtros posteriores fallarán.
                    cacheAverias = listaAverias
                    _uiState.value = ListadoUiState.Success(listaAverias)
                },
                onFailure = { exception ->
                    _uiState.value = ListadoUiState.Error(exception.message ?: "Fallo de conexión crítico.")
                }
            )
        }
    }


    /**
     * Patrón Factory para inyección de dependencias estricta.
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