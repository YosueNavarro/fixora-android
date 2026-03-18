package com.tynsolutions.gestionaveriasmovil.data.network

import com.tynsolutions.gestionaveriasmovil.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato de la API REST para la gestión de averías.
 * Utiliza Corrutinas (suspend) para ejecutar las peticiones en hilos secundarios
 * sin bloquear la interfaz de usuario. Envuelve los retornos en Response<T> para
 * poder gestionar profesionalmente los códigos HTTP (200, 401, 403, 500).
 */
interface ApiService {

    // ==========================================
    // 1. AUTENTICACIÓN
    // ==========================================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ==========================================
    // 2. OBTENCIÓN DE DATOS (GET)
    // ==========================================

    /**
     * Obtiene las averías del técnico.
     * El filtro se envía como parámetro de consulta en la URL (?filtro=nuevas)
     */
    @GET("/tecnicos/{id}/averias")
    suspend fun getAveriasTecnico(
        @Path("id") idTecnico: Int,
        @Query("filtro") filtroTipoLista: String
    ): Response<List<AveriaResponse>>

    /**
     * Obtiene los códigos y descripciones de los estados de situación posibles.
     */
    @GET("/estado/situacion")
    suspend fun getEstadosSituacion(): Response<List<EstadoSituacionResponse>>

    // ==========================================
    // 3. MODIFICACIÓN DE DATOS (PUT)
    // ==========================================

    /**
     * Aceptar una avería asignada.
     * La lógica de actualización de timestamp ocurre en el servidor.
     * Enviamos un mapa vacío por si el framework del servidor exige un cuerpo (body) en las peticiones PUT.
     */
    @PUT("/averias/{id}/aceptar")
    suspend fun aceptarAveria(
        @Path("id") idAveria: Int,
        @Body requestBody: Map<String, String> = emptyMap()
    ): Response<AveriaResponse>

    /**
     * Registrar intervención (cambio en descripción de avería).
     */
    @PUT("/averias/{id}/intervenciones")
    suspend fun registrarIntervencion(
        @Path("id") idAveria: Int,
        @Body request: IntervencionRequest
    ): Response<AveriaResponse>

    /**
     * Finalizar una avería.
     * El servidor aplica el timestamp automáticamente.
     */
    @PUT("/averias/{id}/finalizar")
    suspend fun finalizarAveria(
        @Path("id") idAveria: Int,
        @Body requestBody: Map<String, String> = emptyMap()
    ): Response<AveriaResponse>

    /**
     * Cambiar estado de maquinaria a "fuera de servicio" u "operativa".
     * Devuelve un JSON genérico con un mensaje de confirmación.
     */
    @PUT("/maquinaria/{id}/estado")
    suspend fun cambiarEstadoMaquina(
        @Path("id") idMaquinaria: Int,
        @Body request: CambiarEstadoMaquinaRequest
    ): Response<Map<String, String>>
}