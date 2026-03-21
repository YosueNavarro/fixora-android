package com.tynsolutions.gestionaveriasmovil.data.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Motor central de comunicaciones de Fixora.
 * Configura Retrofit con un interceptor de seguridad para inyectar el JWT
 * automáticamente en cada petición saliente.
 */
object RetrofitClient {
    // La IP del emulador para conectar con el localhost de tu PC (XAMPP/NetBeans)
    private const val BASE_URL = "http://10.0.2.2:9090/api/"

    private var retrofit: Retrofit? = null

    /**
     * Provee la instancia configurada del servicio de API.
     * @param context Necesario para que el interceptor acceda al SessionManager.
     */
    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val sessionManager = SessionManager(context)

            // Interceptor de Logs (Para que veas las peticiones en el Logcat)
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // CLIENTE SEGURO: Inyecta el token de Nereida en cada llamada
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    sessionManager.fetchAuthToken()?.let {
                        requestBuilder.addHeader("Authorization", "Bearer $it")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}