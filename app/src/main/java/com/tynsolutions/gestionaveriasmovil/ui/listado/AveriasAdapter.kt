package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tynsolutions.gestionaveriasmovil.databinding.ItemAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

// Recibe la lista de averías por parámetro
class AveriasAdapter(private var listaAverias: List<Averia>) : RecyclerView.Adapter<AveriasAdapter.AveriaViewHolder>() {

    // 1. El ViewHolder: guarda las referencias a las vistas de una tarjeta
    inner class AveriaViewHolder(private val binding: ItemAveriaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(averia: Averia) {
            // Enlazamos los datos del modelo con los elementos del XML
            binding.tvTituloAveria.text = averia.titulo
            binding.tvMaquinaria.text = averia.maquinaria
            binding.tvFecha.text = averia.fechaAsignacion
            binding.tvEstado.text = averia.estado

            // Más adelante aquí pondremos el clic para ir al Detalle
        }
    }

    // 2. Inflar el diseño: "Imprime" la tarjeta en blanco
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AveriaViewHolder {
        val binding = ItemAveriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AveriaViewHolder(binding)
    }

    // 3. Rellenar datos: Le pasa a la tarjeta los datos de la fila correspondiente
    override fun onBindViewHolder(holder: AveriaViewHolder, position: Int) {
        val averiaActual = listaAverias[position]
        holder.bind(averiaActual)
    }

    // 4. Contar elementos: Le dice al RecyclerView cuántas tarjetas tiene que dibujar
    override fun getItemCount(): Int {
        return listaAverias.size
    }

    // Función extra: Para cuando implementemos los filtros de Nuevas/Recibidas
    fun actualizarLista(nuevaLista: List<Averia>) {
        listaAverias = nuevaLista
        notifyDataSetChanged() // Avisa al RecyclerView de que hay datos nuevos y debe repintarse
    }
}