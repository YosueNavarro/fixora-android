package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AveriasRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion.IntervencionFragment
import kotlinx.coroutines.launch

/**
 * Controlador de vista para el detalle exhaustivo de incidencias.
 * Implementa el patrón Observer para reaccionar a los cambios de estado del ViewModel
 * y gestiona la máquina de estados visual para la botonera de acción técnica.
 */
class DetalleAveriaFragment : Fragment() {

    private var _binding: FragmentDetalleAveriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalleViewModel by viewModels {
        val session = SessionManager(requireContext())
        val repository = AveriasRepository(ApiClient.getApiService(session), session)
        DetalleViewModel.Factory(repository)
    }

    // Retención del estado actual para validaciones de negocio en tiempo de ejecución
    private var idAveriaActual: Int = -1
    private var averiaEnPantalla: Averia? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleAveriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarObservadores()
        configurarManejadoresEventos()
    }

    /**
     * Suscripción reactiva al flujo de estados de la UI.
     * Garantiza la coherencia visual entre los datos en caché y las actualizaciones remotas.
     */
    private fun configurarObservadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    when (estado) {
                        is DetalleUiState.Loading -> alternarInteractividad(false)
                        is DetalleUiState.Success -> {
                            idAveriaActual = estado.averia.id
                            averiaEnPantalla = estado.averia // Sincronizamos la variable local
                            alternarInteractividad(true)
                            poblarComponentesUI(estado.averia)
                        }
                        is DetalleUiState.Error -> {
                            alternarInteractividad(true)
                            Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                        }
                        is DetalleUiState.AccionCompletada -> procesarEventoFinalizacion(estado.message)
                    }
                }
            }
        }
    }

    /**
     * Establece los listeners para la interacción del técnico.
     */
    private fun configurarManejadoresEventos() {
        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnAceptarAveria.setOnClickListener {
            if (idAveriaActual != -1) viewModel.aceptarAveria(idAveriaActual)
        }

        // Interceptamos el click directo para aplicar las reglas de negocio en cascada
        binding.btnFinalizarAveria.setOnClickListener {
            procesarPeticionFinalizacion()
        }

        binding.btnRegistrarIntervencion.setOnClickListener {
            navegarARegistroIntervencion()
        }

        binding.btnCambiarEstadoMaquina.setOnClickListener {
            navegarACambiarEstadoMaquina()
        }
    }

    // =========================================================================
    // REGLAS DE NEGOCIO Y SEGURIDAD (Validación de Cierre)
    // =========================================================================

    /**
     * Evalúa las precondiciones de negocio antes de autorizar el cierre de la incidencia.
     * Implementa programación defensiva para evitar peticiones inválidas al servidor.
     */
    private fun procesarPeticionFinalizacion() {
        val averia = averiaEnPantalla ?: return

        // 1. Validación Estricta: Comprobamos si el técnico ha documentado el proceso
        if (averia.intervenciones.isEmpty()) {
            // Bloqueo de seguridad: No hay proceso descrito
            notificarFaltaDeIntervencion()
            return
        }

        // 2. Validación de Hardware: Comprobamos el estatus de la máquina (Rango 800)
        // Solo se permite finalizar si la máquina está Operativa (801) o Fuera de servicio (804)
        val estadoMaquina = averia.maquinaria.codigoEstado
        if (estadoMaquina != 801 && estadoMaquina != 804) {
            notificarFaltaCambioEstadoMaquina()
            return
        }

        // 3. Si las precondiciones se cumplen, solicitamos confirmación explícita
        mostrarDialogoConfirmacionFinalizar()
    }

    /**
     * Proporciona feedback visual inmediato (Snackbar rojo) si falta el parte de trabajo.
     */
    private fun notificarFaltaDeIntervencion() {
        Snackbar.make(
            binding.root,
            "Operación denegada: Debes registrar al menos una intervención documentando el trabajo.",
            Snackbar.LENGTH_LONG
        ).apply {
            setBackgroundTint(Color.parseColor("#D32F2F")) // Rojo alerta
            setTextColor(Color.WHITE)
            show()
        }
    }

    /**
     * Proporciona feedback visual inmediato (Snackbar naranja) si falta el estado de la máquina.
     */
    private fun notificarFaltaCambioEstadoMaquina() {
        Snackbar.make(
            binding.root,
            "Operación denegada: Debes cambiar el estado de la máquina a 'Operativa' o 'Fuera de servicio' antes de finalizar.",
            Snackbar.LENGTH_LONG
        ).apply {
            setBackgroundTint(Color.parseColor("#FF9800")) // Naranja advertencia
            setTextColor(Color.WHITE)
            show()
        }
    }

    /**
     * Despliega un modal de confirmación crítica (Punto de No Retorno).
     * Previene mutaciones de estado accidentales en la base de datos.
     */
    private fun mostrarDialogoConfirmacionFinalizar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Finalizar Avería")
            .setMessage("¿Estás seguro de que deseas marcar esta avería como finalizada?\n\nEsta acción registrará la hora actual como cierre definitivo y no podrá deshacerse.")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss() // Cierre seguro sin mutación
            }
            .setPositiveButton("Sí, finalizar") { _, _ ->
                if (idAveriaActual != -1) viewModel.finalizarAveria(idAveriaActual)
            }
            .setCancelable(false) // Obliga al usuario a tomar una decisión explícita
            .show()
    }

    // =========================================================================
    // RENDERIZADO Y NAVEGACIÓN
    // =========================================================================

    /**
     * Renderiza los datos de la entidad en los componentes de la vista.
     * Implementa lógica de visibilidad condicional basada en el estado administrativo.
     */
    private fun poblarComponentesUI(averia: Averia) {
        with(binding) {
            tvTituloDetalle.text = averia.titulo

            val textoEstado = obtenerDescripcionEstadoMaquina(averia.maquinaria.codigoEstado)
            tvMaquinariaDetalle.text = "${averia.maquinaria.nombre} [$textoEstado]"

            tvFechaAsignacionDetalle.text = getString(R.string.formato_fecha_asignacion, averia.fechaAsignacion ?: "Pendiente")
            tvDescripcionDetalle.text = averia.descripcion

            aplicarEstiloEstado(tvEstadoAveriaDetalle, averia.estadoAveriaCalculado)

            val tieneIntervenciones = averia.intervenciones.isNotEmpty()
            tvIntervencionesLabel.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE
            tvIntervencionesLista.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE

            if (tieneIntervenciones) {
                tvIntervencionesLista.text = averia.intervenciones.joinToString("\n")
            }

            gestionarVisibilidadAcciones(averia.estadoAveriaCalculado)
        }
    }

    /**
     * Controla la disponibilidad de botones según la fase del ciclo de vida de la avería.
     */
    private fun gestionarVisibilidadAcciones(estado: String) {
        with(binding) {
            when (estado) {
                "Nueva" -> {
                    btnAceptarAveria.visibility = View.VISIBLE
                    btnFinalizarAveria.visibility = View.GONE
                    btnRegistrarIntervencion.visibility = View.GONE
                    btnCambiarEstadoMaquina.visibility = View.GONE
                }
                "Recibida", "Pendiente", "En curso" -> {
                    btnAceptarAveria.visibility = View.GONE
                    btnFinalizarAveria.visibility = View.VISIBLE
                    btnRegistrarIntervencion.visibility = View.VISIBLE
                    btnCambiarEstadoMaquina.visibility = View.VISIBLE
                }
                "Finalizada" -> {
                    btnAceptarAveria.visibility = View.GONE
                    btnFinalizarAveria.visibility = View.GONE
                    btnRegistrarIntervencion.visibility = View.GONE
                    btnCambiarEstadoMaquina.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Aplica semántica de colores a la etiqueta de estado para facilitar la lectura rápida (UX).
     */
    private fun aplicarEstiloEstado(tvEstado: TextView, textoEstado: String) {
        tvEstado.text = textoEstado
        val colorRef = when (textoEstado.lowercase()) {
            "nueva" -> "#03A9F4"
            "recibida", "en curso", "pendiente" -> "#1976D2"
            "finalizada" -> "#388E3C"
            else -> "#757575"
        }

        val colorInt = Color.parseColor(colorRef)
        tvEstado.setTextColor(colorInt)
        tvEstado.setBackgroundColor(ColorUtils.setAlphaComponent(colorInt, 40))
    }

    /**
     * Orquesta la transición hacia el flujo de registro de actividad técnica.
     */
    private fun navegarARegistroIntervencion() {
        val fragment = IntervencionFragment().apply {
            arguments = Bundle().apply { putInt("id_averia", idAveriaActual) }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Orquesta la transición hacia el flujo de mutación de estado del hardware.
     */
    private fun navegarACambiarEstadoMaquina() {
        val fragment = com.tynsolutions.gestionaveriasmovil.ui.detalle.estado.CambiarEstadoFragment()

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun alternarInteractividad(activado: Boolean) {
        binding.btnAceptarAveria.isEnabled = activado
        binding.btnFinalizarAveria.isEnabled = activado
        binding.btnCambiarEstadoMaquina.isEnabled = activado
    }

    private fun procesarEventoFinalizacion(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        if (mensaje.contains("finalizado", ignoreCase = true)) {
            parentFragmentManager.popBackStack()
        } else {
            viewModel.sincronizarConServidor(idAveriaActual)
        }
    }

    /**
     * Diccionario inverso: Transforma el código de catálogo numérico en texto legible para la UI.
     */
    private fun obtenerDescripcionEstadoMaquina(codigo: Int): String = when (codigo) {
        801 -> "Operativa"
        802 -> "Averiada"
        803 -> "En mantenimiento"
        804 -> "Fuera de servicio"
        else -> "Estado desconocido"
    }

    override fun onResume() {
        super.onResume()
        if (idAveriaActual != -1) viewModel.sincronizarConServidor(idAveriaActual)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}