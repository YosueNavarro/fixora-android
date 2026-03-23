package com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion

import android.os.Bundle
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
 * Controlador para el registro de intervenciones técnicas.
 * Recibe el ID de la avería mediante argumentos para garantizar la integridad de la petición.
 */
class IntervencionFragment : Fragment() {

    private var _binding: FragmentIntervencionBinding? = null
    private val binding get() = _binding!!

    // ID de la avería recuperado de la navegación
    private var idAveriaRecibido: Int = -1

    private val viewModel: IntervencionViewModel by viewModels {
        val sessionManager = SessionManager(requireContext())
        val apiService = ApiClient.getApiService(sessionManager)
        IntervencionViewModel.Factory(AveriasRepository(apiService, sessionManager))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIntervencionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RECUPERACIÓN DEL ID: Lo sacamos del bundle de navegación
        idAveriaRecibido = arguments?.getInt("id_averia") ?: -1

        // Seguridad: Si no hay ID, no podemos trabajar
        if (idAveriaRecibido == -1) {
            Toast.makeText(requireContext(), "Error: ID no recibido", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is IntervencionUiState.Loading -> binding.btnGuardarIntervencion.isEnabled = false
                        is IntervencionUiState.Success -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            // IMPORTANTE: Antes de salir, podrías emitir un FragmentResult aquí si lo configuramos
                            parentFragmentManager.popBackStack()
                        }
                        is IntervencionUiState.Error -> {
                            binding.btnGuardarIntervencion.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarIntervencion.setOnClickListener {
            val informe = binding.etDescripcionIntervencion.text.toString().trim()

            // LOG DE SEGURIDAD: Verás en el Logcat qué ID estás intentando usar
            android.util.Log.d("DEBUG_ID", "Intentando guardar en avería ID: $idAveriaRecibido")

            if (idAveriaRecibido > 0 && informe.isNotEmpty()) {
                viewModel.registrarIntervencion(idAveriaRecibido, informe)
            } else if (idAveriaRecibido <= 0) {
                Toast.makeText(requireContext(), "Error crítico: ID de avería inválido", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}