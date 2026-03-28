package com.tynsolutions.gestionaveriasmovil.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Representación atómica del estado de la interfaz de usuario.
 * Se utiliza una 'sealed class' para garantizar un manejo de estados exhaustivo en la vista,
 * evitando estados inconsistentes (illegal states) durante el ciclo de vida.
 */
sealed class ListadoUiState {
    object Loading : ListadoUiState()
    data class Success(val averias: List<Averia>) : ListadoUiState()
    data class Error(val message: String) : ListadoUiState()
}

/**
 * ViewModel: Orquestador de la lógica de presentación bajo el patrón MVVM.
 * * DESIGN NOTES:
 * 1. Mantiene el principio de Single Source of Truth (SSoT) a través de [uiState].
 * 2. Desacopla la lógica de filtrado y ordenación del hilo principal (UI Thread).
 * 3. Utiliza inyección de dependencias vía Constructor para facilitar Unit Testing.
 */
class ListadoViewModel(
    private val repository: AveriasRepository
) : ViewModel() {

    // Encapsulamiento de estado: El MutableStateFlow es privado para evitar mutaciones externas (Read-only exposure)
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Loading)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    // Caché volátil para operaciones de consulta rápida sin re-fetching de red
    private var cacheAverias: List<Averia> = emptyList()

    /**
     * Coordina la obtención de incidencias y aplica reglas de negocio de visualización.
     * * ESTRATEGIA DE DEPURACIÓN:
     * Se introduce 'Dispatchers.Default' para el procesamiento de listas grandes.
     * Ordenar y filtrar colecciones en el Main Thread puede causar 'Jank' (caída de FPS).
     *
     * @param tipoFiltro Categoría de la incidencia (nuevas, en_curso, historico).
     */
    fun cargarAverias(tipoFiltro: String = "nuevas") {
        viewModelScope.launch {
            _uiState.value = ListadoUiState.Loading

            // Ejecución de la llamada a red/DB en el contexto del repositorio
            val result = repository.getAveriasAsignadas(tipoFiltro)

            result.fold(
                onSuccess = { listaAverias ->
                    // Optimizamos: El procesamiento pesado (filtrado/ordenación) se mueve a hilos de cómputo
                    val averiasProcesadas = withContext(Dispatchers.Default) {

                        // 1. DATA SANITIZATION: Eliminamos ruido de la API (entidades sin fecha de asignación)
                        val filtradas = listaAverias.filter { it.fechaAsignacion != null }

                        // 2. SLA PRIORITIZATION ENGINE:
                        // Implementamos lógica de negocio según el contexto operativo del técnico.
                        when (tipoFiltro) {
                            "nuevas" -> {
                                // FIFO: Prioridad a la incidencia más antigua en espera
                                filtradas.sortedBy { it.fechaAsignacion }
                            }
                            "en_curso" -> {
                                // Priorización por hito de aceptación (Work-in-Progress Aging)
                                filtradas.sortedBy { it.fechaAceptacion ?: it.fechaAsignacion }
                            }
                            "historico" -> {
                                // Orden cronológico inverso para facilitar auditoría de cierres recientes
                                filtradas.sortedByDescending { it.fechaFinalizacion ?: "" }
                            }
                            else -> filtradas
                        }
                    }

                    // Sincronización del estado de memoria y despacho a la UI
                    cacheAverias = averiasProcesadas
                    _uiState.value = ListadoUiState.Success(averiasProcesadas)
                },
                onFailure = { exception ->
                    // Manejo de excepciones con fallback descriptivo para facilitar el debug en producción
                    _uiState.value = ListadoUiState.Error(
                        exception.message ?: "Excepción no controlada en la capa de datos."
                    )
                }
            )
        }
    }

    /**
     * Factory de Inyección de Dependencias.
     * Imprescindible para instanciar ViewModels que requieren argumentos en el constructor,
     * respetando el ciclo de vida de la arquitectura de Android.
     */
    class Factory(private val repository: AveriasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ListadoViewModel::class.java)) {
                return ListadoViewModel(repository) as T
            }
            throw IllegalArgumentException("No se puede instanciar el ViewModel: Referencia de clase inválida.")
        }
    }
}