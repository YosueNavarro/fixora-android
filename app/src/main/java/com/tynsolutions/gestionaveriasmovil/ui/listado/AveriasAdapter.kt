package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tynsolutions.gestionaveriasmovil.databinding.ItemAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Adaptador de alto rendimiento para la renderización de incidencias técnicas.
 * Implementa [DiffUtil] para optimizar los ciclos de refresco de la UI y garantizar
 * una experiencia de usuario fluida (60 FPS) durante el scroll y filtrado.
 */
class AveriasAdapter(
    private var listaAverias: List<Averia> = emptyList(),
    private val onAveriaClick: (Averia) -> Unit
) : RecyclerView.Adapter<AveriasAdapter.AveriaViewHolder>() {

    /**
     * ViewHolder especializado en la vinculación de datos de dominio.
     * Mantiene las referencias a las vistas mediante ViewBinding para evitar el coste
     * computacional de 'findViewById'.
     */
    inner class AveriaViewHolder(private val binding: ItemAveriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(averia: Averia) {
            with(binding) {
                tvTituloAveria.text = averia.titulo
                tvMaquinaria.text = averia.maquinaria.nombre

                // Fecha contextual según el ciclo de vida de la incidencia
                val textoFecha = when (averia.estadoAveriaCalculado.lowercase()) {
                    "nueva" -> "Asignada el: ${averia.fechaAsignacion ?: "Pendiente"}"
                    "recibida", "en curso", "pendiente" -> "Aceptada el: ${averia.fechaAceptacion ?: averia.fechaAsignacion ?: "Desconocida"}"
                    "finalizada" -> "Finalizada el: ${averia.fechaFinalizacion ?: "Desconocida"}"
                    else -> averia.fechaInforme
                }

                tvFecha.text = textoFecha

                // Gestión de la identidad visual del estado
                configurarBadgeEstado(averia.estadoAveriaCalculado)

                root.setOnClickListener { onAveriaClick(averia) }
            }
        }

        /**
         * Aplica la semántica de colores según el estado administrativo.
         * Nota: En entornos de producción, estos colores deberían provenir de atributos
         * de tema (Surface/OnSurface) para soportar Modo Oscuro dinámico.
         */
        private fun configurarBadgeEstado(estado: String) {
            val colorHex = when (estado.lowercase()) {
                "nueva" -> "#03A9F4"      // Light Blue 500
                "finalizada" -> "#2E7D32" // Green 800
                else -> "#1976D2"         // Blue 700 (Recibidas/Pendientes)
            }

            val colorInt = colorHex.toColorInt()
            binding.tvEstado.apply {
                text = estado
                setTextColor(colorInt)
                // Aplicamos un fondo sutil (15% opacidad) para mejorar la jerarquía visual
                setBackgroundColor(ColorUtils.setAlphaComponent(colorInt, 40))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AveriaViewHolder {
        val binding = ItemAveriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AveriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AveriaViewHolder, position: Int) {
        holder.bind(listaAverias[position])
    }

    override fun getItemCount(): Int = listaAverias.size

    /**
     * Actualiza la colección de datos utilizando el algoritmo de diferencia de Myers.
     * Esta operación es atómica y despacha notificaciones específicas (ItemChanged,
     * ItemInserted, etc.) al RecyclerView.
     */
    fun actualizarLista(nuevaLista: List<Averia>) {
        val diffCallback = AveriaDiffCallback(listaAverias, nuevaLista)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.listaAverias = nuevaLista
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Utilidad interna para el cálculo de diferencias entre colecciones de averías.
     */
    private class AveriaDiffCallback(
        private val oldList: List<Averia>,
        private val newList: List<Averia>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}