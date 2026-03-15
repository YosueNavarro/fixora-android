package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentCambiarEstadoBinding

/**
 * Controlador de la interfaz de usuario para el Bloque 7.2 (Cambiar Estado Fragment)[cite: 63, 198].
 * Delega de forma transparente toda la lógica de negocio y persistencia al [CambiarEstadoViewModel].
 */
class CambiarEstadoFragment : Fragment() {

    // Prevención de fugas de memoria (Memory Leaks) mediante manejo seguro del ciclo de vida del ViewBinding
    private var _binding: FragmentCambiarEstadoBinding? = null
    private val binding get() = _binding!!

    // Inyección de dependencias del ViewModel ligado exclusivamente al ciclo de vida de este Fragmento
    private val viewModel: CambiarEstadoViewModel by viewModels()

    // Estado de navegación persistido
    private var averiaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deserialización segura de los argumentos de entrada
        arguments?.let {
            averiaId = it.getInt("AVERIA_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCambiarEstadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Orquestación de la inicialización de la vista
        cargarDatosUI()
        setupListeners()
    }

    /**
     * Solicita los datos de la entidad a la capa lógica para inicializar el estado visual de los componentes.
     */
    private fun cargarDatosUI() {
        // Solicitud de estado delegado al ViewModel (Patrón MVVM estricto)
        val averia = viewModel.obtenerAveria(averiaId)

        averia?.let {
            binding.tvNombreMaquinaActual.text = "Máquina: ${it.maquinaria}"

            // Mapeo del estado del modelo de dominio al estado visual interactivo (RadioButtons)
            when (it.estadoMaquinaria) {
                "Averiada" -> binding.rbAveriada.isChecked = true
                "En mantenimiento" -> binding.rbMantenimiento.isChecked = true
                "Fuera de servicio" -> binding.rbFueraServicio.isChecked = true
                "Operativa" -> binding.rbOperativa.isChecked = true
            }
        }
    }

    /**
     * Registra los manejadores de eventos (Event Handlers) para capturar las intenciones del usuario.
     */
    private fun setupListeners() {
        binding.btnVolver.setOnClickListener {
            // Esto saca el fragmento actual de la pila y vuelve a la lista
            parentFragmentManager.popBackStack()
        }

        binding.btnGuardarEstadoMaquina.setOnClickListener {
            val selectedId = binding.rgEstadoMaquina.checkedRadioButtonId

            // Early Return Pattern: Validación en la capa de vista antes de invocar lógica de negocio
            if (selectedId != -1) {
                val radioButton = binding.root.findViewById<RadioButton>(selectedId)
                val nuevoEstado = radioButton.text.toString()

                // Delegación de la mutación de estado al controlador lógico
                val exito = viewModel.actualizarEstadoMaquinaria(averiaId, nuevoEstado)

                if (exito) {
                    Toast.makeText(requireContext(), "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                    // Retorno enrutado al flujo principal (Detalle de Avería) tras confirmar el éxito
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Error técnico al actualizar el estado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Operación denegada: Seleccione un estado válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        /**
         * Factory Method Pattern.
         * Garantiza una instanciación segura proporcionando una API tipada para inyectar
         * dependencias (AVERIA_ID) en el Bundle del fragmento.
         */
        fun newInstance(id: Int) = CambiarEstadoFragment().apply {
            arguments = Bundle().apply {
                putInt("AVERIA_ID", id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpieza de referencias críticas en el desmontaje de la vista para evitar memory leaks
        _binding = null
    }
}