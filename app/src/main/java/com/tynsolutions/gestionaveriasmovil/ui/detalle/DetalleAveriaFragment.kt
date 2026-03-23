package com.tynsolutions.gestionaveriasmovil.ui.detalle

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
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion.IntervencionFragment
import kotlinx.coroutines.launch
import android.graphics.Color
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * Controlador de la vista detallada de una avería.
 * Gestiona la presentación de datos y el flujo hacia el registro de intervenciones.
 */
class DetalleAveriaFragment : Fragment() {

    private var _binding: FragmentDetalleAveriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalleViewModel by viewModels {
        val sessionManager = SessionManager(requireContext())
        val apiService = ApiClient.getApiService(sessionManager)
        DetalleViewModel.Factory(AveriasRepository(apiService, sessionManager))
    }

    private var idActual: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleAveriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetalleUiState.Loading -> deshabilitarBotones()
                        is DetalleUiState.Success -> {
                            idActual = state.averia.id
                            restaurarBotones() // Importante: restaurar para permitir más acciones
                            renderizarUI(state.averia)
                        }
                        is DetalleUiState.Error -> {
                            restaurarBotones()
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is DetalleUiState.AccionCompletada -> {
                            // Usamos el manejador de lógica para decidir si cerrar o refrescar
                            handleAccionCompletada(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnAceptarAveria.setOnClickListener {
            if (idActual != -1) viewModel.aceptarAveria(idActual)
        }

        binding.btnFinalizarAveria.setOnClickListener {
            if (idActual != -1) viewModel.finalizarAveria(idActual)
        }

        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        // Navegación exclusiva al registro de intervención
        binding.btnRegistrarIntervencion.setOnClickListener {
            val fragment = IntervencionFragment()
            val bundle = Bundle()

            bundle.putInt("id_averia", idActual)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun renderizarUI(averia: Averia) {

        binding.tvTituloDetalle.text = averia.titulo
        binding.tvMaquinariaDetalle.text = averia.maquinaria
        binding.tvFechaAsignacionDetalle.text = "Asignada el: ${averia.fechaAsignacion ?: "Pendiente"}"
        binding.tvDescripcionDetalle.text = averia.descripcion

        // =========================================================================
        // INTEGRACIÓN DEL ESTADO VISUAL: Sustituimos el text= literal por el método
        // =========================================================================
        aplicarEstiloEstado(binding.tvEstadoAveriaDetalle, averia.estadoAveriaCalculado)

        if (averia.intervenciones.isNotEmpty()) {
            binding.tvIntervencionesLabel.visibility = View.VISIBLE
            binding.tvIntervencionesLista.visibility = View.VISIBLE

            // Unimos los elementos de la lista (aunque solo sea uno con todo el texto)
            binding.tvIntervencionesLista.text = averia.intervenciones.joinToString("\n")
        } else {
            binding.tvIntervencionesLabel.visibility = View.GONE
            binding.tvIntervencionesLista.visibility = View.GONE
        }

        // Lógica de visibilidad purgada de referencias a cambios de estado de maquinaria
        when (averia.estadoAveriaCalculado) {
            "Nueva" -> {
                binding.btnAceptarAveria.visibility = View.VISIBLE
                binding.btnFinalizarAveria.visibility = View.GONE
                binding.btnRegistrarIntervencion.visibility = View.GONE
            }
            "Recibida", "Pendiente" -> {
                binding.btnAceptarAveria.visibility = View.GONE
                binding.btnFinalizarAveria.visibility = View.VISIBLE
                binding.btnRegistrarIntervencion.visibility = View.VISIBLE
            }
            "Finalizada" -> {
                binding.btnAceptarAveria.visibility = View.GONE
                binding.btnFinalizarAveria.visibility = View.GONE
                binding.btnRegistrarIntervencion.visibility = View.GONE
            }
        }
    }

    private fun deshabilitarBotones() {
        binding.btnAceptarAveria.isEnabled = false
        binding.btnFinalizarAveria.isEnabled = false
    }

    private fun restaurarBotones() {
        binding.btnAceptarAveria.isEnabled = true
        binding.btnFinalizarAveria.isEnabled = true
    }

    /**
     * Aplica estilos dinámicos a la etiqueta de estado garantizando una rápida
     * identificación visual por parte del técnico, mejorando la UX.
     * @param tvEstado Referencia al TextView del layout que muestra el estado.
     * @param textoEstado El texto literal del estado ("Nueva", "Recibida", etc.)
     */
    private fun aplicarEstiloEstado(tvEstado: TextView, textoEstado: String) {
        tvEstado.text = textoEstado

        // Usamos colores hexadecimales estándar de Material Design para un acabado profesional
        val colorTexto = when (textoEstado.lowercase()) {
            "nueva" -> Color.parseColor("#03A9F4")      // Azul Claro (Light Blue 500)
            "recibida", "en curso", "pendiente" -> Color.parseColor("#1976D2") // Azul Oscuro (Blue 700)
            "finalizada" -> Color.parseColor("#388E3C") // Verde (Green 700)
            else -> Color.parseColor("#757575")         // Gris por defecto (Grey 600)
        }

        tvEstado.setTextColor(colorTexto)

        // Aplicamos un fondo sutil con el mismo tono que el texto, pero con 15% de opacidad (40 en alpha)
        val colorFondo = ColorUtils.setAlphaComponent(colorTexto, 40)
        tvEstado.setBackgroundColor(colorFondo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Intercepta el regreso al primer plano de la vista para forzar una sincronización automática.
     * Garantiza que si el técnico viene de registrar una intervención, vea el historial actualizado.
     */
    override fun onResume() {
        super.onResume()
        if (idActual != -1) {
            viewModel.recargarDesdeRed(idActual)
        }
    }

    /**
     * Procesa la confirmación de acciones exitosas del servidor.
     * Si la acción no implica el cierre de la pantalla, dispara una recarga de datos inmediata.
     */
    private fun handleAccionCompletada(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // Si el ciclo se ha cerrado, volvemos. Si solo se ha aceptado, refrescamos.
        if (message.contains("finalizado", ignoreCase = true)) {
            parentFragmentManager.popBackStack()
        } else {
            // Esto hará que desaparezca el botón 'Aceptar' y aparezca 'Intervención'
            viewModel.recargarDesdeRed(idActual)
        }
    }
}