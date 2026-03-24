package com.tynsolutions.gestionaveriasmovil.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityLoginBinding
import com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Entry Point de la aplicación.
 * Implementa una Vista Pasiva que delega la orquestación del acceso al ViewModel.
 * Gestiona la persistencia de sesión "Remember Me" y la navegación inicial.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Inyección de dependencias mediante el Factory definido en el ViewModel
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDiseñoEdgeToEdge()
        verificarSesionExistente()
        establecerInteracciones()
        vincularEstadoLogico()
    }

    /**
     * Ajusta el padding de la vista principal para respetar las barras del sistema.
     */
    private fun configurarDiseñoEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Implementa el bypass de autenticación (Auto-login).
     * Evalúa la persistencia del token y la preferencia del usuario antes de inflar el flujo manual.
     */
    private fun verificarSesionExistente() {
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        val recordado = sharedPref.getBoolean("sesionGuardada", false)
        val sessionManager = SessionManager(applicationContext)

        if (recordado && !sessionManager.fetchAuthToken().isNullOrEmpty()) {
            navegarAlMain(guardarPreferencia = false)
        } else if (!recordado) {
            // Purga de seguridad: Si el usuario no quiere ser recordado, limpiamos rastros previos.
            sessionManager.clearSession()
        }
    }

    /**
     * Define los disparadores de intención del usuario.
     */
    private fun establecerInteracciones() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            viewModel.intentarLogin(email, pass)
        }
    }

    /**
     * Suscripción al flujo de estado (UI State) del ViewModel.
     * Utiliza el patrón de recolección segura vinculado al ciclo de vida de la Activity.
     */
    private fun vincularEstadoLogico() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    manejarCambioEstado(estado)
                }
            }
        }
    }

    /**
     * Orquestador de cambios visuales basados en la respuesta del dominio.
     */
    private fun manejarCambioEstado(estado: LoginUiState) {
        when (estado) {
            is LoginUiState.Loading -> alternarModoCarga(true)
            is LoginUiState.Success -> {
                alternarModoCarga(false)
                navegarAlMain(guardarPreferencia = true)
            }
            is LoginUiState.Error -> {
                alternarModoCarga(false)
                Toast.makeText(this, estado.message, Toast.LENGTH_LONG).show()
                viewModel.resetEstado()
            }
            is LoginUiState.Idle -> alternarModoCarga(false)
        }
    }

    /**
     * Gestiona la interactividad de la UI durante procesos de red.
     */
    private fun alternarModoCarga(cargando: Boolean) {
        with(binding) {
            btnLogin.isEnabled = !cargando
            btnLogin.text = if (cargando) "Autenticando..." else "Acceder"
            // Opcional: pbLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        }
    }

    /**
     * Ejecuta la transición hacia la pantalla principal y destruye la pila de Login.
     */
    private fun navegarAlMain(guardarPreferencia: Boolean) {
        if (guardarPreferencia && binding.cbRecordar.isChecked) {
            getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE).edit()
                .putBoolean("sesionGuardada", true)
                .apply()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}