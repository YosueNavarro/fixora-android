package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentIntervencionBinding

/**
 * Controlador de la interfaz gráfica para el caso de uso CU04: Registrar Intervención.
 * Implementa un formulario para la entrada de datos del técnico.
 */
class IntervencionFragment : Fragment() {

    // Gestión segura del ciclo de vida del ViewBinding para evitar fugas de memoria (Memory Leaks).
    private var _binding: FragmentIntervencionBinding? = null
    private val binding get() = _binding!!

    // Estado persistido de la entidad a la que pertenece esta intervención.
    private var averiaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deserialización de los argumentos inyectados en la creación del fragmento.
        arguments?.let {
            averiaId = it.getInt("AVERIA_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflado reactivo de la jerarquía de vistas
        _binding = FragmentIntervencionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de mapeadores de eventos (Event Handlers)
        setupListeners()
    }

    /**
     * Mapea los eventos de la UI hacia la lógica de validación y persistencia.
     */
    private fun setupListeners() {
        binding.btnGuardarIntervencion.setOnClickListener {
            // Extracción y sanitización de la entrada del usuario (eliminación de espacios en blanco)
            val comentario = binding.etDescripcionIntervencion.text.toString().trim()

            // Validación de capa de vista (Early Return Pattern)
            if (comentario.isNotEmpty()) {
                guardarIntervencionLocal(comentario)
            } else {
                Toast.makeText(requireContext(), "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Implementa la lógica transicional de la Fase 1: agregar a la lista local.
     * * Nota Arquitectónica: En la Fase 2, esta lógica se delegará a un ViewModel
     * que realizará un POST /averias/{id}/intervenciones hacia el backend.
     * * @param texto Descripción técnica de la intervención realizada.
     */
    private fun guardarIntervencionLocal(texto: String) {
        // 1. Recuperación de la referencia en memoria (Mock Data Source)
        val averia = FakeDataSource.averias.find { it.id == averiaId }

        averia?.let {
            // 2. Mutación del estado local de la entidad
            it.intervenciones.add(texto)

            // 3. Feedback no bloqueante para el usuario
            Toast.makeText(requireContext(), "Intervención guardada localmente", Toast.LENGTH_SHORT).show()

            // 4. Gestión del enrutamiento: Extracción del fragmento actual del BackStack
            // para retornar de forma natural a la pantalla de Detalles de la Avería.
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        /**
         * Factory Method Pattern.
         * Provee una API limpia y tipada para instanciar este Fragmento, garantizando
         * que la dependencia requerida (AVERIA_ID) se inyecta correctamente en el Bundle.
         * * @param id Identificador primario de la avería.
         */
        fun newInstance(id: Int) = IntervencionFragment().apply {
            arguments = Bundle().apply {
                putInt("AVERIA_ID", id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevención crítica de Memory Leaks: destrucción de la referencia a las vistas
        // cuando el ciclo de vida del layout finaliza.
        _binding = null
    }
}