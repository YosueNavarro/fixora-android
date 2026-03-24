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
 * Jerarquía de estados inmutables para el flujo de registro de intervenciones.
 * Define un modelo determinista para la gestión de transacciones de red y feedback al operario.
 */
sealed class IntervencionUiState {
    object Idle : IntervencionUiState()
    object Loading : IntervencionUiState()
    data class Success(val message: String) : IntervencionUiState()
    data class Error(val message: String) : IntervencionUiState()
}

/**
 * Orquestador de lógica de presentación para el registro de informes técnicos.
 * Gestiona la validación de entrada y la persistencia asíncrona mediante el patrón Repository.
 */
class IntervencionViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    // Encapsulamiento de estado reactivo mediante StateFlow
    private val _uiState = MutableStateFlow<IntervencionUiState>(IntervencionUiState.Idle)
    val uiState: StateFlow<IntervencionUiState> = _uiState.asStateFlow()

    /**
     * Inicia la transacción asíncrona para la persistencia del informe técnico.
     * Implementa una validación previa (Fail-fast) para asegurar el cumplimiento normativo
     * antes de comprometer recursos de red.
     *
     * @param idAveria Clave primaria de la incidencia objetivo.
     * @param texto Cuerpo descriptivo del procedimiento técnico realizado.
     */
    fun registrarIntervencion(idAveria: Int, texto: String) {
        // Validación de integridad de datos en la capa de presentación
        if (texto.isBlank()) {
            _uiState.value = IntervencionUiState.Error("La descripción del procedimiento es obligatoria para la trazabilidad.")
            return
        }

        viewModelScope.launch {
            _uiState.value = IntervencionUiState.Loading

            // Delegación de la persistencia a la capa de datos
            val result = repository.registrarIntervencion(idAveria, texto)

            result.fold(
                onSuccess = {
                    _uiState.value = IntervencionUiState.Success("Informe técnico sincronizado correctamente.")
                },
                onFailure = {
                    _uiState.value = IntervencionUiState.Error(it.message ?: "Fallo crítico en la sincronización del registro.")
                }
            )
        }
    }

    /**
     * Purga el estado actual de la UI.
     * Utilizado para limpiar mensajes de error o éxito tras la interacción del usuario.
     */
    fun resetState() {
        _uiState.value = IntervencionUiState.Idle
    }

    /**
     * Factory de inyección de dependencias para la instanciación del ViewModel.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IntervencionViewModel::class.java)) {
                return IntervencionViewModel(repository) as T
            }
            throw IllegalArgumentException("Asignación de ViewModel inválida: Clase incompatible.")
        }
    }
}