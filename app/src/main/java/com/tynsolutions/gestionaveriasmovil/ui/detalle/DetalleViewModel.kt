package com.tynsolutions.gestionaveriasmovil.ui.detalle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.data.local.FakeDataSource
import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

class DetalleViewModel : ViewModel() {

    // Backing property: Estado reactivo encapsulado que representa la avería actual.
    private val _averia = MutableLiveData<Averia>()

    // Exposición inmutable del estado para que la Vista se suscriba (Observer Pattern).
    val averia: LiveData<Averia> get() = _averia

    /**
     * Ejecuta la consulta al origen de datos (Data Source / Repository) para obtener
     * la entidad correspondiente al ID proporcionado.
     *
     * @param id Identificador primario de la avería solicitada.
     */
    fun cargarAveria(id: Int) {
        // En una implementación Clean Architecture, esto invocaría un UseCase (ej: GetAveriaByIdUseCase).
        val averiaEncontrada = FakeDataSource.averias.find { it.id == id }

        // Actualizamos el estado reactivo solo si la entidad existe (null safety).
        averiaEncontrada?.let {
            _averia.value = it
        }
    }

    /**
     * Procesa la aceptación de la avería asignando un timestamp local (Fase 1).
     * @param id Identificador único de la entidad a modificar.
     */
    fun aceptarAveria(id: Int) {
        // 1. Localización de la entidad en el mock data source
        val averia = FakeDataSource.averias.find { it.id == id }

        averia?.let {
            // 2. Generación de timestamp para simular la respuesta del servidor
            val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val timestamp = format.format(java.util.Date())

            // 3. Actualización de la colección local (Simulación de persistencia)
            val index = FakeDataSource.averias.indexOf(it)
            FakeDataSource.averias[index] = it.copy(fechaAceptacion = timestamp)

            // 4. Emisión del nuevo estado a los observadores de la UI
            _averia.value = FakeDataSource.averias[index]
        }
    }

    /**
     * CU06: Finalizar Avería.
     * Cambia el estado de la avería a un estado terminal ("Finalizada").
     * Esto simula la lógica del Bloque 7.3 en la Fase 1.
     *
     * @param averiaId El identificador de la avería que se va a cerrar.
     */
    fun finalizarAveria(averiaId: Int) {
        val averia = FakeDataSource.averias.find { it.id == averiaId }

        averia?.let {
            val index = FakeDataSource.averias.indexOf(it)

            // Obtenemos la fecha actual formateada (o como la use tu modelo, ej: String o Date)
            val fechaHoy = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            // IMPORTANTE: Al poner valor a fechaFinalizacion, el estado cambia automáticamente
            val averiaActualizada = it.copy(fechaFinalizacion = fechaHoy)

            FakeDataSource.averias[index] = averiaActualizada

            // Notificamos a la UI. El Observer recibirá el objeto y al leer
            // 'estadoAveriaCalculado' verá que ahora es "Finalizada".
            _averia.value = averiaActualizada
        }
    }
}