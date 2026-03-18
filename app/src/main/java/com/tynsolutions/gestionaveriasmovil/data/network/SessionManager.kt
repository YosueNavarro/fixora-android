package com.tynsolutions.gestionaveriasmovil.data.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de sesión responsable de almacenar las credenciales de forma persistente.
 * TODO (Seguridad): En la fase de release, migrar a EncryptedSharedPreferences de la librería de AndroidX Security.
 */
class SessionManager(context: Context) {

    // Archivo de preferencias privado (solo esta app puede leerlo)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "fixora_secure_prefs"
        const val KEY_USER_TOKEN = "jwt_token"
    }

    /**
     * Guarda el token JWT devuelto por el servidor tras un login exitoso.
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_USER_TOKEN, token).apply()
    }

    /**
     * Recupera el token JWT. Retorna null si el usuario no ha iniciado sesión.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_USER_TOKEN, null)
    }

    /**
     * Borra la sesión actual (Logout).
     */
    fun clearSession() {
        prefs.edit().remove(KEY_USER_TOKEN).apply()
    }
}