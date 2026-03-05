package com.tynsolutions.gestionaveriasmovil.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.domain.model.Usuario

class LoginViewModel : ViewModel() {

    // 1. Encapsulación del estado de éxito (Backing Property)
    // _loginExitoso es privado y mutable (solo el ViewModel puede cambiarlo)
    private val _loginExitoso = MutableLiveData<Boolean>()

    // loginExitoso es público e inmutable (la Activity solo puede observarlo)
    val loginExitoso: LiveData<Boolean> get() = _loginExitoso

    // 2. Encapsulación de los mensajes de error
    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    fun validarLogin(emailInput: String, passwordInput: String) {
        // Validaciones previas de seguridad/UX
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            _mensajeError.value = "Por favor, rellena todos los campos"
            return
        }

        // 3. Lógica local simulada (Fase 1)
        // Simulamos el usuario basándonos en la tabla de la base de datos
        val usuarioSimulado = Usuario(
            email = "tecnico@taller.com",
            password = "1234",
            activo = true
        )

        // 4. Lógica de negocio y validación de credenciales
        if (emailInput == usuarioSimulado.email && passwordInput == usuarioSimulado.password) {

            // Verificamos si el técnico está activo
            if (!usuarioSimulado.activo) {
                // El sistema deniega el acceso
                _mensajeError.value = "Usuario inactivo"
            } else {
                // Todo correcto, permitimos el acceso
                _loginExitoso.value = true
            }

        } else {
            // Las credenciales no coinciden
            _mensajeError.value = "Usuario o contraseña incorrectos"
        }
    }
}