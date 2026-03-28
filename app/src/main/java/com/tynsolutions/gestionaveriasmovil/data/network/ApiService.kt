package com.tynsolutions.gestionaveriasmovil.data.network

import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaDetalleResponse
import com.tynsolutions.gestionaveriasmovil.data.network.dto.AveriaTecnicoResponse
import com.tynsolutions.gestionaveriasmovil.data.network.dto.CambiarEstadoMaquinaRequest
import com.tynsolutions.gestionaveriasmovil.data.network.dto.CambiarEstadoMaquinaResponse
import com.tynsolutions.gestionaveriasmovil.data.network.dto.IntervencionRequestDTO
import com.tynsolutions.gestionaveriasmovil.data.network.dto.LoginRequest
import com.tynsolutions.gestionaveriasmovil.data.network.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato de la API REST para el dominio de gestión de averías.
 * Define la topología de los endpoints y los métodos HTTP autorizados.
 *
 * POLÍTICA DE SEGURIDAD: La inyección de tokens de autorización (JWT) está delegada
 * de forma global en la capa del Interceptor de OkHttp, garantizando que ninguna
 * petición expuesta en esta interfaz viaje sin firma criptográfica (a excepción del login).
 */
interface ApiService {

    /**
     * Endpoint de autenticación inicial.
     * Única ruta no autenticada del sistema. Intercambia credenciales por un JWT válido.
     *
     * @param request Payload con email y contraseña.
     * @return [LoginResponse] con el token de sesión y el perfil del técnico.
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Consulta el catálogo de incidencias asignadas al técnico.
     * Implementa filtrado opcional por estado a través de Query Params.
     *
     * @param idTecnico Identificador único del operario (Extraído de la sesión).
     * @param tipoLista Filtro opcional ("nuevas", "en_curso", "historico").
     * @return [AveriaTecnicoResponse] con la colección de datos.
     */
    @GET("tecnicos/{id}/averias")
    suspend fun getAveriasTecnico(
        @Path("id") idTecnico: Int,
        @Query("tipo") tipoLista: String? = null
    ): Response<AveriaTecnicoResponse>

    /**
     * Recupera la entidad completa de una avería específica para la vista de detalle.
     *
     * @param idAveria Clave primaria de la incidencia.
     * @return [AveriaDetalleResponse] con los datos extendidos.
     */
    @GET("averias/{id}")
    suspend fun getAveriaDetalle(@Path("id") idAveria: Int): Response<AveriaDetalleResponse>

    /**
     * Mutación de estado: Marca una incidencia como "Recibida/En curso".
     * Operación idempotente (PUT) que inicializa el temporizador de resolución del técnico.
     *
     * @param idAveria Identificador de la avería a modificar.
     */
    @PUT("averias/{id}/aceptar")
    suspend fun aceptarAveria(@Path("id") idAveria: Int): Response<Any>

    /**
     * Mutación de datos: Anexa un nuevo registro de intervención al historial de la avería.
     *
     * @param idAveria Identificador de la avería objetivo.
     * @param request Payload con el texto del proceso realizado.
     */
    @PUT("averias/{id}/intervenciones")
    suspend fun registrarIntervencion(
        @Path("id") idAveria: Int,
        @Body request: IntervencionRequestDTO
    ): Response<Any>

    /**
     * Mutación de estado: Cierra formalmente el ciclo de vida de la incidencia.
     * Sella el registro y estampa la fecha de finalización en el backend.
     *
     * @param idAveria Identificador de la avería a clausurar.
     */
    @PUT("averias/{id}/finalizar")
    suspend fun finalizarAveria(@Path("id") idAveria: Int): Response<Any>

    /**
     * Actualiza el estado operativo de la maquinaria en la planta (Ej: de 'Averiada' a 'Operativa').
     *
     * @param idMaquinaria Identificador del equipo físico.
     * @param request Payload con el nuevo código de estado.
     */
    @PUT("maquinaria/{id}/estado")
    suspend fun cambiarEstadoMaquina(
        @Path("id") idMaquinaria: Int,
        @Body request: CambiarEstadoMaquinaRequest
    ): Response<CambiarEstadoMaquinaResponse>
}