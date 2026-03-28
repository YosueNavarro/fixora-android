package com.tynsolutions.gestionaveriasmovil.ui.detalle

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.tynsolutions.gestionaveriasmovil.data.repository.MaquinariaRepository
import com.tynsolutions.gestionaveriasmovil.databinding.FragmentDetalleAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.ui.detalle.intervencion.IntervencionFragment
import kotlinx.coroutines.launch

/**
 * [DetalleAveriaFragment]
 * Controlador de UI especializado en la gestión del ciclo de vida de una avería.
 * * ARQUITECTURA:
 * - Implementa un patrón reactivo basado en StateFlow.
 * - Sigue el principio de "Single Source of Truth" (SSoT) delegando el estado al ViewModel.
 * - Utiliza "Optimistic UI Updates" para mejorar la percepción de rendimiento del operario.
 */
class DetalleAveriaFragment : Fragment() {

    // Gestión de ViewBinding con nulabilidad para prevenir Memory Leaks en el Backstack.
    private var _binding: FragmentDetalleAveriaBinding? = null
    private val binding get() = _binding!!

    /**
     * Inyección de dependencias vía Factory.
     * Garantiza que los repositorios mantengan el scope correcto durante la vida del ViewModel.
     */
    private val viewModel: DetalleViewModel by viewModels {
        val session = SessionManager(requireContext())
        val api = ApiClient.getApiService(session)
        DetalleViewModel.Factory(
            AveriasRepository(api, session),
            MaquinariaRepository(api)
        )
    }

    // Estado local efímero para validaciones rápidas de UI sin suscripción de flujo.
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
     * Pipeline de observación reactiva.
     * El uso de [repeatOnLifecycle] con el estado [STARTED] es crítico:
     * 1. Pausa el consumo de recursos cuando la app está en segundo plano.
     * 2. Evita colisiones de fragmentos al intentar modificar la UI en estados no seguros.
     */
    private fun configurarObservadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    when (estado) {
                        is DetalleUiState.Loading -> alternarInteractividad(false)
                        is DetalleUiState.Success -> {
                            idAveriaActual = estado.averia.id
                            averiaEnPantalla = estado.averia
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
     * Registro de listeners y callbacks de sistema.
     * Incluye la intercepción del OnBackPressedDispatcher para garantizar la integridad
     * del flujo de navegación entre el detalle y el listado segmentado.
     */
    private fun configurarManejadoresEventos() {
        // Intercepción del hardware/gestos: Sincroniza el botón físico con la lógica de negocio.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                retornarAlListadoActualizado()
            }
        })

        binding.btnVolver.setOnClickListener {
            retornarAlListadoActualizado()
        }

        binding.btnAceptarAveria.setOnClickListener {
            val maqId = averiaEnPantalla?.maquinaria?.id ?: return@setOnClickListener
            if (idAveriaActual != -1) {
                viewModel.aceptarAveriaYCambiarMaquina(idAveriaActual, maqId)
            }
        }

        binding.btnFinalizarAveria.setOnClickListener {
            procesarPeticionFinalizacion()
        }

        binding.btnRegistrarIntervencion.setOnClickListener {
            navegarARegistroIntervencion()
        }
    }

    // =========================================================================
    // REGLAS DE NEGOCIO Y FLUJO GUIADO (WIZARD DE CIERRE)
    // =========================================================================

    /**
     * Implementa la validación de integridad documental previo al cierre.
     * Bloquea el flujo si no se detectan evidencias técnicas (intervenciones).
     */
    private fun procesarPeticionFinalizacion() {
        val averia = averiaEnPantalla ?: return

        if (averia.intervenciones.isEmpty()) {
            notificarFaltaDeIntervencion()
            return
        }

        mostrarDialogoSeleccionEstado()
    }

    /**
     * Paso 1 del Wizard: Selección de estado post-mantenimiento.
     * Usa un sistema de "Cross-Checking" manual en los Checkboxes para simular
     * comportamiento de RadioButton, manteniendo el requisito visual de diseño.
     */
    private fun mostrarDialogoSeleccionEstado() {
        val opcionesMenu = arrayOf("Activa (Operativa)", "Fuera de servicio")
        val codigosBD = intArrayOf(801, 804)
        val estadosCheck = booleanArrayOf(false, false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Introduce el estado en el que ha quedado la máquina tras el mantenimiento:")
            .setMultiChoiceItems(opcionesMenu, estadosCheck) { dialogInterface, which, isChecked ->
                if (isChecked) {
                    val otroIndice = if (which == 0) 1 else 0
                    estadosCheck[otroIndice] = false
                    (dialogInterface as androidx.appcompat.app.AlertDialog).listView.setItemChecked(otroIndice, false)
                }
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Siguiente", null)
            .setCancelable(false)
            .create()

        dialog.show()

        // Intercepción del botón positivo para forzar validación de selección obligatoria.
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val indiceSeleccionado = estadosCheck.indexOfFirst { it }

            if (indiceSeleccionado != -1) {
                dialog.dismiss()
                mostrarDialogoConfirmacionFinalizar(codigosBD[indiceSeleccionado], opcionesMenu[indiceSeleccionado])
            } else {
                Toast.makeText(requireContext(), "Por favor, marca uno de los dos estados operativos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Paso 2 del Wizard: Confirmación de mutación irreversible.
     * Actúa como "Safe-guard" final antes de impactar el servidor.
     */
    private fun mostrarDialogoConfirmacionFinalizar(codigoEstadoFinal: Int, nombreEstado: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Cierre")
            .setMessage("La avería se marcará como Finalizada y la máquina pasará a estado:\n\n👉 **$nombreEstado**\n\nEsta acción es irreversible. ¿Deseas continuar?")
            .setNegativeButton("Atrás", null)
            .setPositiveButton("Sí, finalizar") { _, _ ->
                val maqId = averiaEnPantalla?.maquinaria?.id ?: return@setPositiveButton
                if (idAveriaActual != -1) {
                    viewModel.finalizarAveriaYCambiarMaquina(idAveriaActual, maqId, codigoEstadoFinal)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun notificarFaltaDeIntervencion() {
        val snackbar = Snackbar.make(
            binding.root,
            "Operación denegada: Debes registrar al menos una intervención documentando el trabajo.",
            Snackbar.LENGTH_INDEFINITE
        )

        snackbar.apply {
            setBackgroundTint(Color.parseColor("#D32F2F"))
            setTextColor(Color.WHITE)
            setActionTextColor(Color.WHITE)
            setAction("OK") { dismiss() }
            view.setOnClickListener { dismiss() }
            show()
        }
    }

    // =========================================================================
    // NAVEGACIÓN INTELIGENTE CON CONTEXTO
    // =========================================================================

    /**
     * [Fragment Result API]
     * Inyecta en el bus de comunicación del FragmentManager el destino calculado.
     * Evita inconsistencias visuales donde una avería aceptada sigue apareciendo en "Nuevas".
     */
    private fun retornarAlListadoActualizado() {
        val estadoActual = averiaEnPantalla?.estadoAveriaCalculado?.lowercase() ?: "nueva"

        val destinoListado = when (estadoActual) {
            "nueva" -> "nuevas"
            "finalizada" -> "finalizadas"
            else -> "recibidas"
        }

        parentFragmentManager.setFragmentResult(
            "request_cambio_seccion",
            Bundle().apply { putString("destino", destinoListado) }
        )

        parentFragmentManager.popBackStack()
    }

    // =========================================================================
    // RENDERIZADO Y UI
    // =========================================================================

    /**
     * Traduce el modelo de dominio a representación visual.
     * Implementa lógica de visibilidad condicional para hitos temporales.
     */
    private fun poblarComponentesUI(averia: Averia) {
        with(binding) {
            tvTituloDetalle.text = averia.titulo

            val textoEstado = obtenerDescripcionEstadoMaquina(averia.maquinaria.codigoEstado)
            tvMaquinariaDetalle.text = "${averia.maquinaria.nombre} [$textoEstado]"

            tvFechaAsignacionDetalle.text = getString(R.string.formato_fecha_asignacion, averia.fechaAsignacion ?: "Pendiente")

            // Gestión de visibilidad dinámica basada en nulabilidad de hitos.
            tvFechaAceptacionDetalle.visibility = if (averia.fechaAceptacion != null) View.VISIBLE else View.GONE
            averia.fechaAceptacion?.let { tvFechaAceptacionDetalle.text = "Aceptada el: $it" }

            tvFechaFinalizacionDetalle.visibility = if (averia.fechaFinalizacion != null) View.VISIBLE else View.GONE
            averia.fechaFinalizacion?.let { tvFechaFinalizacionDetalle.text = "Finalizada el: $it" }

            tvDescripcionDetalle.text = averia.descripcion
            aplicarEstiloEstado(tvEstadoAveriaDetalle, averia.estadoAveriaCalculado)

            // Renderizado de listas anidadas con separadores visuales (bullets).
            val tieneIntervenciones = averia.intervenciones.isNotEmpty()
            tvIntervencionesLabel.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE
            tvIntervencionesLista.visibility = if (tieneIntervenciones) View.VISIBLE else View.GONE

            if (tieneIntervenciones) {
                tvIntervencionesLista.text = averia.intervenciones.joinToString("\n") { "• $it" }
            }

            gestionarVisibilidadAcciones(averia.estadoAveriaCalculado)
        }
    }

    /**
     * Máquina de estados visual de la botonera.
     * Guía al operario eliminando ruido visual (acciones no permitidas en el estado actual).
     */
    private fun gestionarVisibilidadAcciones(estado: String) {
        with(binding) {
            when (estado) {
                "Nueva" -> {
                    btnAceptarAveria.visibility = View.VISIBLE
                    btnFinalizarAveria.visibility = View.GONE
                    btnRegistrarIntervencion.visibility = View.GONE
                }
                "Recibida", "Pendiente", "En curso" -> {
                    btnAceptarAveria.visibility = View.GONE
                    btnFinalizarAveria.visibility = View.VISIBLE
                    btnRegistrarIntervencion.visibility = View.VISIBLE
                }
                "Finalizada" -> {
                    btnAceptarAveria.visibility = View.GONE
                    btnFinalizarAveria.visibility = View.GONE
                    btnRegistrarIntervencion.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Helper de estilizado semántico.
     * Aplica jerarquía de colores (Gestalt) para facilitar el reconocimiento rápido de estados.
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

    private fun alternarInteractividad(activado: Boolean) {
        binding.btnAceptarAveria.isEnabled = activado
        binding.btnFinalizarAveria.isEnabled = activado
    }

    private fun procesarEventoFinalizacion(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        // Refresco mandatorio post-transacción para asegurar convergencia de datos.
        viewModel.sincronizarConServidor(idAveriaActual)
    }

    private fun obtenerDescripcionEstadoMaquina(codigo: Int): String = when (codigo) {
        801 -> "Operativa"
        802 -> "Averiada"
        803 -> "En mantenimiento"
        804 -> "Fuera de servicio"
        0 -> "Estado sin especificar"
        else -> "Estado desconocido"
    }

    override fun onResume() {
        super.onResume()
        // Estrategia de re-fetching pasivo para garantizar datos frescos tras navegar.
        if (idAveriaActual != -1) viewModel.sincronizarConServidor(idAveriaActual)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpieza de Binding mandatoria para evitar fugas de memoria del Layout.
        _binding = null
    }
}