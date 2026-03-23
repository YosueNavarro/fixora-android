package com.tynsolutions.gestionaveriasmovil.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Máquina de estados determinista para la pantalla de detalle.
 * Sustituimos el LiveData tradicional por StateFlow para garantizar la inmutabilidad
 * y un manejo reactivo unidireccional (UDF - Unidirectional Data Flow).
 */
sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(val averia: Averia) : DetalleUiState()
    data class Error(val message: String) : DetalleUiState()
    data class AccionCompletada(val message: String) : DetalleUiState()
}

/**
 * Orquestador de lógica de negocio para una avería específica.
 * Aísla a la vista de la capa de red y ejecuta las mutaciones de estado
 * dentro de un contexto asíncrono seguro (viewModelScope).
 */
class DetalleViewModel(private val repository: AveriasRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargarDesdeCache()
    }

    /**
     * Intenta sincronizar los datos con el servidor.
     * Si falla (ej. Error 404), mantiene el estado actual para evitar que la pantalla quede vacía.
     */
    fun recargarDesdeRed(id: Int) {
        viewModelScope.launch {
            repository.getAveriaPorId(id).fold(
                onSuccess = { averiaActualizada ->
                    // Éxito: Actualizamos caché y notificamos a la UI
                    AveriaCache.averiaSeleccionada = averiaActualizada
                    _uiState.value = DetalleUiState.Success(averiaActualizada)
                },
                onFailure = { error ->
                    // Error de red o 404: Mantenemos los datos viejos en pantalla y solo avisamos por log
                    android.util.Log.e("SYNC_ERROR", "No se pudo refrescar el ID $id: ${error.message}")

                    // OPCIONAL: Si quieres que el técnico sepa que el historial puede estar desfasado
                    // _uiState.value = DetalleUiState.Error("Aviso: No se pudo conectar para actualizar el historial.")
                }
            )
        }
    }

    /**
     * Rehidrata el estado de la vista utilizando la información almacenada en la memoria volátil.
     * Se utiliza para restaurar el detalle tras vueltas atrás en la pila de navegación.
     */
    fun cargarDesdeCache() {
        val averia = AveriaCache.averiaSeleccionada
        if (averia != null) {
            _uiState.value = DetalleUiState.Success(averia)
        }
    }

    /**
     * Ejecuta la transacción de aceptación delegando la validación y seguridad al repositorio.
     * @param id Identificador único de la avería.
     */
    fun aceptarAveria(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading
            repository.aceptarAveria(id).fold(
                onSuccess = { _uiState.value = DetalleUiState.AccionCompletada(it) },
                onFailure = { _uiState.value = DetalleUiState.Error(it.message ?: "Error de red al aceptar la avería.") }
            )
        }
    }

    /**
     * Ejecuta la orden de cierre del ciclo de vida de la avería.
     * @param id Identificador único de la avería a clausurar.
     */
    fun finalizarAveria(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading
            repository.finalizarAveria(id).fold(
                onSuccess = { _uiState.value = DetalleUiState.AccionCompletada(it) },
                onFailure = { _uiState.value = DetalleUiState.Error(it.message ?: "Requisitos incumplidos o error al finalizar.") }
            )
        }
    }

    /**
     * Patrón Factory estricto para la inyección del repositorio.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetalleViewModel::class.java)) {
                return DetalleViewModel(repository) as T
            }
            throw IllegalArgumentException("Clase ViewModel no reconocida en el Factory.")
        }
    }
}