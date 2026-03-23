package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.databinding.ItemAveriaBinding
import androidx.core.graphics.toColorInt

/**
 * Adaptador de alto rendimiento para el listado de averías.
 * Consume estrictamente modelos de Dominio (Averia), garantizando el encapsulamiento
 * y previniendo la inyección de datos no sanitizados o estructuras inestables desde la capa de red.
 */
class AveriasAdapter(
    // Exigimos una lista inmutable del modelo de negocio
    private var listaAverias: List<Averia> = emptyList(),
    // Callback tipado fuertemente al modelo de dominio para transiciones seguras
    private val onAveriaClick: (Averia) -> Unit
) : RecyclerView.Adapter<AveriasAdapter.AveriaViewHolder>() {

    inner class AveriaViewHolder(private val binding: ItemAveriaBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Vincula la entidad Averia (ya procesada y saneada) con los componentes de la Card.
         * Al delegar la lógica de negocio al Mapper previo, el Adapter alcanza una complejidad O(1).
         */
        fun bind(averia: Averia) {

            // 1. TÍTULO
            binding.tvTituloAveria.text = averia.titulo

            // 2. SUBTÍTULO: Refactorizado para mostrar únicamente el nombre
            // Se elimina la concatenación con el estado físico de la máquina.
            binding.tvMaquinaria.text = averia.maquinaria

            // 3. FECHA
            binding.tvFecha.text = averia.fechaInforme

            // 4. BADGE DE GESTIÓN
            val estadoGestion = averia.estadoAveriaCalculado
            binding.tvEstado.text = estadoGestion

            val colorTexto = when (estadoGestion.lowercase()) {
                "nueva" -> "#03A9F4".toColorInt()      // Azul Claro
                "finalizada" -> "#2E7D32".toColorInt() // Verde
                else -> "#3A75B5".toColorInt()         // Azul oscuro (Recibidas / En curso)
            }
            binding.tvEstado.setTextColor(colorTexto)

            binding.root.setOnClickListener {
                onAveriaClick(averia)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AveriaViewHolder {
        val binding = ItemAveriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AveriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AveriaViewHolder, position: Int) {
        // Acceso al índice del array con garantía de tipo estricto
        holder.bind(listaAverias[position])
    }

    override fun getItemCount(): Int = listaAverias.size

    /**
     * Actualiza la colección de datos de forma atómica.
     * @param nuevaLista Colección validada proveniente del origen de la verdad (SSOT).
     */
    fun actualizarLista(nuevaLista: List<Averia>) {
        listaAverias = nuevaLista
        // Refresco de la interfaz tras la sustitución de la referencia de memoria
        notifyDataSetChanged()
    }
}