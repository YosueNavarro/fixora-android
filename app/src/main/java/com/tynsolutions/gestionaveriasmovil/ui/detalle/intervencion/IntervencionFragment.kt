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
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentIntervencionBinding
import kotlinx.coroutines.launch

/**
 * [IntervencionFragment]
 * Controlador de vista (UI Controller) encargado de la persistencia de bitácoras técnicas.
 * * ARQUITECTURA Y PATRONES:
 * - Unidirectional Data Flow (UDF): Reacciona a estados inmutables del ViewModel.
 * - Optimistic UI: Actualiza proactivamente el estado local para mitigar latencias de red.
 * - SSoT (Single Source of Truth): Mantiene la integridad de los datos mediante [AveriaCache].
 */
class IntervencionFragment : Fragment() {

    // Patrón de Backing Property para ViewBinding: Garantiza seguridad de nulidad
    // y previene fugas de memoria al limpiar la referencia en onDestroyView.
    private var _binding: FragmentIntervencionBinding? = null
    private val binding get() = _binding!!

    // Propiedad delegada 'lazy': Asegura que el acceso a argumentos ocurra solo cuando
    // el fragmento esté adjunto, evitando IllegalStateExceptions prematuros.
    private val idAveria: Int by lazy { arguments?.getInt(ARG_ID_AVERIA) ?: -1 }

    /**
     * Inyección manual de dependencias vía Factory.
     * Desacopla la creación del ViewModel de sus dependencias (Repository/API),
     * facilitando la escalabilidad y las pruebas unitarias.
     */
    private val viewModel: IntervencionViewModel by viewModels {
        val session = SessionManager(requireContext())
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

        // Validación de precondiciones de navegación (Fail-fast principle)
        validarIntegridadNavegacion()

        // Renderizado inicial síncrono para evitar parpadeos visuales (Layout Thrashing)
        renderizarHistorialIntervenciones()

        configurarObservadores()
        configurarInteracciones()
    }

    /**
     * Auditoría de integridad de navegación.
     * Actúa como barrera defensiva contra flujos de navegación inconsistentes
     * donde el identificador de dominio sea nulo o inválido.
     */
    private fun validarIntegridadNavegacion() {
        if (idAveria == -1) {
            Log.e(TAG, "Violación de precondición: Parámetro 'id_averia' ausente o nulo.")
            Toast.makeText(requireContext(), "Error de integridad: Identificador de incidencia no válido.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Sincronización de vista con la Caché L1 (SSoT).
     * Transforma la colección de dominio en una representación visual jerárquica
     * mediante prefijos (bullets) para optimizar la escaneabilidad técnica.
     */
    private fun renderizarHistorialIntervenciones() {
        val averia = AveriaCache.averiaSeleccionada ?: return

        with(binding) {
            val tieneIntervenciones = averia.intervenciones.isNotEmpty()

            // Gestión de visibilidad dinámica (Conditional Rendering)
            tvIntervencionesLabel.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE
            tvIntervencionesLista.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE

            if (tieneIntervenciones) {
                // Transformación funcional de colecciones: O(n)
                tvIntervencionesLista.text = averia.intervenciones.joinToString("\n") { "• $it" }
            }
        }
    }

    /**
     * Pipeline de observación de estado reactivo.
     * 'repeatOnLifecycle(STARTED)' garantiza que la corrutina se suspenda cuando
     * la app está en background, ahorrando CPU y ciclos de batería.
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
     * Máquina de estados de la UI.
     * Orquesta la interactividad y la persistencia optimista basada en la
     * respuesta determinista del flujo del ViewModel.
     */
    private fun manejarCambioEstado(estado: IntervencionUiState) {
        when (estado) {
            is IntervencionUiState.Loading -> {
                // Prevención de ataques de reentrada: Bloqueo de doble inserción accidental (Double-tap)
                binding.btnGuardarIntervencion.isEnabled = false
            }
            is IntervencionUiState.Success -> {
                // =========================================================================
                // ACTUALIZACIÓN OPTIMISTA DE CACHÉ (Manual State Reconciliation)
                // Mutamos el estado local tras la confirmación HTTP 200 del servidor
                // para eludir latencias de re-fetching y fallos de deserialización.
                // =========================================================================
                val textoIntervencion = binding.etDescripcionIntervencion.text.toString().trim()
                val averiaActual = AveriaCache.averiaSeleccionada

                if (averiaActual != null && textoIntervencion.isNotEmpty()) {
                    // Mantenemos la inmutabilidad clonando la colección (Deep Copy)
                    val listaActualizada = averiaActual.intervenciones.toMutableList()
                    listaActualizada.add(textoIntervencion)

                    // Sobrescribimos el Singleton de caché con la nueva instancia de datos
                    AveriaCache.averiaSeleccionada = averiaActual.copy(intervenciones = listaActualizada)
                    Log.i(TAG, "Caché L1 actualizada proactivamente. N: ${listaActualizada.size}")
                }
                // =========================================================================

                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Retorno al contexto padre
            }
            is IntervencionUiState.Error -> {
                // Liberación del cerrojo de UI para permitir reintentos manuales tras fallo transitorio
                binding.btnGuardarIntervencion.isEnabled = true
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                viewModel.resetState() // Limpieza de estado de error (Purge)
            }
            else -> Unit
        }
    }

    /**
     * Registro de manejadores de eventos (Input Intents).
     * Aplica sanitización de cadenas (trim) previo al despacho al ViewModel.
     */
    private fun configurarInteracciones() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarIntervencion.setOnClickListener {
            val informeCuerpo = binding.etDescripcionIntervencion.text.toString().trim()

            // Validación de campo obligatorio (Input Sanitization)
            if (informeCuerpo.isEmpty()) {
                binding.etDescripcionIntervencion.error = "El informe técnico no puede estar vacío."
                return@setOnClickListener
            }

            // Despacho asíncrono hacia la capa de presentación
            if (idAveria > 0) {
                viewModel.registrarIntervencion(idAveria, informeCuerpo)
            }
        }
    }

    /**
     * Limpieza determinista de recursos.
     * La anulación de _binding es obligatoria para evitar Memory Leaks,
     * ya que los fragmentos suelen sobrevivir a sus vistas en el backstack.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}