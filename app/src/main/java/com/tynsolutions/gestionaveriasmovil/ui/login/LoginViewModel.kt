package com.tynsolutions.gestionaveriasmovil.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tynsolutions.gestionaveriasmovil.domain.model.Usuario

/**
 * Capa de presentación (ViewModel) encargada de procesar las reglas de negocio
 * del flujo de autenticación y exponer el estado reactivo a la Vista.
 */
class LoginViewModel : ViewModel() {

    // --- Backing Properties ---
    // Patrón arquitectónico para encapsular la mutabilidad del estado.
    // _loginExitoso permite lectura/escritura interna. loginExitoso expone solo lectura a la Vista.
    private val _loginExitoso = MutableLiveData<Boolean>()
    val loginExitoso: LiveData<Boolean> get() = _loginExitoso

    private val _mensajeError = MutableLiveData<String>()
    val mensajeError: LiveData<String> get() = _mensajeError

    /**
     * Valida las credenciales ingresadas aplicando reglas de negocio locales o remotas.
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

        // 2. Mocking de Data Source (Fase 1).
        // Nota técnica: En futuras iteraciones, esto será sustituido por un UseCase o Repository.
        val usuarioSimulado = Usuario(
            email = "tecnico@taller.com",
            password = "1234",
            activo = true
        )

        // 3. Evaluación de reglas de negocio cruzadas (Credenciales + Estado de la cuenta).
        if (emailInput == usuarioSimulado.email && passwordInput == usuarioSimulado.password) {

            if (!usuarioSimulado.activo) {
                // Notificación de estado denegado (Regla de negocio: cuenta inactiva).
                _mensajeError.value = "Usuario inactivo"
            } else {
                // Emisión de evento de éxito.
                _loginExitoso.value = true
            }

        } else {
            // Emisión de evento de fallo de autenticación.
            _mensajeError.value = "Usuario o contraseña incorrectos"
        }
    }
}