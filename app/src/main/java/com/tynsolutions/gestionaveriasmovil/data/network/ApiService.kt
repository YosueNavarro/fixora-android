package com.tynsolutions.gestionaveriasmovil.data.network

import com.tynsolutions.gestionaveriasmovil.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato de la API REST para la gestión de averías de TYN Solutions.
 * Centraliza todas las operaciones de red, delegando la seguridad (JWT)
 * al Interceptor de OkHttp para mantener una arquitectura limpia.
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
     * Recupera el listado de averías asignadas a un técnico específico.
     */
    @GET("tecnicos/{id}/averias")
    suspend fun getAveriasTecnico(
        @Path("id") idTecnico: Int,
        @Query("tipo") tipoLista: String? = null
    ): Response<AveriaTecnicoResponse>

    /**
     * Obtiene el catálogo de estados de situación para la maquinaria (Operativa, Averiada, etc.).
     * Eliminamos el parámetro @Header porque el Interceptor ya lo gestiona.
     */

    /**
     * Recupera el detalle de una avería envuelto en el objeto 'data'.
     */
    @GET("averias/{id}")
    suspend fun getAveriaDetalle(@Path("id") idAveria: Int): Response<AveriaDetalleResponse>

    // ==========================================
    // 3. OPERACIONES DE AVERÍAS (PUT)
    // ==========================================

    @PUT("averias/{id}/aceptar")
    suspend fun aceptarAveria(@Path("id") idAveria: Int): Response<Any>

    @PUT("averias/{id}/intervenciones")
    suspend fun registrarIntervencion(
        @Path("id") idAveria: Int,
        @Body request: IntervencionRequestDTO
    ): Response<Any>

    @PUT("averias/{id}/finalizar")
    suspend fun finalizarAveria(@Path("id") idAveria: Int): Response<Any>

    // ==========================================
    // 4. GESTIÓN DE MAQUINARIA (PUT)
    // ==========================================

    /**
     * Actualiza el estado operativo de una máquina específica.
     * Eliminamos el parámetro @Header para evitar redundancias con el Interceptor.
     */
    @PUT("maquinaria/{id}/estado")
    suspend fun cambiarEstadoMaquina(
        @Path("id") idMaquinaria: Int,
        @Body request: CambiarEstadoMaquinaRequest
    ): Response<Map<String, String>>
}