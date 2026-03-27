package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.repository.MaquinariaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache

/**
 * Jerarquía de estados inmutables para la gestión reactiva de la interfaz.
 * Implementa el patrón State Pattern para garantizar que la vista solo reaccione
 * a estados finitos y consistentes, mitigando condiciones de carrera.
 */
sealed class CambiarEstadoUiState {
    object Idle : CambiarEstadoUiState()
    object Loading : CambiarEstadoUiState()
    data class Success(val message: String) : CambiarEstadoUiState()
    data class Error(val message: String) : CambiarEstadoUiState()
}

/**
 * Orquestador de la lógica de presentación para la mutación del estatus operativo de maquinaria.
 * Centraliza la gestión de corrutinas y la transformación de tipos para desacoplar
 * la vista (Fragment) de las reglas de persistencia del Backend.
 */
class CambiarEstadoViewModel(
    private val repository: MaquinariaRepository // <- REFACTORIZACIÓN: SRP Cumplido
) : ViewModel() {

    // Encapsulamiento estricto: El MutableStateFlow es privado para evitar mutaciones externas
    private val _uiState = MutableStateFlow<CambiarEstadoUiState>(CambiarEstadoUiState.Idle)
    val uiState: StateFlow<CambiarEstadoUiState> = _uiState.asStateFlow()

    /**
     * Inicia la transacción asíncrona para actualizar el estatus físico del hardware.
     * Gestiona el ciclo de vida de la corrutina vinculado al ViewModel, garantizando
     * la cancelación automática si el usuario abandona la pantalla (Memory Safety).
     *
     * @param idMaquinaria Clave primaria del activo.
     * @param estadoTexto Etiqueta descriptiva proveniente de la selección del usuario.
     */
    fun actualizarEstadoMaquinaria(idMaquinaria: Int, estadoTexto: String) {
        viewModelScope.launch {
            _uiState.value = CambiarEstadoUiState.Loading

            // Delegación del mapeo de dominio a código de catálogo (RBAC/Integridad)
            val codigoEstado = mapearTextoACodigo(estadoTexto)

            // Consumo del servicio de dominio con gestión de resultados (Result Pattern)
            val result = repository.cambiarEstadoMaquinaria(idMaquinaria, codigoEstado)

            result.fold(
                onSuccess = { mensaje ->
                    // 1. Auditoría In-Memory: Registramos el cambio en el diccionario global
                    // Esto garantiza que el estado sobreviva aunque el listado sobrescriba la avería
                    AveriaCache.estadoMaquinasGlobal[idMaquinaria] = codigoEstado

                    // 2. Actualización en tiempo real de la entidad seleccionada
                    val averiaActual = AveriaCache.averiaSeleccionada
                    if (averiaActual != null) {
                        val maquinaActualizada = averiaActual.maquinaria.copy(codigoEstado = codigoEstado)
                        AveriaCache.averiaSeleccionada = averiaActual.copy(maquinaria = maquinaActualizada)
                    }

                    _uiState.value = CambiarEstadoUiState.Success(mensaje)
                },
                onFailure = { error ->
                    _uiState.value = CambiarEstadoUiState.Error(error.message ?: "Fallo crítico en la sincronización remota.")
                }
            )
        }
    }

    /**
     * Resetea el estado de la UI al valor inicial.
     * Fundamental para limpiar buffers de error o éxito tras la interacción del usuario.
     */
    fun resetState() {
        _uiState.value = CambiarEstadoUiState.Idle
    }

    /**
     * Diccionario determinista de conversión entre la capa de presentación y el contrato de la base de datos.
     * Centralizar el mapeo aquí previene la dispersión de IDs mágicos (Magic Numbers) por el código.
     */
    private fun mapearTextoACodigo(texto: String): Int = when (texto) {
        "Operativa" -> 801
        "Averiada" -> 802
        "En mantenimiento" -> 803
        "Fuera de servicio" -> 804
        else -> 802 // Fallback defensivo: 'Averiada' ante ambigüedad o error tipográfico
    }

    /**
     * Factory de inyección de dependencias.
     * Provee el repositorio especializado manteniendo el desacoplamiento entre capas.
     */
    class Factory(private val repository: MaquinariaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CambiarEstadoViewModel::class.java)) {
                return CambiarEstadoViewModel(repository) as T
            }
            throw IllegalArgumentException("Asignación de ViewModel inválida: Clase incompatible.")
        }
    }
}