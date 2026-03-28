package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentListadoAveriasBinding
import com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Controlador principal del catálogo de incidencias técnicas.
 * Implementa una arquitectura reactiva para el filtrado dinámico y la gestión
 * de la persistencia del estado de navegación (Backstack Retention).
 */
class ListadoAveriasFragment : Fragment() {

    private var _binding: FragmentListadoAveriasBinding? = null
    private val binding get() = _binding!!

    // Inyección de dependencias delegada. Mantiene la integridad del ciclo de vida.
    private val viewModel: ListadoViewModel by viewModels {
        val session = SessionManager(requireContext())
        val repository = AveriasRepository(ApiClient.getApiService(session), session)
        ListadoViewModel.Factory(repository)
    }

    private lateinit var averiasAdapter: AveriasAdapter

    // Persistencia volátil del estado de filtrado para restaurar la UI tras navegación
    private var filtroActivo: String = "nuevas"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListadoAveriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarComponentesLista()
        vincularFlujoDeDatos()
        configurarFiltrosYControles()
        configurarRefrescoManual()
        restaurarContextoNavegacion()
        configurarBuzonNavegacion() // <- Activamos la escucha de redirecciones dinámicas
    }

    /**
     * Intercepta directivas de navegación (Intents) emitidas por Fragmentos hijos.
     * Actúa como receptor del Fragment Result API para reubicar al usuario en la
     * pestaña correcta tras una mutación de estado en el Detalle.
     */
    private fun configurarBuzonNavegacion() {
        parentFragmentManager.setFragmentResultListener("request_cambio_seccion", viewLifecycleOwner) { _, bundle ->
            val destino = bundle.getString("destino")

            // Enrutamiento dinámico simulando la interacción del usuario
            when (destino) {
                "nuevas" -> ejecutarFiltrado("nuevas", binding.btnFiltroNuevas)
                "recibidas" -> ejecutarFiltrado("en_curso", binding.btnFiltroRecibidas)
                "finalizadas" -> ejecutarFiltrado("historico", binding.btnFiltroFinalizadas)
            }
        }
    }

    /**
     * Configura el adaptador y la estrategia de Layout.
     * Implementa la transferencia de estado hacia la caché antes de la transición de fragmentos.
     */
    private fun inicializarComponentesLista() {
        averiasAdapter = AveriasAdapter { seleccion ->
            AveriaCache.averiaSeleccionada = seleccion
            navegarADetalle()
        }

        binding.rvAverias.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = averiasAdapter
        }
    }

    /**
     * Suscripción reactiva al estado de la interfaz (UI State).
     * Utiliza repeatOnLifecycle para garantizar que la recolección de corrutinas
     * sea segura y eficiente en términos de recursos (CPU/Batería).
     */
    private fun vincularFlujoDeDatos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    when (estado) {
                        is ListadoUiState.Loading -> alternarProgreso(true)
                        is ListadoUiState.Success -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            alternarProgreso(false)
                            averiasAdapter.actualizarLista(estado.averias)
                        }
                        is ListadoUiState.Error -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            alternarProgreso(false)
                            notificarError(estado.message)
                        }
                    }
                }
            }
        }
    }

    /**
     * Define los puntos de interacción para la segmentación de datos.
     */
    private fun configurarFiltrosYControles() {
        with(binding) {
            btnFiltroNuevas.setOnClickListener { ejecutarFiltrado("nuevas", btnFiltroNuevas) }
            btnFiltroRecibidas.setOnClickListener { ejecutarFiltrado("en_curso", btnFiltroRecibidas) }
            btnFiltroFinalizadas.setOnClickListener { ejecutarFiltrado("historico", btnFiltroFinalizadas) }

            btnCerrarSesion.setOnClickListener { solicitarConfirmacionSalida() }
        }
    }

    /**
     * Orquesta el cambio de estado de filtrado y la actualización de la capa de datos.
     */
    private fun ejecutarFiltrado(tipo: String, botonTrigger: Button) {
        filtroActivo = tipo
        actualizarEstéticaNavegación(botonTrigger)
        viewModel.cargarAverias(tipo)
    }

    /**
     * Sincroniza la visualización de la pestaña activa basándose en la última
     * interacción registrada del usuario.
     */
    private fun restaurarContextoNavegacion() {
        val botonARestaurar = when (filtroActivo) {
            "en_curso" -> binding.btnFiltroRecibidas
            "historico" -> binding.btnFiltroFinalizadas
            else -> binding.btnFiltroNuevas
        }
        ejecutarFiltrado(filtroActivo, botonARestaurar)
    }

    /**
     * Realiza la transición de fragmentos hacia la vista de detalle.
     */
    private fun navegarADetalle() {
        val detalleFragment = com.tynsolutions.gestionaveriasmovil.ui.detalle.DetalleAveriaFragment()
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.main_container, detalleFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Actualiza el feedback visual de la botonera de filtrado (Tabs).
     */
    private fun actualizarEstéticaNavegación(botonActivo: Button) {
        val botones = listOf(binding.btnFiltroNuevas, binding.btnFiltroRecibidas, binding.btnFiltroFinalizadas)

        botones.forEach { boton ->
            val esActivo = (boton == botonActivo)
            // Estilos definidos dinámicamente para garantizar consistencia visual
            boton.setBackgroundColor(if (esActivo) "#1976D2".toColorInt() else "#F2F6FA".toColorInt())
            boton.setTextColor(if (esActivo) Color.WHITE else "#1976D2".toColorInt())
        }
    }

    /**
     * Despliega un diálogo de confirmación (Material Modal) para prevenir cierres de sesión accidentales.
     */
    private fun solicitarConfirmacionSalida() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout_title))
            .setMessage(getString(R.string.logout_message))
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.salir)) { _, _ ->
                (requireActivity() as? MainActivity)?.cerrarSesion()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Configura el componente SwipeRefreshLayout para la recarga manual de datos.
     * Implementa feedback visual (Spinning loader) personalizado con los colores corporativos.
     */
    private fun configurarRefrescoManual() {
        // 1. Personalización de UI (Aplicamos el color azul de la empresa)
        binding.swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#3A75B5"))

        // 2. Suscripción al evento de deslizamiento
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Cuando el usuario tira hacia abajo, forzamos una recarga con el filtro actual
            viewModel.cargarAverias(filtroActivo)
        }
    }

    private fun alternarProgreso(visible: Boolean) {
        // Implementación futura para ProgressBar
    }

    private fun notificarError(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}