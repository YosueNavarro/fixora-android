package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentListadoAveriasBinding
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import kotlinx.coroutines.launch

class ListadoAveriasFragment : Fragment() {

    private var _binding: FragmentListadoAveriasBinding? = null
    private val binding get() = _binding!!

    // Inyección manual del ViewModel usando nuestro Factory y el Repositorio real
    private lateinit var viewModel: ListadoViewModel
    private lateinit var adapter: AveriasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListadoAveriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Instanciamos las dependencias reales (Caja fuerte + Cable de red)
        val sessionManager = SessionManager(requireContext())
        val apiService = ApiClient.getApiService(sessionManager)
        val repository = AveriasRepository(apiService, sessionManager)

        // 2. Creamos el ViewModel inyectándole el repositorio
        val factory = ListadoViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[ListadoViewModel::class.java]

        setupRecyclerView()
        setupFiltros()
        setupObservers()

        viewModel.cargarAverias()
    }

    private fun setupRecyclerView() {
        adapter = AveriasAdapter { idAveriaSeleccionada ->
            abrirDetalle(idAveriaSeleccionada)
        }
        binding.rvAverias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAverias.adapter = adapter
    }

    private fun setupObservers() {
        // Recolectamos el StateFlow de forma segura respetando el ciclo de vida de Android
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ListadoUiState.Loading -> {
                            // Aquí podrías mostrar un ProgressBar (binding.progressBar.visibility = View.VISIBLE)
                        }
                        is ListadoUiState.Success -> {
                            // Ocultar ProgressBar y pasar datos al adaptador
                            adapter.actualizarLista(state.averias)
                        }
                        is ListadoUiState.Error -> {
                            // Mostrar mensaje de error al mecánico
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupFiltros() {
        binding.btnFiltroNuevas.setOnClickListener { viewModel.cargarAverias("nuevas") }
        binding.btnFiltroRecibidas.setOnClickListener { viewModel.cargarAverias("en_curso") }
        binding.btnFiltroFinalizadas.setOnClickListener { viewModel.cargarAverias("historico") }

        binding.btnCerrarSesion.setOnClickListener {
            (requireActivity() as com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity).cerrarSesion()
        }
    }

    private fun abrirDetalle(idAveria: Int) {
        val fragmentDetalle = com.tynsolutions.gestionaveriasmovil.ui.detalle.DetalleAveriaFragment.newInstance(idAveria)
        parentFragmentManager.beginTransaction()
            .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragmentDetalle)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}