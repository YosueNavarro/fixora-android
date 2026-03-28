package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.data.repository.MaquinariaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [DetalleUiState]
 * Representación atómica de los estados de la interfaz.
 * Garantiza un flujo de datos unidireccional (UDF) y previene estados inconsistentes en la vista.
 */
sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(val averia: Averia) : DetalleUiState()
    data class Error(val message: String) : DetalleUiState()
    data class AccionCompletada(val message: String) : DetalleUiState()
}

/**
 * [DetalleViewModel]
 * Orquestador de lógica de negocio para la gestión de incidencias críticas.
 * * DESIGN PATTERNS:
 * - Reactive State Management: Uso de StateFlow para la observación del dominio.
 * - Saga Pattern (Simplified): Gestión de transacciones compuestas entre Averías y Maquinaria.
 * - Optimistic UI: Mutación proactiva de la caché local para mitigar latencias y fallos del backend.
 */
class DetalleViewModel(
    private val averiasRepository: AveriasRepository,
    private val maquinariaRepository: MaquinariaRepository
) : ViewModel() {

    // Encapsulamiento estricto del estado de la UI (Encapsulation Principle)
    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "DetalleViewModel"
    }

    init {
        // Carga inicial inmediata para mejorar el LCP (Largest Contentful Paint) visual
        rehidratarDesdeCache()
    }

    /**
     * Sincroniza la entidad con el servidor remoto.
     * Implementa una estrategia de 'Fallback a Caché' en caso de error de red o de parseo (HTTP 400).
     */
    fun sincronizarConServidor(id: Int) {
        viewModelScope.launch {
            averiasRepository.getAveriaPorId(id).fold(
                onSuccess = { averiaActualizada ->
                    // Sincronización del Single Source of Truth (SSoT)
                    AveriaCache.averiaSeleccionada = averiaActualizada
                    _uiState.value = DetalleUiState.Success(averiaActualizada)
                },
                onFailure = { error ->
                    Log.w(TAG, "Sync fail para ID $id. Motivo: ${error.message}")
                    rehidratarDesdeCache() // Recuperación graciosa ante fallos del servidor
                }
            )
        }
    }

    /**
     * Reconciliación de estado (State Reconciliation).
     * Parchea la entidad con datos históricos de la caché global para asegurar
     * la continuidad visual de la maquinaria ante recargas incompletas del listado.
     */
    fun rehidratarDesdeCache() {
        AveriaCache.averiaSeleccionada?.let { averia ->
            val estadoHistorico = AveriaCache.estadoMaquinasGlobal[averia.maquinaria.id]

            // Lógica de parcheo: Si el servidor devuelve estados vacíos o por defecto,
            // prevalece la memoria local del técnico (Expert Knowledge).
            val averiaReconciliada = if (estadoHistorico != null && (averia.maquinaria.codigoEstado == 0 || averia.maquinaria.codigoEstado == 802)) {
                val maqRestaurada = averia.maquinaria.copy(codigoEstado = estadoHistorico)
                averia.copy(maquinaria = maqRestaurada)
            } else {
                averia
            }

            AveriaCache.averiaSeleccionada = averiaReconciliada
            _uiState.value = DetalleUiState.Success(averiaReconciliada)

        } ?: run {
            _uiState.value = DetalleUiState.Error("Contexto de datos perdido. Reingrese desde el panel principal.")
        }
    }

    /**
     * Helper: Genera marcas de tiempo ISO-8601 simuladas para el motor de actualización optimista.
     */
    private fun obtenerFechaHoraActualFormateada(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }

    // =========================================================================
    // TRANSACCIONES COMPUESTAS (SAGA / OPTIMISTIC UPDATES)
    // =========================================================================

    /**
     * [aceptarAveriaYCambiarMaquina]
     * Transiciona la avería a estado activo y el hardware a mantenimiento (803).
     * Inyecta marcas de tiempo locales para garantizar que la UI refleje el cambio de estado
     * inmediatamente, ignorando temporalmente la inconsistencia de fechas del backend.
     */
    fun aceptarAveriaYCambiarMaquina(idAveria: Int, idMaquina: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading

            // Bloque Atómico de Red
            val resultAveria = averiasRepository.aceptarAveria(idAveria)
            if (resultAveria.isSuccess) {
                val resultMaquina = maquinariaRepository.cambiarEstadoMaquinaria(idMaquina, 803)

                if (resultMaquina.isSuccess) {
                    // Sincronización proactiva de la caché L1
                    AveriaCache.estadoMaquinasGlobal[idMaquina] = 803

                    AveriaCache.averiaSeleccionada?.let { averiaActual ->
                        val maquinaActualizada = averiaActual.maquinaria.copy(codigoEstado = 803)

                        // Mutación Optimista: Inyectamos fecha local para forzar el cambio de badge en la UI
                        AveriaCache.averiaSeleccionada = averiaActual.copy(
                            maquinaria = maquinaActualizada,
                            fechaAceptacion = averiaActual.fechaAceptacion ?: obtenerFechaHoraActualFormateada()
                        )
                    }
                    _uiState.value = DetalleUiState.AccionCompletada("Incidencia aceptada: Hardware en mantenimiento.")
                } else {
                    _uiState.value = DetalleUiState.Error("Avería registrada, pero falló la actualización del hardware.")
                }
            } else {
                _uiState.value = DetalleUiState.Error("Error crítico al procesar la aceptación de la avería.")
            }
        }
    }

    /**
     * [finalizarAveriaYCambiarMaquina]
     * Cierra el ciclo de vida de la incidencia y libera el hardware al estado operativo deseado.
     * Implementa la misma lógica de inyección de fechas locales para asegurar el refresco de la vista.
     */
    fun finalizarAveriaYCambiarMaquina(idAveria: Int, idMaquina: Int, codigoEstadoFinal: Int) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Loading

            val resultMaquina = maquinariaRepository.cambiarEstadoMaquinaria(idMaquina, codigoEstadoFinal)
            if (resultMaquina.isSuccess) {
                val resultAveria = averiasRepository.finalizarAveria(idAveria)

                if (resultAveria.isSuccess) {
                    // Actualización de la caché global de estados
                    AveriaCache.estadoMaquinasGlobal[idMaquina] = codigoEstadoFinal

                    AveriaCache.averiaSeleccionada?.let { averiaActual ->
                        val maquinaActualizada = averiaActual.maquinaria.copy(codigoEstado = codigoEstadoFinal)

                        // Mutación Optimista de cierre para habilitar el modo lectura en la UI
                        AveriaCache.averiaSeleccionada = averiaActual.copy(
                            maquinaria = maquinaActualizada,
                            fechaFinalizacion = averiaActual.fechaFinalizacion ?: obtenerFechaHoraActualFormateada()
                        )
                    }
                    _uiState.value = DetalleUiState.AccionCompletada("Operación finalizada: Registro de hardware actualizado.")
                } else {
                    _uiState.value = DetalleUiState.Error("Estatus de máquina actualizado, pero falló el cierre administrativo.")
                }
            } else {
                _uiState.value = DetalleUiState.Error("Fallo de comunicación con el módulo de maquinaria. Cierre denegado.")
            }
        }
    }

    /**
     * [Factory]
     * Inyección manual de dependencias. Centraliza la creación del ViewModel y
     * garantiza el desacoplamiento entre la capa de UI y la de Datos.
     */
    class Factory(
        private val averiasRepo: AveriasRepository,
        private val maqRepo: MaquinariaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetalleViewModel::class.java)) {
                return DetalleViewModel(averiasRepo, maqRepo) as T
            }
            throw IllegalArgumentException("Fallo en la resolución del Provider: Clase incompatible.")
        }
    }
}