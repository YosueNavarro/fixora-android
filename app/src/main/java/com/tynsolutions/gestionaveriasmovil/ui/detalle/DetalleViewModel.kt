package com.tynsolutions.gestionaveriasmovil.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Los posibles estados de la pantalla de detalle.
 * Sustituimos el LiveData simple por un sellado de estados para manejar errores de red.
 */
sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(val averia: AveriaItemDTO) : DetalleUiState()
    data class Error(val message: String) : DetalleUiState()
    data class AccionCompletada(val message: String) : DetalleUiState()
}

class DetalleViewModel(private val repository: AveriasRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargarDesdeCache()
    }

    private fun cargarDesdeCache() {
        // Recuperamos el objeto que el listado dejó en la "mesa de trabajo"
        val averia = AveriaCache.averiaSeleccionada
        if (averia != null) {
            _uiState.value = DetalleUiState.Success(averia)
        } else {
            _uiState.value = DetalleUiState.Error("No se encontró la avería en memoria.")
        }
    }

    fun aceptarAveria(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading
            repository.aceptarAveria(id).fold(
                onSuccess = { _uiState.value = DetalleUiState.AccionCompletada(it) },
                onFailure = { _uiState.value = DetalleUiState.Error(it.message ?: "Error al aceptar") }
            )
        }
    }

    fun finalizarAveria(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading
            repository.finalizarAveria(id).fold(
                onSuccess = { _uiState.value = DetalleUiState.AccionCompletada(it) },
                onFailure = { _uiState.value = DetalleUiState.Error(it.message ?: "Error al finalizar") }
            )
        }
    }

    // El Factory es OBLIGATORIO para pasarle el repositorio al ViewModel
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetalleViewModel(repository) as T
        }
    }
}