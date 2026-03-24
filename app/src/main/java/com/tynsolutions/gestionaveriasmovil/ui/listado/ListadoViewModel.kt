package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Jerarquía de estados inmutables para la orquestación del listado de incidencias.
 * Implementa una arquitectura de flujo de datos unidireccional (UDF) para garantizar
 * la consistencia visual y el aislamiento de la lógica de red.
 */
sealed class ListadoUiState {
    object Loading : ListadoUiState()
    data class Success(val averias: List<Averia>) : ListadoUiState()
    data class Error(val message: String) : ListadoUiState()
}

/**
 * Orquestador de la lógica de presentación para el catálogo de averías técnicas.
 * Actúa como mediador reactivo entre el [AveriasRepository] y la UI, manteniendo
 * una caché volátil para operaciones de filtrado rápido y gestión de estados.
 */
class ListadoViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    // Encapsulamiento del estado reactivo: Solo el ViewModel puede mutar el flujo (Internal State).
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Loading)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    // Single Source of Truth (SSoT) en memoria para permitir filtrado reactivo sin latencia.
    private var cacheAverias: List<Averia> = emptyList()

    /**
     * Sincroniza el listado de averías con el servidor perimetral basándose en un criterio de filtrado.
     * La operación se ejecuta en el ámbito de vida del ViewModel (Memory Safe), cancelándose
     * automáticamente si el técnico abandona la vista.
     *
     * @param tipoFiltro Criterio de segmentación ("nuevas", "en_curso", "historico").
     */
    fun cargarAverias(tipoFiltro: String = "nuevas") {
        viewModelScope.launch {
            _uiState.value = ListadoUiState.Loading

            // Ejecución de la consulta de dominio con gestión de resultados (Result Pattern)
            val result = repository.getAveriasAsignadas(tipoFiltro)

            result.fold(
                onSuccess = { listaAverias ->
                    // Actualización de la caché de dominio para operaciones de UI posteriores
                    cacheAverias = listaAverias
                    _uiState.value = ListadoUiState.Success(listaAverias)
                },
                onFailure = { exception ->
                    _uiState.value = ListadoUiState.Error(
                        exception.message ?: "Excepción no tipificada en la sincronización de datos."
                    )
                }
            )
        }
    }

    /**
     * Factory para la inyección de dependencias.
     * Garantiza la instanciación correcta del ViewModel cumpliendo con el patrón de inversión de control.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ListadoViewModel::class.java)) {
                return ListadoViewModel(repository) as T
            }
            throw IllegalArgumentException("Fallo en la resolución del Factory: Clase ViewModel incompatible.")
        }
    }
}