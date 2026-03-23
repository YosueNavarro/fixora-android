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
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import kotlinx.coroutines.launch
import android.graphics.Color
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.graphics.toColorInt

/**
 * Fragmento encargado de orquestar el listado principal de averías.
 * Implementa el patrón Observer para reaccionar a los estados del ViewModel
 * y gestiona la navegación hacia el detalle mediante una caché compartida.
 */
class ListadoAveriasFragment : Fragment() {

    private var _binding: FragmentListadoAveriasBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ListadoViewModel
    private lateinit var adapter: AveriasAdapter

    // Memoria del estado de navegación. Sobrevive a la destrucción de la vista
    // mientras el fragmento permanezca en la pila de retroceso (BackStack).
    private var filtroActivo: String = "nuevas"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListadoAveriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de la infraestructura de datos
        val sessionManager = SessionManager(requireContext())
        val apiService = ApiClient.getApiService(sessionManager)
        val repository = AveriasRepository(apiService, sessionManager)

        // Inyección de dependencias en el ViewModel a través de su Factory
        val factory = ListadoViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[ListadoViewModel::class.java]

        setupRecyclerView()
        setupFiltros()
        setupObservers()

        // Restauración del estado de la interfaz
        // Lee la memoria para devolver al usuario al punto donde estaba antes de navegar.
        restaurarEstadoFiltro()
    }

    /**
     * Configura el componente de lista y define el comportamiento del clic.
     */
    private fun setupRecyclerView() {
        adapter = AveriasAdapter { averiaSeleccionada ->
            // Transferencia de estado: Guardamos el objeto en la caché antes de navegar
            AveriaCache.averiaSeleccionada = averiaSeleccionada
            abrirDetalle()
        }
        binding.rvAverias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAverias.adapter = adapter
    }

    /**
     * Suscripción reactiva al flujo de datos del ViewModel.
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ListadoUiState.Loading -> {
                            // TODO: Activar indicador de progreso visual
                        }
                        is ListadoUiState.Success -> {
                            // Actualización atómica de la lista en el adaptador
                            adapter.actualizarLista(state.averias)
                        }
                        is ListadoUiState.Error -> {
                            // Feedback de error mediante notificación emergente
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Define los puntos de entrada de interacción para el filtrado de datos
     * y registra la intención del usuario en la variable de memoria.
     */
    private fun setupFiltros() {
        // Filtros por estado de gestión
        binding.btnFiltroNuevas.setOnClickListener {
            filtroActivo = "nuevas"
            actualizarEstiloBotones(binding.btnFiltroNuevas)
            viewModel.cargarAverias("nuevas")
        }

        binding.btnFiltroRecibidas.setOnClickListener {
            filtroActivo = "en_curso"
            actualizarEstiloBotones(binding.btnFiltroRecibidas)
            viewModel.cargarAverias("en_curso")
        }

        binding.btnFiltroFinalizadas.setOnClickListener {
            filtroActivo = "historico"
            actualizarEstiloBotones(binding.btnFiltroFinalizadas)
            viewModel.cargarAverias("historico")
        }

        // Gestión de salida de la aplicación
        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()        }
    }

    /**
     * Evalúa la memoria del fragmento y sincroniza la capa visual (Botones)
     * con la capa de datos (Petición al ViewModel) para mantener la coherencia al volver atrás.
     */
    private fun restaurarEstadoFiltro() {
        when (filtroActivo) {
            "en_curso" -> {
                actualizarEstiloBotones(binding.btnFiltroRecibidas)
                viewModel.cargarAverias("en_curso")
            }
            "historico" -> {
                actualizarEstiloBotones(binding.btnFiltroFinalizadas)
                viewModel.cargarAverias("historico")
            }
            else -> {
                actualizarEstiloBotones(binding.btnFiltroNuevas)
                viewModel.cargarAverias("nuevas")
            }
        }
    }

    /**
     * Ejecuta la transacción de fragmentos para mostrar el detalle de la avería.
     */
    private fun abrirDetalle() {
        val fragmentDetalle = com.tynsolutions.gestionaveriasmovil.ui.detalle.DetalleAveriaFragment()
        parentFragmentManager.beginTransaction()
            .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragmentDetalle)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Gestor de estado visual para la navegación por pestañas/botones.
     * Utiliza colores parseados directamente para evitar conflictos con temas de Material.
     */
    private fun actualizarEstiloBotones(botonActivo: Button) {
        val listaBotones = listOf(binding.btnFiltroNuevas, binding.btnFiltroRecibidas, binding.btnFiltroFinalizadas)

        listaBotones.forEach { boton ->
            if (boton == botonActivo) {
                // ESTILO ACTIVO (Pulsado)
                boton.setBackgroundColor("#1976D2".toColorInt()) // Azul corporativo
                boton.setTextColor(Color.WHITE)
            } else {
                // ESTILO INACTIVO (Reposo)
                boton.setBackgroundColor("#F2F6FA".toColorInt())
                boton.setTextColor("#1976D2".toColorInt())
            }
        }
    }

    /**
     * Despliega un diálogo modal de confirmación utilizando Material Design.
     * Intercepta la intención de salida para prevenir cierres de sesión accidentales,
     * protegiendo el flujo de trabajo del técnico.
     */
    private fun mostrarDialogoCerrarSesion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas salir de la aplicación? Tendrás que volver a introducir tus credenciales.")
            // Acción secundaria (Seguridad): Cancela la operación y cierra el diálogo
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            // Acción principal (Destructiva): Ejecuta el cierre de sesión real
            .setPositiveButton("Salir") { _, _ ->
                (requireActivity() as? com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity)?.cerrarSesion()
            }
            .setCancelable(false) // Evita que se cierre tocando fuera del cuadro para forzar una decisión
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberación de binding para evitar fugas de memoria (Memory Leaks)
        _binding = null
    }
}