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
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentIntervencionBinding
import kotlinx.coroutines.launch

/**
 * Controlador de interfaz para el registro de bitácoras técnicas.
 * Implementa la captura de informes de intervención vinculados a una incidencia mediante
 * el paso de parámetros seguro (Safe Args/Bundle).
 */
class IntervencionFragment : Fragment() {

    private var _binding: FragmentIntervencionBinding? = null
    private val binding get() = _binding!!

    // Recuperación persistente del identificador de la avería
    private val idAveria: Int by lazy { arguments?.getInt(ARG_ID_AVERIA) ?: -1 }

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

        validarIntegridadNavegacion()
        configurarObservadores()
        configurarInteracciones()
    }

    /**
     * Verifica la presencia de los parámetros obligatorios para la operación.
     * En caso de ausencia, aborta la transacción para proteger la integridad del backend.
     */
    private fun validarIntegridadNavegacion() {
        if (idAveria == -1) {
            Log.e(TAG, "Error de navegación: Parámetro 'id_averia' ausente.")
            Toast.makeText(requireContext(), "Error de sistema: Referencia de avería no encontrada.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Suscripción reactiva al estado de la interfaz (UI State).
     * Garantiza que la vista refleje fielmente el estado de la transacción remota.
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
     * Orquestador visual para el feedback del operario.
     */
    private fun manejarCambioEstado(estado: IntervencionUiState) {
        when (estado) {
            is IntervencionUiState.Loading -> {
                // Bloqueo preventivo de UI para evitar duplicidad de registros (Double-tap protection)
                binding.btnGuardarIntervencion.isEnabled = false
            }
            is IntervencionUiState.Success -> {
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            is IntervencionUiState.Error -> {
                binding.btnGuardarIntervencion.isEnabled = true
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                viewModel.resetState() // Limpieza del estado de error para permitir reintentos
            }
            else -> Unit
        }
    }

    /**
     * Establece los manejadores de eventos para la interacción física del técnico.
     */
    private fun configurarInteracciones() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarIntervencion.setOnClickListener {
            val informeCuerpo = binding.etDescripcionIntervencion.text.toString().trim()

            // Delegación de la lógica de guardado al ViewModel
            if (idAveria > 0) {
                viewModel.registrarIntervencion(idAveria, informeCuerpo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevención de Memory Leaks anulando la referencia al ViewBinding
        _binding = null
    }
}