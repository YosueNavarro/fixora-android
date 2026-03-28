package com.tynsolutions.gestionaveriasmovil.data.repository

import android.util.Log
import com.tynsolutions.gestionaveriasmovil.data.network.ApiService
import com.tynsolutions.gestionaveriasmovil.data.network.dto.CambiarEstadoMaquinaRequest

/**
 * Repositorio exclusivo para el dominio de Maquinaria.
 * Desacopla la gestión del hardware físico del flujo de trabajo de las incidencias,
 * cumpliendo con el Principio de Responsabilidad Única (SRP).
 */
class MaquinariaRepository(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "MaquinariaRepo_Sec"
    }

    /**
     * Sincronización de estado físico (PUT): Actualiza el estatus operativo del hardware.
     * Consigna los cambios directamente contra el endpoint especializado del backend.
     *
     * @param idMaquinaria Identificador del equipo físico.
     * @param codigoEstado Código de catálogo representativo del nuevo estado.
     * @return [Result] confirmando la mutación en base de datos.
     */
    suspend fun cambiarEstadoMaquinaria(idMaquinaria: Int, codigoEstado: Int): Result<String> {
        return try {
            val request = CambiarEstadoMaquinaRequest(codigoEstado)
            val response = apiService.cambiarEstadoMaquina(idMaquinaria, request)

            if (response.isSuccessful) {
                Result.success("El estado de la maquinaria ha sido sincronizado correctamente.")
            } else {
                Log.w(TAG, "Denegación de mutación. HTTP: ${response.code()} - ${response.errorBody()?.string()}")
                Result.failure(Exception("Operación denegada. El servidor devolvió el código: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Caída de red sincronizando maquinaria ID: $idMaquinaria", e)
            Result.failure(Exception("Error de conectividad al sincronizar la máquina con el servidor."))
        }
    }
}