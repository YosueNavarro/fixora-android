package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentListadoAveriasBinding

class ListadoAveriasFragment : Fragment() {

    // Implementación segura de ViewBinding. Utilizamos una backing property nullable
    // para gestionar el ciclo de vida de la vista de forma independiente al ciclo de vida del Fragmento.
    private var _binding: FragmentListadoAveriasBinding? = null
    private val binding get() = _binding!!

    // Inicialización lazy (perezosa) del ViewModel. El delegado 'viewModels()'
    // asocia la instancia al ciclo de vida del Fragmento de forma automática.
    private val viewModel: ListadoViewModel by viewModels()

    private lateinit var adapter: AveriasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListadoAveriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de los componentes de la UI y suscripción a eventos reactivos.
        setupRecyclerView()
        setupFiltros()
        setupObservers()

        // Disparamos la carga de estado inicial.
        viewModel.cargarAverias()
    }

    /**
     * Configura el RecyclerView: asocia el LayoutManager y establece el Adapter.
     * Implementa la función de callback (lambda) para gestionar la navegación on-click.
     */
    private fun setupRecyclerView() {
        adapter = AveriasAdapter(emptyList()) { idAveriaSeleccionada ->
            abrirDetalle(idAveriaSeleccionada)
        }

        binding.rvAverias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAverias.adapter = adapter
    }

    /**
     * Configura el patrón Observer para reaccionar de forma reactiva a los cambios
     * de estado emitidos por el ViewModel.
     */
    private fun setupObservers() {
        // Utilizamos 'viewLifecycleOwner' para evitar memory leaks si el Fragmento
        // sobrevive a la destrucción de su Vista.
        viewModel.averias.observe(viewLifecycleOwner) { listaNueva ->
            // Inyectamos el nuevo dataset en el Adapter.
            adapter.actualizarLista(listaNueva)
        }
    }

    /**
     * Mapea los eventos de la interfaz (clics) hacia intenciones en la capa lógica.
     * La Vista actúa como entidad pasiva (Passive View) y no ejecuta lógica de negocio.
     */
    private fun setupFiltros() {
        binding.btnFiltroNuevas.setOnClickListener {
            viewModel.filtrarPorEstado("Nueva")
        }

        binding.btnFiltroRecibidas.setOnClickListener {
            viewModel.filtrarPorEstado("Recibida")
        }

        binding.btnFiltroFinalizadas.setOnClickListener {
            viewModel.filtrarPorEstado("Finalizada")
        }

        binding.btnCerrarSesion.setOnClickListener {
            (requireActivity() as com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity).cerrarSesion()
        }
    }

    /**
     * Ejecuta una transacción de fragmentos para navegar a la vista de detalle.
     * * @param idAveria Identificador único de la entidad a mostrar.
     */
    private fun abrirDetalle(idAveria: Int) {
        val fragmentDetalle = com.tynsolutions.gestionaveriasmovil.ui.detalle.DetalleAveriaFragment.newInstance(idAveria)

        // Ejecutamos la transición y añadimos el estado al BackStack del sistema
        // para permitir el retorno natural mediante el botón hardware/gesto "Atrás".
        parentFragmentManager.beginTransaction()
            .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragmentDetalle)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevención crítica de Memory Leaks: Liberamos la referencia a las vistas
        // cuando el layout es destruido por el sistema operativo.
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Al volver del detalle, refrescamos la carga para que los cambios
        // en los estados de las averías se reflejen en los filtros.

        // Aquí puedes elegir qué filtro dejar por defecto al volver.
        // Lo ideal es cargar las "Nuevas" o mantener el último filtro,
        // pero para empezar, cargar todas o las nuevas es lo más seguro:
        viewModel.filtrarPorEstado("Nueva")
    }
}