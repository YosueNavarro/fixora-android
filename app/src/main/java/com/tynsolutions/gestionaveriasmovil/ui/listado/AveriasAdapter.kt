package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaItemDTO
import com.tynsolutions.gestionaveriasmovil.databinding.ItemAveriaBinding

/**
 * Adaptador para el RecyclerView del listado de averías.
 * Consume directamente el DTO de la API para optimizar el rendimiento y evitar mapeos innecesarios.
 */
class AveriasAdapter(
    private var listaAverias: List<AveriaItemDTO> = emptyList(),
    private val onAveriaClick: (Int) -> Unit
) : RecyclerView.Adapter<AveriasAdapter.AveriaViewHolder>() {

    inner class AveriaViewHolder(private val binding: ItemAveriaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(averia: AveriaItemDTO) {
            // Mapeamos los datos reales de la API hacia la UI.
            // Si el nombre de la máquina viene nulo, ponemos un texto por defecto.
            val nombreMaquina = averia.maquinaria?.nombre ?: "Máquina desconocida"
            val tipoAveria = averia.tipoAveria?.descripcion ?: "Tipo no especificado"

            binding.tvTituloAveria.text = "$nombreMaquina - $tipoAveria"
            binding.tvMaquinaria.text = nombreMaquina

            // Usamos la fecha de asignación. Si es nula, mostramos aviso.
            binding.tvFecha.text = averia.fechaAsigTecnico ?: "Sin asignar"

            // Calculamos el estado sobre la marcha basándonos en las fechas reales de la BD
            val estadoCalculado = when {
                !averia.fechaFinalizTecnico.isNullOrEmpty() -> "Finalizada"
                !averia.fechaAcepTecnico.isNullOrEmpty() -> "Recibida"
                !averia.fechaAsigTecnico.isNullOrEmpty() -> "Nueva"
                else -> "Pendiente"
            }
            binding.tvEstado.text = estadoCalculado

            // Routing del clic
            binding.root.setOnClickListener {
                onAveriaClick(averia.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AveriaViewHolder {
        val binding = ItemAveriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AveriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AveriaViewHolder, position: Int) {
        holder.bind(listaAverias[position])
    }

    override fun getItemCount(): Int = listaAverias.size

    fun actualizarLista(nuevaLista: List<AveriaItemDTO>) {
        listaAverias = nuevaLista
        notifyDataSetChanged()
    }
}