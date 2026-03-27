package com.tynsolutions.gestionaveriasmovil.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP Singleton para la configuración y orquestación de Retrofit.
 * Centraliza la provisión de servicios de red, garantizando una única instancia
 * del pool de conexiones HTTP para optimizar el consumo de recursos del dispositivo.
 */
object ApiClient {

    // Configuración del entorno (Dev/Localhost)
    // NOTA DE SEGURIDAD CRÍTICA: En un entorno de producción real, este endpoint debe
    // migrar estrictamente a HTTPS para evitar ataques MITM (Man-in-the-Middle).
    private const val BASE_URL = "http://10.208.137.26:9090/api/"

    // La anotación @Volatile garantiza que los cambios en esta variable sean visibles
    // inmediatamente por todos los hilos concurrentes.
    @Volatile
    private var apiService: ApiService? = null

    /**
     * Instancia y provee el servicio de red aplicando el patrón Singleton con
     * Double-Checked Locking (DCL). Esto previene condiciones de carrera (Race Conditions)
     * cuando múltiples corrutinas solicitan el cliente simultáneamente al inicio de la app.
     * * @param sessionManager Gestor inyectado para la intercepción y firmado de tokens (JWT).
     * @return Implementación concreta de la interfaz [ApiService].
     */
    fun getApiService(sessionManager: SessionManager): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(sessionManager).also { apiService = it }
        }
    }

    /**
     * Construye la cadena de interceptores y la configuración base del cliente HTTP.
     * Separar la construcción en una función privada mejora la legibilidad de la instanciación.
     */
    private fun buildApiService(sessionManager: SessionManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Nivel BODY configurado para entornos de desarrollo.
            // TODO: En producción debe ajustarse a NONE o BASIC para evitar la exposición
            // de PII (Personally Identifiable Information) o credenciales en el Logcat.
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            // Timeouts conservadores para lidiar con redes móviles inestables (3G/4G/Edge)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}