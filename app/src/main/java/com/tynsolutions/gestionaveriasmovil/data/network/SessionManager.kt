package com.tynsolutions.gestionaveriasmovil.data.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de sesión responsable de almacenar las credenciales y datos básicos
 * de forma persistente.
 * TODO (Seguridad): En la fase de release, migrar a EncryptedSharedPreferences de la librería de AndroidX Security.
 */
class SessionManager(context: Context) {

    // Archivo de preferencias privado (solo esta app puede leerlo)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "fixora_secure_prefs"
        const val KEY_USER_TOKEN = "jwt_token"
        const val KEY_USER_ID = "user_id" // Constante para persistir el ID del técnico
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
     * Almacena el identificador único del técnico en base de datos.
     * Dato crítico para poder solicitar sus averías asignadas posteriormente.
     * * @param id Identificador numérico del técnico.
     */
    fun saveUserId(id: Int) {
        prefs.edit().putInt(KEY_USER_ID, id).apply()
    }

    /**
     * Recupera el ID del técnico.
     * Retorna -1 si no hay ningún usuario logueado en la sesión actual.
     */
    fun fetchUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    /**
     * Borra la sesión actual completa (Logout). Purga credenciales y datos de usuario.
     */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_ID)
            .apply() // Ejecución asíncrona recomendada
    }
}