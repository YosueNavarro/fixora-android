package com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache // Importación crítica para la mutación de estado local
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentIntervencionBinding
import kotlinx.coroutines.launch

/**
 * Controlador de vista (UI Controller) responsable del registro de bitácoras técnicas.
 * Implementa un patrón de actualización optimista (Optimistic UI) para aislar al cliente
 * de posibles inconsistencias en el contrato de red del backend (ej. fallos de parseo de fechas).
 */
class IntervencionFragment : Fragment() {

    private var _binding: FragmentIntervencionBinding? = null
    // Delegado de acceso seguro a la vista. Solo válido entre onCreateView y onDestroyView.
    private val binding get() = _binding!!

    // Resolución perezosa (Lazy) del identificador de dominio. Mitiga inicializaciones prematuras.
    private val idAveria: Int by lazy { arguments?.getInt(ARG_ID_AVERIA) ?: -1 }

    private val viewModel: IntervencionViewModel by viewModels {
        val session = SessionManager(requireContext())
        // Inyección de dependencias manual (Locator Pattern) para el repositorio de red
        val repository = AveriasRepository(ApiClient.getApiService(session), session)
        IntervencionViewModel.Factory(repository)
    }

    companion object {
        private const val ARG_ID_AVERIA = "id_averia"
        private const val TAG = "IntervencionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntervencionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validarIntegridadNavegacion()
        configurarObservadores()
        configurarInteracciones()
    }

    /**
     * Auditoría de precondiciones de navegación.
     * Previene operaciones de mutación huérfanas asegurando la existencia de la FK (idAveria).
     */
    private fun validarIntegridadNavegacion() {
        if (idAveria == -1) {
            Log.e(TAG, "Violación de precondición: Parámetro 'id_averia' ausente o nulo.")
            Toast.makeText(requireContext(), "Error de integridad: Identificador de incidencia no válido.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Establece la suscripción al pipeline de estados emitido por el ViewModel.
     * Utiliza 'repeatOnLifecycle' para garantizar la recolección segura (Lifecycle-aware)
     * y evitar fugas de memoria o crashes en background.
     */
    private fun configurarObservadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    manejarCambioEstado(estado)
                }
            }
        }
    }

    /**
     * Orquesta las transiciones de la máquina de estados visual.
     * Implementa la estrategia de resiliencia de caché tras la confirmación del servidor.
     */
    private fun manejarCambioEstado(estado: IntervencionUiState) {
        when (estado) {
            is IntervencionUiState.Loading -> {
                // Prevención activa de concurrencia: Bloqueo de doble inserción accidental
                binding.btnGuardarIntervencion.isEnabled = false
            }
            is IntervencionUiState.Success -> {
                // =========================================================================
                // ACTUALIZACIÓN OPTIMISTA DE CACHÉ (Optimistic UI Update)
                // =========================================================================
                // Al recibir el HTTP 200 OK, inyectamos el payload directamente en la memoria
                // RAM (AveriaCache) para compensar el fallo de deserialización del endpoint GET.
                val textoIntervencion = binding.etDescripcionIntervencion.text.toString().trim()
                val averiaActual = AveriaCache.averiaSeleccionada

                if (averiaActual != null && textoIntervencion.isNotEmpty()) {
                    // Mantenemos la inmutabilidad de la Data Class clonando la colección
                    val listaActualizada = averiaActual.intervenciones.toMutableList()
                    listaActualizada.add(textoIntervencion)

                    // Sobrescribimos el Singleton local con el estado mutado
                    AveriaCache.averiaSeleccionada = averiaActual.copy(intervenciones = listaActualizada)
                    Log.i(TAG, "Caché de L1 mutada exitosamente. Elementos actuales: ${listaActualizada.size}")
                }
                // =========================================================================

                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Retorno al contexto padre (DetalleAveria)
            }
            is IntervencionUiState.Error -> {
                // Liberación del cerrojo de UI para permitir reintentos manuales tras fallo de red
                binding.btnGuardarIntervencion.isEnabled = true
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                viewModel.resetState() // Purga del estado transitorio
            }
            else -> Unit // No-op para estados iniciales o inactivos
        }
    }

    /**
     * Vincula las acciones del usuario (inputs) con la capa de presentación (ViewModel).
     */
    private fun configurarInteracciones() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarIntervencion.setOnClickListener {
            val informeCuerpo = binding.etDescripcionIntervencion.text.toString().trim()

            if (informeCuerpo.isEmpty()) {
                binding.etDescripcionIntervencion.error = "El informe técnico no puede estar vacío."
                return@setOnClickListener
            }

            // Despacho de la orden de persistencia hacia la capa de datos
            if (idAveria > 0) {
                viewModel.registrarIntervencion(idAveria, informeCuerpo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Destrucción obligatoria del ViewBinding para evitar retenciones de memoria (Memory Leaks)
        _binding = null
    }
}