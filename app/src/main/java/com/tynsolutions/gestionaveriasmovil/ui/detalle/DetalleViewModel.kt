package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.util.Log
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
 * Jerarquía de estados para la vista de detalle.
 * Implementa el patrón Redux-like para garantizar una representación visual
 * determinista basada en el estado actual del dominio.
 */
sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(val averia: Averia) : DetalleUiState()
    data class Error(val message: String) : DetalleUiState()
    data class AccionCompletada(val message: String) : DetalleUiState()
}

/**
 * Orquestador de lógica de presentación para la gestión profunda de incidencias.
 * Centraliza las operaciones de lectura, sincronización y mutación de estado administrativo,
 * garantizando la integridad de la caché local tras cada transacción exitosa.
 */
class DetalleViewModel(private val repository: AveriasRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "DetalleViewModel"
    }

    init {
        rehidratarDesdeCache()
    }

    /**
     * Sincroniza la entidad actual con el estado remoto del servidor.
     * Implementa una estrategia de "Graceful Degradation": si la red falla, se mantiene
     * el estado previo de la caché para no interrumpir el flujo del técnico.
     *
     * @param id Identificador único de la avería.
     */
    fun sincronizarConServidor(id: Int) {
        viewModelScope.launch {
            repository.getAveriaPorId(id).fold(
                onSuccess = { averiaActualizada ->
                    // Sincronización de la Single Source of Truth (SSoT)
                    AveriaCache.averiaSeleccionada = averiaActualizada
                    _uiState.value = DetalleUiState.Success(averiaActualizada)
                },
                onFailure = { error ->
                    Log.w(TAG, "Fallo de sincronización para ID $id. Motivo: ${error.message}")
                    // No sobreescribimos el estado Success si ya existe, para evitar parpadeos de error.
                }
            )
        }
    }

    /**
     * Recupera la última instantánea conocida de la avería desde la memoria volátil.
     * Permite una carga instantánea de la UI mientras se disparan procesos de red en paralelo.
     */
    fun rehidratarDesdeCache() {
        AveriaCache.averiaSeleccionada?.let {
            _uiState.value = DetalleUiState.Success(it)
        } ?: run {
            _uiState.value = DetalleUiState.Error("Referencia de datos perdida. Reingrese desde el listado.")
        }
    }

    /**
     * Ejecuta la transición de estado a 'Recibida'.
     * @param id Identificador de la avería.
     */
    fun aceptarAveria(id: Int) {
        realizarTransaccion { repository.aceptarAveria(id) }
    }

    /**
     * Ejecuta el cierre definitivo del ciclo de vida de la incidencia.
     * @param id Identificador de la avería.
     */
    fun finalizarAveria(id: Int) {
        realizarTransaccion { repository.finalizarAveria(id) }
    }

    /**
     * Función de orden superior para estandarizar el manejo de transacciones de escritura.
     * Reduce la duplicidad de código (DRY) y centraliza el manejo de estados de carga y error.
     */
    private fun realizarTransaccion(block: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading
            block().fold(
                onSuccess = { _uiState.value = DetalleUiState.AccionCompletada(it) },
                onFailure = { _uiState.value = DetalleUiState.Error(it.message ?: "Error en la transacción remota.") }
            )
        }
    }

    /**
     * Factory de inyección de dependencias para el desacoplamiento de capas.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetalleViewModel::class.java)) {
                return DetalleViewModel(repository) as T
            }
            throw IllegalArgumentException("Fallo en la instanciación: Clase ViewModel incompatible.")
        }
    }
}