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

    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Loading)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    private var cacheAverias: List<Averia> = emptyList()

    /**
     * Sincroniza el listado de averías con el servidor perimetral.
     * Implementa sanitización de datos (Data Sanitization) para asegurar la integridad
     * de las reglas de negocio antes de la renderización visual.
     *
     * @param tipoFiltro Criterio de segmentación ("nuevas", "en_curso", "historico").
     */
    fun cargarAverias(tipoFiltro: String = "nuevas") {
        viewModelScope.launch {
            _uiState.value = ListadoUiState.Loading

            val result = repository.getAveriasAsignadas(tipoFiltro)

            result.fold(
                onSuccess = { listaAverias ->

                    // ====================================================================
                    // 🛡️ BARRERA DE DEFENSA (Zero Tolerance Policy)
                    // Regla de Negocio: Un técnico NO puede estar asignado sin fecha.
                    // Descartamos silenciosamente cualquier anomalía proveniente de la API.
                    // ====================================================================
                    val averiasValidas = listaAverias.filter { averia ->
                        averia.fechaAsignacion != null
                    }

                    // Actualización de la memoria y la UI EXCLUSIVAMENTE con datos íntegros
                    cacheAverias = averiasValidas
                    _uiState.value = ListadoUiState.Success(averiasValidas)
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