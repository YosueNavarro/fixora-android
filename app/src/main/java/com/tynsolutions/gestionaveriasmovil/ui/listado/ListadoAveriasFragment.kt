package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentListadoAveriasBinding

class ListadoAveriasFragment : Fragment() {

    private var _binding: FragmentListadoAveriasBinding? = null
    private val binding get() = _binding!!

    // Declaramos nuestro adaptador a nivel de clase
    private lateinit var adapter: AveriasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListadoAveriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Pro-tip: La lógica de la vista siempre va en onViewCreated, no en onCreateView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFiltros()
    }

    private fun setupRecyclerView() {
        // 1. Obtenemos todas las averías de nuestra despensa local
        val listaCompleta = FakeDataSource.averias

        // 2. Inicializamos el adaptador pasándole esa lista
        adapter = AveriasAdapter(listaCompleta)

        // 3. Enchufamos el adaptador a nuestro RecyclerView del XML
        binding.rvAverias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAverias.adapter = adapter
    }

    private fun setupFiltros() {
        // Al pulsar el botón "Nuevas", filtramos la lista y actualizamos el adaptador
        binding.btnFiltroNuevas.setOnClickListener {
            val averiasNuevas = FakeDataSource.averias.filter { it.estado == "Nueva" }
            adapter.actualizarLista(averiasNuevas)
        }

        // Al pulsar "Recibidas", hacemos lo mismo pero buscando ese estado
        binding.btnFiltroRecibidas.setOnClickListener {
            val averiasRecibidas = FakeDataSource.averias.filter { it.estado == "Recibida" }
            adapter.actualizarLista(averiasRecibidas)
        }
        // Configurar botón de Salir
        binding.btnCerrarSesion.setOnClickListener {
            // Como este código está en un Fragment, le decimos a su "jefe" (la Activity) que ejecute la función
            (requireActivity() as com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity).cerrarSesion()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}