package com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados inmutables para la pantalla de registro de intervención.
 * Permiten una gestión determinista de la UI durante las transacciones de red.
 */
sealed class IntervencionUiState {
    object Idle : IntervencionUiState()
    object Loading : IntervencionUiState()
    data class Success(val message: String) : IntervencionUiState()
    data class Error(val message: String) : IntervencionUiState()
}

/**
 * Orquestador lógico para el caso de uso CU04: Registrar Intervención[cite: 53, 99].
 * Realiza la transición de datos desde la entrada del usuario hacia la persistencia remota.
 */
class IntervencionViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntervencionUiState>(IntervencionUiState.Idle)
    val uiState: StateFlow<IntervencionUiState> = _uiState.asStateFlow()

    /**
     * Ejecuta la lógica de persistencia remota para un informe técnico.
     * @param idAveria Identificador de la entidad sobre la que se opera.
     * @param texto Cuerpo del informe técnico.
     */
    fun registrarIntervencion(idAveria: Int, texto: String) {
        if (texto.isBlank()) {
            _uiState.value = IntervencionUiState.Error("La descripción del informe es obligatoria para el cumplimiento normativo.")
            return
        }

        viewModelScope.launch {
            _uiState.value = IntervencionUiState.Loading

            // Invocación al repositorio para la persistencia en la API REST
            val result = repository.registrarIntervencion(idAveria, texto)

            result.fold(
                onSuccess = {
                    _uiState.value = IntervencionUiState.Success("Informe guardado en servidor.")
                },
                onFailure = { _uiState.value = IntervencionUiState.Error(it.message ?: "Fallo crítico en la sincronización del informe.") }
            )
        }
    }

    /**
     * Patrón Factory para inyección de dependencias.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IntervencionViewModel::class.java)) {
                return IntervencionViewModel(repository) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida")
        }
    }
}