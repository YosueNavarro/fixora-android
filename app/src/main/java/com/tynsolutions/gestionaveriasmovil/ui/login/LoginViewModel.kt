package com.tynsolutions.gestionaveriasmovil.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.repository.AuthRepository
import kotlinx.coroutines.launch
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager

/**
 * Capa de presentación (ViewModel) encargada de procesar las reglas de negocio
 * del flujo de autenticación y exponer el estado reactivo a la Vista.
 */
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    // --- Backing Properties ---
    // Patrón arquitectónico para encapsular la mutabilidad del estado.
    // _loginExitoso permite lectura/escritura interna. loginExitoso expone solo lectura a la Vista.
    private val _loginExitoso = MutableLiveData<Boolean>()
    val loginExitoso: LiveData<Boolean> get() = _loginExitoso

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    // Nuevo estado reactivo para gestionar el feedback visual durante la latencia de red.
    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> get() = _cargando

    /**
     * Valida las credenciales ingresadas aplicando reglas de negocio locales y
     * delegando la autenticación remota al repositorio.
     *
     * @param emailInput Correo electrónico ingresado por el usuario.
     * @param passwordInput Contraseña ingresada por el usuario.
     */
    fun validarLogin(emailInput: String, passwordInput: String) {
        // 1. Sanitización y validación de capa de vista (Early return pattern).
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            _mensajeError.value = "Por favor, rellena todos los campos"
            return
        }

        // 2. Transición a estado de carga. La UI debe bloquear interacciones repetidas.
        _cargando.value = true

        // 3. Delegación al Repositorio (Fase 2 - Conexión real a la API).
        // viewModelScope garantiza que la corrutina se cancele automáticamente si el ViewModel se destruye,
        // previniendo fugas de memoria (Memory Leaks) y crashes por respuestas tardías.
        viewModelScope.launch {
            val resultado = repository.realizarLogin(emailInput, passwordInput)

            // 4. Evaluación del resultado devuelto por la capa de red y seguridad.
            resultado.fold(
                onSuccess = {
                    // El SessionManager ya ha almacenado el token JWT de forma segura.
                    _cargando.value = false
                    _loginExitoso.value = true
                },
                onFailure = { excepcion ->
                    // Emisión de evento de fallo con el detalle proporcionado por el servidor o la red.
                    _cargando.value = false
                    _mensajeError.value = excepcion.message ?: "Error desconocido al contactar con el servidor"
                }
            )
        }
    }

    /**
     * Patrón Factory para inyectar las dependencias de red y seguridad
     * en el LoginViewModel en el momento de su creación.
     */
    class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                // Ensamblamos la cadena de dependencias
                val sessionManager = SessionManager(context)
                val apiService = ApiClient.getApiService(sessionManager)
                val repository = AuthRepository(apiService, sessionManager)

                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida")
        }
    }
}