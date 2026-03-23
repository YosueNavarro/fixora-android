package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentCambiarEstadoBinding
import kotlinx.coroutines.launch

/**
 * Controlador de la interfaz de usuario para el Bloque 7.2.
 * Captura la decisión del técnico y la delega a la capa de red a través del ViewModel.
 */
class CambiarEstadoFragment : Fragment() {

    private var _binding: FragmentCambiarEstadoBinding? = null
    private val binding get() = _binding!!

    // Inyección segura con Retrofit y Token de sesión
    private val viewModel: CambiarEstadoViewModel by viewModels {
        val sessionManager = SessionManager(requireContext())
        val apiService = ApiClient.getApiService(sessionManager)
        CambiarEstadoViewModel.Factory(AveriasRepository(apiService, sessionManager))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCambiarEstadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosUI()
        setupObservers()
        setupListeners()
    }

    /**
     * Extrae el contexto de la máquina directamente desde la memoria segura (Single Source of Truth).
     */
    private fun cargarDatosUI() {
        val averia = AveriaCache.averiaSeleccionada
        if (averia != null) {
            binding.tvNombreMaquinaActual.text = getString(R.string.formato_nombre_maquina, averia.maquinaria)
            // Al no recibir el estado del backend, dejamos los RadioButtons limpios
            // para forzar una selección consciente por parte del técnico.
        } else {
            Toast.makeText(requireContext(), "Error de integridad: Memoria caché vacía.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Escucha reactivamente los eventos de red.
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CambiarEstadoUiState.Idle -> { /* Estado inicial */ }
                        is CambiarEstadoUiState.Loading -> {
                            // Bloqueamos el botón para evitar envíos duplicados a la base de datos
                            binding.btnGuardarEstadoMaquina.isEnabled = false
                        }
                        is CambiarEstadoUiState.Success -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        is CambiarEstadoUiState.Error -> {
                            binding.btnGuardarEstadoMaquina.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Captura las acciones físicas del usuario.
     */
    private fun setupListeners() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarEstadoMaquina.setOnClickListener {
            val selectedId = binding.rgEstadoMaquina.checkedRadioButtonId

            if (selectedId != -1) {
                val radioButton = binding.root.findViewById<RadioButton>(selectedId)
                val nuevoEstado = radioButton.text.toString()

                // Extraemos el ID de la caché segura y lanzamos la petición
                val idMaquina = AveriaCache.averiaSeleccionada?.id ?: return@setOnClickListener
                viewModel.actualizarEstadoMaquinaria(idMaquina, nuevoEstado)
            } else {
                Toast.makeText(requireContext(), "Operación denegada: Seleccione un estado válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}