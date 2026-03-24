package com.tynsolutions.gestionaveriasmovil.data.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de persistencia local para el ciclo de vida de la sesión del usuario.
 * Encapsula el acceso a las SharedPreferences para el almacenamiento seguro
 * de credenciales (JWT) y metadatos críticos del operario.
 *
 * TODO (Security): Para cumplimiento estricto normativo en producción, migrar
 * la implementación subyacente a EncryptedSharedPreferences (AndroidX Security)
 * para garantizar el cifrado At-Rest (AES256-GCM) de los tokens.
 */
class SessionManager(context: Context) {

    // Instancia aislada en modo privado (Sandbox) para prevenir filtraciones (Data Leakage)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        // Constantes privadas: Restringen el acceso a las claves de persistencia
        // estrictamente a las fronteras de esta clase.
        private const val PREFS_NAME = "fixora_secure_prefs"
        private const val KEY_USER_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
    }

    /**
     * Persiste el token criptográfico (JWT) emitido por el backend tras la autenticación.
     * Utiliza ejecución asíncrona [apply] para no bloquear el hilo principal (UI Thread).
     *
     * @param token Cadena JWT firmada.
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_USER_TOKEN, token).apply()
    }

    /**
     * Recupera el token de autorización activo.
     *
     * @return El token JWT actual, o null si la sesión ha expirado o no existe.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_USER_TOKEN, null)
    }

    /**
     * Almacena el identificador relacional del técnico.
     * Este valor es crítico para aislar el dominio de datos en las peticiones posteriores.
     *
     * @param id Clave primaria (FK) del técnico en el sistema central.
     */
    fun saveUserId(id: Int) {
        prefs.edit().putInt(KEY_USER_ID, id).apply()
    }

    /**
     * Recupera el identificador del operario actual.
     *
     * @return ID numérico del usuario, o -1 indicando ausencia de sesión (Unauthenticated state).
     */
    fun fetchUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    /**
     * Purga irreversiblemente todos los artefactos de la sesión actual.
     * Debe ser invocado durante el flujo de Logout o tras un error HTTP 401 (Unauthorized)
     * para garantizar el borrado seguro de las credenciales en el dispositivo.
     */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }
}