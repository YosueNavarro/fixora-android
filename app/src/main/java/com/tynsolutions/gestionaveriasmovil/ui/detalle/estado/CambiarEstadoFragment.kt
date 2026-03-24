package com.tynsolutions.gestionaveriasmovil.ui.detalle.estado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.data.local.AveriaCache
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentCambiarEstadoBinding
import kotlinx.coroutines.launch

/**
 * Controlador de la interfaz de usuario para la mutación del estatus operativo del hardware.
 * Implementa un flujo reactivo basado en el estado del ViewModel (UI State) y garantiza
 * la integridad de los datos mediante el uso de una caché compartida segura.
 */
class CambiarEstadoFragment : Fragment() {

    private var _binding: FragmentCambiarEstadoBinding? = null
    private val binding get() = _binding!!

    /**
     * Inyección de dependencias mediante Factory.
     * Centraliza la instanciación de la infraestructura de red y datos en el ámbito del Fragment.
     */
    private val viewModel: CambiarEstadoViewModel by viewModels {
        val session = SessionManager(requireContext())
        val repository = AveriasRepository(ApiClient.getApiService(session), session)
        CambiarEstadoViewModel.Factory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCambiarEstadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sincronizarContextoUI()
        configurarObservadoresReactivos()
        establecerInteraccionesUsuario()
    }

    /**
     * Sincroniza la vista con el contexto de la máquina seleccionada.
     * Implementa una validación de integridad para prevenir estados nulos en la navegación.
     */
    private fun sincronizarContextoUI() {
        val averia = AveriaCache.averiaSeleccionada
        if (averia != null) {
            // Uso de recursos formateados para cumplimiento de i18n/accesibilidad
            binding.tvNombreMaquinaActual.text = getString(R.string.formato_nombre_maquina, averia.maquinaria)
        } else {
            // Recuperación ante fallos de integridad de memoria
            Toast.makeText(requireContext(), "Fallo de integridad: Datos de sesión perdidos.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Suscripción al flujo de estado del ViewModel.
     * Utiliza repeatOnLifecycle para optimizar el consumo de recursos y asegurar
     * que la recolección de datos solo ocurra cuando la UI es visible.
     */
    private fun configurarObservadoresReactivos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    manejarCambioDeEstado(estado)
                }
            }
        }
    }

    /**
     * Orquestador visual de estados de red.
     * Garantiza la retroalimentación inmediata al técnico y la protección de transacciones.
     */
    private fun manejarCambioDeEstado(estado: CambiarEstadoUiState) {
        when (estado) {
            is CambiarEstadoUiState.Loading -> {
                // Prevención de Race Conditions: Bloqueo de entrada durante la transacción
                binding.btnGuardarEstadoMaquina.isEnabled = false
            }
            is CambiarEstadoUiState.Success -> {
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            is CambiarEstadoUiState.Error -> {
                binding.btnGuardarEstadoMaquina.isEnabled = true
                Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                viewModel.resetState() // Limpieza del buffer de error tras notificación
            }
            else -> Unit
        }
    }

    /**
     * Define los puntos de entrada de interacción física.
     */
    private fun establecerInteraccionesUsuario() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnGuardarEstadoMaquina.setOnClickListener {
            ejecutarAccionGuardar()
        }
    }

    /**
     * Captura la intención del técnico y la traslada a la capa de negocio.
     */
    private fun ejecutarAccionGuardar() {
        val selectedId = binding.rgEstadoMaquina.checkedRadioButtonId

        if (selectedId != -1) {
            val radioButton = binding.root.findViewById<RadioButton>(selectedId)
            val etiquetaEstado = radioButton.text.toString()

            // Extracción segura de ID de maquinaria desde el Singleton de Caché
            val idMaquina = AveriaCache.averiaSeleccionada?.id ?: return
            viewModel.actualizarEstadoMaquinaria(idMaquina, etiquetaEstado)
        } else {
            Toast.makeText(requireContext(), "Debe seleccionar un estatus operativo válido.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevención de fugas de memoria mediante la anulación del binding
        _binding = null
    }
}