package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tynsolutions.gestionaveriasmovil.data.network.RetrofitClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import kotlinx.coroutines.launch

class DetalleAveriaFragment : Fragment() {

    private var _binding: FragmentDetalleAveriaBinding? = null
    private val binding get() = _binding!!

    // Inyectamos el repositorio real para que el ViewModel funcione
    private val viewModel: DetalleViewModel by viewModels {
        DetalleViewModel.Factory(
            AveriasRepository(
                RetrofitClient.getApiService(requireContext()), // <--- ¡Aquí se usa!
                SessionManager(requireContext())
            )
        )
    }

    private var idActual: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleAveriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observamos el StateFlow usando corrutinas de ciclo de vida
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DetalleUiState.Loading -> { /* Mostrar progress bar si tienes */ }
                    is DetalleUiState.Success -> {
                        idActual = state.averia.id
                        renderizarUI(state.averia)
                    }
                    is DetalleUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    is DetalleUiState.AccionCompletada -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack() // Volvemos al listado tras éxito
                    }
                }
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnAceptarAveria.setOnClickListener {
            if (idActual != -1) viewModel.aceptarAveria(idActual)
        }

        binding.btnFinalizarAveria.setOnClickListener {
            if (idActual != -1) viewModel.finalizarAveria(idActual)
        }

        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun renderizarUI(averia: AveriaItemDTO) {
        // Mapeo profesional de los campos del DTO a la vista
        binding.tvTituloDetalle.text = "Avería #${averia.id}"
        binding.tvMaquinariaDetalle.text = averia.maquinaria?.nombre ?: "Sin nombre"
        binding.tvDescripcionDetalle.text = averia.descripcionAveria

        // Lógica de visibilidad de botones según fechas (Estados)
        if (averia.fechaAcepTecnico == null) {
            binding.btnAceptarAveria.visibility = View.VISIBLE
            binding.btnFinalizarAveria.visibility = View.GONE
        } else if (averia.fechaFinalizTecnico == null) {
            binding.btnAceptarAveria.visibility = View.GONE
            binding.btnFinalizarAveria.visibility = View.VISIBLE
        } else {
            binding.btnAceptarAveria.visibility = View.GONE
            binding.btnFinalizarAveria.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}