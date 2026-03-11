package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding
import com.tynsolutions.gestionaveriasmovil.ui.detalle.estado.CambiarEstadoFragment
import com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion.IntervencionFragment

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

        //Centralizamos todos los eventos de interacción del usuario.
        setupListeners()

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
     * Mapea los eventos de la interfaz (clics) hacia intenciones en la capa lógica o de navegación.
     */
    private fun setupListeners() {
        // CU03: Acción de Aceptar Avería (Lógica delegada al ViewModel)
        binding.btnAceptarAveria.setOnClickListener {
            viewModel.aceptarAveria(averiaId)
        }

        // CU04: Acción de Registrar Intervención (Navegación)
        binding.btnRegistrarIntervencion.setOnClickListener {
            val fragmentIntervencion = IntervencionFragment.newInstance(averiaId)

            parentFragmentManager.beginTransaction()
                .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragmentIntervencion)
                .addToBackStack(null)
                .commit()
        }

        // CU05: Acción de Cambiar Estado de la Maquinaria (Navegación)
        binding.btnCambiarEstado.setOnClickListener {
            // Instanciamos el fragmento destino inyectando el ID de la avería
            val fragmentEstado = CambiarEstadoFragment.newInstance(averiaId)

            // Ejecutamos la transacción para cambiar de pantalla
            parentFragmentManager.beginTransaction()
                .replace(com.tynsolutions.gestionaveriasmovil.R.id.main_container, fragmentEstado)
                .addToBackStack(null)
                .commit()
        }

        // CU06: Acción de Finalizar Avería
        binding.btnFinalizarAveria.setOnClickListener {
            mostrarDialogoFinalizacion()
        }
    }

    /**
     * CU06: Finalizar Avería.
     * Valida instantáneamente el estado de la máquina antes de permitir el cierre de la avería.
     */
    private fun mostrarDialogoFinalizacion() {
        val averiaActual = viewModel.averia.value
        val estadoMaquinaria = averiaActual?.estadoMaquinaria // "Averiada", "Operativa", etc.

        // 1. VALIDACIÓN INSTANTÁNEA
        // Si la máquina sigue "Averiada" o "En mantenimiento", no dejamos finalizar
        if (estadoMaquinaria != "Operativa" && estadoMaquinaria != "Fuera de servicio") {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Atención: Máquina en mal estado")
                .setMessage("No puedes finalizar la avería si la máquina está: $estadoMaquinaria.\n\n" +
                        "Primero debes marcarla como 'Operativa' o 'Fuera de servicio' en la sección anterior.")
                .setPositiveButton("Entendido", null)
                .show()
        }
        else {
            // 2. SI LA VALIDACIÓN PASA: Pedimos confirmación para cerrar la gestión
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Finalizar Gestión")
                .setMessage("¿Confirmas que los trabajos han terminado? La avería se archivará como Finalizada.")
                .setPositiveButton("Sí, finalizar") { _, _ ->
                    viewModel.finalizarAveria(averiaId)
                }
                .setNegativeButton("Cancelar", null)
                .show()
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

        // Renderizado dinámico del historial de intervenciones
        if (averia.intervenciones.isEmpty()) {
            // Si no hay historial, ocultamos la sección
            binding.separadorIntervenciones.visibility = View.GONE
            binding.tvIntervencionesLabel.visibility = View.GONE
            binding.tvIntervencionesLista.visibility = View.GONE
        } else {
            // Si hay datos, los mostramos formateados con viñetas (bullets)
            binding.separadorIntervenciones.visibility = View.VISIBLE
            binding.tvIntervencionesLabel.visibility = View.VISIBLE
            binding.tvIntervencionesLista.visibility = View.VISIBLE

            // Transformamos la lista en un único String unido por saltos de línea
            val historialFormateado = averia.intervenciones.joinToString(separator = "\n\n") { comentario ->
                "• $comentario"
            }
            binding.tvIntervencionesLista.text = historialFormateado


        }

        // Máquina de estados para la visibilidad de componentes
        when (averia.estadoAveriaCalculado) {
            "Nueva" -> {
                // El técnico solo puede aceptar en este estado
                binding.btnAceptarAveria.visibility = View.VISIBLE
                binding.btnRegistrarIntervencion.visibility = View.GONE
                binding.btnFinalizarAveria.visibility = View.GONE
            }
            "Recibida" -> {
                // Una vez aceptada, habilitamos gestión de intervenciones y cierre
                binding.btnAceptarAveria.visibility = View.GONE
                binding.btnRegistrarIntervencion.visibility = View.VISIBLE
                binding.btnCambiarEstado.visibility = View.VISIBLE
                binding.btnFinalizarAveria.visibility = View.VISIBLE
            }
            "Finalizada" -> {
                // Estado terminal: se inhabilitan las acciones de modificación
                binding.layoutAcciones.visibility = View.GONE
            }
        }
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