package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados inmutables para gestionar el ciclo de vida de la petición de red.
 * Previene clics múltiples y permite manejar errores del servidor limpiamente.
 */
sealed class CambiarEstadoUiState {
    object Idle : CambiarEstadoUiState()
    object Loading : CambiarEstadoUiState()
    data class Success(val message: String) : CambiarEstadoUiState()
    data class Error(val message: String) : CambiarEstadoUiState()
}

/**
 * Orquestador de la lógica de negocio para el cambio de estado físico de la maquinaria.
 * Aisla a la vista de la complejidad de corrutinas y llamadas de red.
 */
class CambiarEstadoViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CambiarEstadoUiState>(CambiarEstadoUiState.Idle)
    val uiState: StateFlow<CambiarEstadoUiState> = _uiState.asStateFlow()

    /**
     * Transforma la selección humana en un código de estado válido para el backend
     * y delega la ejecución al repositorio en un hilo secundario.
     */
    fun actualizarEstadoMaquinaria(idMaquinaria: Int, estadoTexto: String) {
        viewModelScope.launch {
            _uiState.value = CambiarEstadoUiState.Loading

            // Mapeo defensivo: Convertimos el texto del RadioButton al ID esperado por el backend
            // (Ajusta estos números según la tabla 'Estado' de tu base de datos)
            val codigoEstado = when (estadoTexto) {
                "Operativa" -> 1
                "Averiada" -> 2
                "En mantenimiento" -> 3
                "Fuera de servicio" -> 4
                else -> 2 // Fallback seguro
            }

            // Ejecución de la transacción de red
            // NOTA: Asegúrate de tener este método implementado en tu AveriasRepository
            val result = repository.cambiarEstadoMaquinaria(idMaquinaria, codigoEstado)

            result.fold(
                onSuccess = { _uiState.value = CambiarEstadoUiState.Success(it) },
                onFailure = { _uiState.value = CambiarEstadoUiState.Error(it.message ?: "Fallo al comunicar con el servidor.") }
            )
        }
    }

    /**
     * Factory obligatorio para inyectar el AveriasRepository
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CambiarEstadoViewModel::class.java)) {
                return CambiarEstadoViewModel(repository) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida")
        }
    }
}