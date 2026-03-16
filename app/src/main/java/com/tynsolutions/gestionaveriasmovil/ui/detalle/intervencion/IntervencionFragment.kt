package com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentIntervencionBinding

/**
 * Controlador de la interfaz gráfica (View) para el registro de intervenciones.
 * Delega toda la lógica de validación de negocio y persistencia al [IntervencionViewModel],
 * cumpliendo con la estructura de la Fase 1 del proyecto[cite: 6].
 */
class IntervencionFragment : Fragment() {

    // Gestión defensiva del ViewBinding para mitigar fugas de memoria (Memory Leaks) en el ciclo de vida del Fragmento.
    private var _binding: FragmentIntervencionBinding? = null
    private val binding get() = _binding!!

    // Inyección de dependencias lazy del ViewModel asociado a este Fragmento.
    private val viewModel: IntervencionViewModel by viewModels()

    // Estado persistido de la transacción (Identificador de enrutamiento).
    private var averiaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Extracción segura del payload de navegación inyectado vía Bundle.
        arguments?.let {
            averiaId = it.getInt("AVERIA_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflado y vinculación de la jerarquía de vistas XML
        _binding = FragmentIntervencionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Despacho de la configuración de manejadores de eventos.
        setupListeners()
    }

    /**
     * Suscribe las interacciones del usuario (clics) a las acciones correspondientes.
     */
    private fun setupListeners() {
        binding.btnVolver.setOnClickListener {
            // Esto saca el fragmento actual de la pila y vuelve a la lista
            parentFragmentManager.popBackStack()
        }

        binding.btnGuardarIntervencion.setOnClickListener {
            // Sanitización de la entrada del usuario (eliminación de espacios residuales)
            val comentario = binding.etDescripcionIntervencion.text.toString().trim()

            // Patrón Early Return: Validación de UI básica antes de invocar la capa lógica
            if (comentario.isNotEmpty()) {

                // Delegación del caso de uso al controlador lógico (ViewModel)
                val exito = viewModel.guardarIntervencion(averiaId, comentario)

                if (exito) {
                    Toast.makeText(requireContext(), "Intervención guardada", Toast.LENGTH_SHORT).show()
                    // Retorno orquestado al componente anterior en el BackStack (Detalle de Avería)
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Error al guardar localmente", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Feedback visual de error de validación
                Toast.makeText(requireContext(), "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        /**
         * Patrón Factory Method.
         * Provee una API robusta y tipada para instanciar este controlador, asegurando
         * que la dependencia requerida (AVERIA_ID) esté presente antes de la inicialización.
         *
         * @param id Identificador primario de la avería.
         * @return Instancia configurada de [IntervencionFragment].
         */
        fun newInstance(id: Int) = IntervencionFragment().apply {
            arguments = Bundle().apply {
                putInt("AVERIA_ID", id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Destrucción explícita de referencias críticas al desmontar la vista.
        _binding = null
    }
}