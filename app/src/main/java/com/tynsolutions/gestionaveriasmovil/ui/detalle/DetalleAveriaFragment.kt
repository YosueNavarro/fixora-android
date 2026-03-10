package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding

class DetalleAveriaFragment : Fragment() {

    // Gestión del ciclo de vida del ViewBinding (prevención de Memory Leaks).
    private var _binding: FragmentDetalleAveriaBinding? = null
    private val binding get() = _binding!!

    // Inyección del ViewModel ligado al ciclo de vida de este Fragmento.
    private val viewModel: DetalleViewModel by viewModels()

    // Argumento de navegación persistido.
    private var averiaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deserialización de los argumentos inyectados vía Factory Method.
        arguments?.let {
            averiaId = it.getInt("AVERIA_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleAveriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Suscripción al estado del ViewModel antes de solicitar los datos.
        setupObservers()

        // Despachamos el evento de inicialización hacia la capa lógica.
        if (averiaId != -1) {
            viewModel.cargarAveria(averiaId)
        }
    }

    /**
     * Configura el binding reactivo entre el estado emitido por el ViewModel y la UI.
     */
    private fun setupObservers() {
        // El Observer reacciona automáticamente cuando el ViewModel encuentra la avería.
        viewModel.averia.observe(viewLifecycleOwner) { averia ->
            renderizarUI(averia)
        }
    }

    /**
     * Función pura de renderizado. Mapea las propiedades del modelo de dominio
     * a los componentes visuales del layout.
     */
    private fun renderizarUI(averia: com.tynsolutions.gestionaveriasmovil.domain.model.Averia) {
        binding.tvTituloDetalle.text = averia.titulo

        // Formateo de presentación visual: Entidad + Estado de la máquina.
        binding.tvMaquinariaDetalle.text = "${averia.maquinaria} - [${averia.estadoMaquinaria}]"

        // Mapeo de propiedad calculada.
        binding.tvEstadoDetalle.text = "Estado avería: ${averia.estadoAveriaCalculado}"

        // Lógica de presentación de fechas basada en nullabilidad.
        val fechaMostrar = averia.fechaAsignacion ?: averia.fechaInforme
        binding.tvFechaDetalle.text = "Fecha: $fechaMostrar"

        binding.tvDescripcionDetalle.text = averia.descripcion
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberación explícita de referencias UI.
        _binding = null
    }

    // Factory Method Pattern: Provee una API limpia para la instanciación de este Fragmento
    // garantizando la inyección segura de sus dependencias (argumentos).
    companion object {
        fun newInstance(idAveria: Int) = DetalleAveriaFragment().apply {
            arguments = Bundle().apply {
                putInt("AVERIA_ID", idAveria)
            }
        }
    }
}