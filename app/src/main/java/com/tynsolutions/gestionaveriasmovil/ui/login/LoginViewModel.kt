package com.tynsolutions.gestionaveriasmovil.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tynsolutions.gestionaveriasmovil.data.network.ApiClient
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Jerarquía de estados inmutables para el flujo de autenticación.
 * Garantiza que la vista reaccione de forma determinista ante el éxito,
 * el fallo o la latencia de la red.
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * Orquestador de la lógica de presentación para el control de acceso.
 * Implementa el patrón Unidirectional Data Flow (UDF) para gestionar las credenciales
 * y asegurar la persistencia de la sesión mediante la capa de Dominio.
 */
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    // Encapsulamiento del estado: El estado interno es mutable, pero se expone como inmutable (Read-only).
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Valida y procesa la intención de acceso del usuario.
     * Implementa un patrón de validación temprana (Fail-fast) para minimizar
     * las peticiones innecesarias al servidor perimetral.
     *
     * @param email Correo electrónico sanitizado.
     * @param password Contraseña para verificación criptográfica en servidor.
     */
    fun intentarLogin(email: String, password: String) {
        // 1. Validación de integridad de entrada en capa de presentación
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Identidad y credenciales son obligatorias.")
            return
        }

        viewModelScope.launch {
            // 2. Transición a estado de bloqueo de UI (Indempotencia)
            _uiState.value = LoginUiState.Loading

            // 3. Delegación de la transacción de seguridad al repositorio de dominio
            val resultado = repository.realizarLogin(email, password)

            // 4. Mapeo del resultado de red a estados de interfaz
            resultado.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                },
                onFailure = { excepcion ->
                    _uiState.value = LoginUiState.Error(
                        excepcion.message ?: "Error de protocolo: Fallo en la comunicación con el servicio de identidad."
                    )
                }
            )
        }
    }

    /**
     * Resetea el estado para permitir nuevos intentos tras un error de validación.
     */
    fun resetEstado() {
        _uiState.value = LoginUiState.Idle
    }

    /**
     * Factory de inyección de dependencias.
     * Centraliza la construcción del grafo de objetos (Inversión de Control).
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                val sessionManager = SessionManager(context)
                val apiService = ApiClient.getApiService(sessionManager)
                val repository = AuthRepository(apiService, sessionManager)
                return LoginViewModel(repository) as T
            }
            throw IllegalArgumentException("Fallo en la resolución: Clase de ViewModel no registrada.")
        }
    }
}