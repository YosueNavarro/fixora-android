package com.tynsolutions.gestionaveriasmovil.data.network

import com.tynsolutions.gestionaveriasmovil.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato de la API REST para la gestión de averías de TYN Solutions.
 * Utiliza Corrutinas (suspend) para concurrencia asíncrona segura.
 * * ATENCIÓN: Todos los endpoints privados requieren inyección explícita del token JWT
 * mediante la cabecera 'Authorization' para garantizar la seguridad del sistema.
 */
interface ApiService {

    // ==========================================
    // 1. AUTENTICACIÓN (ENDPOINT PÚBLICO)
    // ==========================================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ==========================================
    // 2. OBTENCIÓN DE DATOS (ENDPOINTS PRIVADOS - GET)
    // ==========================================

    /**
     * Obtiene las averías del técnico asignado.
     * @param token Cadena JWT con el formato "Bearer <token>".
     * @param idTecnico Identificador único del técnico en BD.
     * @param filtroTipoLista Filtro opcional para la consulta SQL del backend.
     */
    @GET("tecnicos/{id}/averias")
    suspend fun getAveriasTecnico(
        @Path("id") idTecnico: Int,
        @Query("tipo") tipoLista: String? = null
    ): Response<AveriaTecnicoResponse>

    /**
     * Obtiene los códigos y descripciones de los estados de situación posibles.
     */
    @GET("estado/situacion")
    suspend fun getEstadosSituacion(
        @Header("Authorization") token: String
    ): Response<List<EstadoSituacionResponse>>

    // ==========================================
    // 3. MODIFICACIÓN DE DATOS (ENDPOINTS PRIVADOS - PUT)
    // ==========================================

    /**
     * Aceptar una avería asignada.
     */
    @PUT("averias/{id}/aceptar")
    suspend fun aceptarAveria(
        @Header("Authorization") token: String,
        @Path("id") idAveria: Int,
        @Body requestBody: Map<String, String> = emptyMap()
    ): Response<AveriaItemDTO>

    /**
     * Registrar intervención (cambio en descripción de avería).
     */
    @PUT("averias/{id}/intervenciones")
    suspend fun registrarIntervencion(
        @Header("Authorization") token: String,
        @Path("id") idAveria: Int,
        @Body request: IntervencionRequest
    ): Response<AveriaItemDTO>

    /**
     * Finalizar una avería.
     */
    @PUT("averias/{id}/finalizar")
    suspend fun finalizarAveria(
        @Header("Authorization") token: String,
        @Path("id") idAveria: Int,
        @Body requestBody: Map<String, String> = emptyMap()
    ): Response<AveriaItemDTO>

    /**
     * Cambiar estado de maquinaria a "fuera de servicio" u "operativa".
     */
    @PUT("maquinaria/{id}/estado")
    suspend fun cambiarEstadoMaquina(
        @Header("Authorization") token: String,
        @Path("id") idMaquinaria: Int,
        @Body request: CambiarEstadoMaquinaRequest
    ): Response<Map<String, String>>
}