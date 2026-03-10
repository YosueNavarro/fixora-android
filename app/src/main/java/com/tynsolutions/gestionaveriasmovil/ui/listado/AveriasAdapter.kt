package com.tynsolutions.gestionaveriasmovil.ui.listado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tynsolutions.gestionaveriasmovil.databinding.ItemAveriaBinding
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

/**
 * Adaptador para el RecyclerView del listado de averías.
 * Implementa el patrón Adapter para actuar como puente entre la colección de datos (Domain)
 * y las vistas reciclables (UI).
 *
 * @property listaAverias Dataset inicial que alimentará el listado.
 * @property onAveriaClick Función de orden superior (Higher-Order Function) que delega
 * la responsabilidad del evento click a la capa superior (Fragmento).
 */
class AveriasAdapter(
    private var listaAverias: List<Averia>,
    private val onAveriaClick: (Int) -> Unit
) : RecyclerView.Adapter<AveriasAdapter.AveriaViewHolder>() {

    /**
     * ViewHolder interno que almacena y recicla las referencias a las vistas de un ítem.
     * Al usar ViewBinding garantizamos Null Safety y Type Safety en la UI.
     */
    inner class AveriaViewHolder(private val binding: ItemAveriaBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Función de renderizado (Bind). Mapea los atributos de la entidad de dominio
         * hacia los componentes visuales del layout.
         *
         * @param averia Instancia del modelo de dominio a representar.
         */
        fun bind(averia: Averia) {
            // Inyección directa de datos simples
            binding.tvTituloAveria.text = averia.titulo

            // Composición de strings para presentación UX: Nombre + [Estado Físico]
            binding.tvMaquinaria.text = "${averia.maquinaria} (${averia.estadoMaquinaria})"

            // Null-safety handling: Uso del operador Elvis (?:) para proveer un fallback
            // en caso de que la entidad no tenga fecha de asignación.
            binding.tvFecha.text = averia.fechaAsignacion ?: "Sin asignar"

            // Mapeo de propiedad calculada (regla de negocio de UI)
            binding.tvEstado.text = averia.estadoAveriaCalculado

            // Event Routing: Interceptamos el evento de UI a nivel de raíz (ConstraintLayout/CardView)
            // y lo despachamos hacia la función inyectada por el constructor.
            binding.root.setOnClickListener {
                onAveriaClick(averia.id)
            }
        }
    }

    /**
     * Invocado por el LayoutManager para instanciar una nueva vista cuando no hay suficientes
     * vistas recicladas disponibles en la caché del RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AveriaViewHolder {
        // Inflamos el layout asociado utilizando el contexto del ViewGroup padre
        val binding = ItemAveriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AveriaViewHolder(binding)
    }

    /**
     * Invocado por el LayoutManager para reemplazar el contenido de una vista reciclada
     * con los datos de un elemento específico del dataset.
     */
    override fun onBindViewHolder(holder: AveriaViewHolder, position: Int) {
        val averiaActual = listaAverias[position]
        holder.bind(averiaActual)
    }

    /**
     * Retorna el tamaño total del dataset actual. Vital para que el RecyclerView
     * sepa cuántos elementos debe iterar.
     */
    override fun getItemCount(): Int {
        return listaAverias.size
    }

    /**
     * Mutador del estado interno del adaptador.
     * Actualiza la colección en memoria y fuerza un repintado de la lista completa.
     *
     * @param nuevaLista Nuevo dataset filtrado o actualizado.
     * * Nota técnica: Para listas masivas en producción, se recomienda refactorizar
     * usando 'DiffUtil' o 'ListAdapter' en lugar de 'notifyDataSetChanged()' para
     * calcular solo las diferencias y optimizar el rendimiento.
     */
    fun actualizarLista(nuevaLista: List<Averia>) {
        listaAverias = nuevaLista
        notifyDataSetChanged()
    }
}