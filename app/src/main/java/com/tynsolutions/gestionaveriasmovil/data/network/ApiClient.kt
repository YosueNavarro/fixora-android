package com.tynsolutions.gestionaveriasmovil.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton que provee la instancia configurada del cliente API.
 * Garantiza que solo exista una conexión HTTP abierta en toda la aplicación.
 */
object ApiClient {

    // IP del servidor (Modificar según tu entorno de desarrollo o producción)
    // 10.0.2.2 es el alias del emulador de Android para referirse al 'localhost' del PC
    private const val BASE_URL = "http://10.0.2.2:9090/api/"

    private var apiService: ApiService? = null

    /**
     * Construye y devuelve el servicio de Retrofit con los interceptores inyectados.
     */
    fun getApiService(sessionManager: SessionManager): ApiService {
        if (apiService == null) {

            // Logger para depuración: nos permite ver las peticiones JSON en el Logcat
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Configuramos OkHttp con nuestro vigilante de seguridad y tiempos de espera (timeouts)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(sessionManager))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // Construimos Retrofit conectando OkHttp y Gson
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}