package com.tynsolutions.gestionaveriasmovil.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityLoginBinding
import com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity

/**
 * Punto de entrada de la aplicación (Entry Point).
 * Implementa el patrón Passive View (Vista Pasiva): no contiene lógica de negocio,
 * delegando la validación al ViewModel y reaccionando a los cambios de estado.
 */
class LoginActivity : AppCompatActivity() {

    // Inicialización del ViewModel usando nuestro Factory personalizado para inyectar la capa de red.
    private lateinit var viewModel: LoginViewModel

    // ViewBinding para acceso seguro (Null-safe y Type-safe) a la jerarquía de vistas.
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitación de renderizado Edge-to-Edge para UI modernas.
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Manejo de WindowInsets para evitar solapamientos con las barras del sistema (Status/Navigation).
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 1. Inicialización del ViewModel con Factory ---
        val factory = LoginViewModel.LoginViewModelFactory(applicationContext)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // --- 2. Verificación de Persistencia de Sesión ---
        // Evaluamos si el usuario marcó "Recordar" Y si tenemos un token JWT criptográfico válido.
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        val quiereRecordarSesion = sharedPref.getBoolean("sesionGuardada", false)

        val sessionManager = SessionManager(applicationContext)
        val tokenExistente = sessionManager.fetchAuthToken()

        if (quiereRecordarSesion && !tokenExistente.isNullOrEmpty()) {
            // Bypass del flujo de autenticación si la sesión existe y es válida.
            navigateToMain(guardarSesion = false)
            return // Prevención de carga innecesaria de listeners/observers.
        } else if (!quiereRecordarSesion) {
            // Por seguridad, si no quiso recordar sesión, purgamos cualquier token residual.
            sessionManager.clearSession()
        }

        // Inicialización reactiva si requerimos autenticación manual.
        setupListeners()
        setupObservers()
    }

    /**
     * Mapea los eventos de la UI hacia intenciones del ViewModel.
     */
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Delegamos la responsabilidad de validación a la capa lógica.
            viewModel.validarLogin(email, password)
        }
    }

    /**
     * Establece las suscripciones (Observers) a los flujos de datos emitidos por el ViewModel.
     */
    private fun setupObservers() {
        // Suscripción al estado de carga (Latencia de red XAMPP/API)
        viewModel.cargando.observe(this) { isLoading ->
            // Bloqueamos la UI para evitar peticiones concurrentes y saturación del servidor
            binding.btnLogin.isEnabled = !isLoading
            binding.btnLogin.text = if (isLoading) "Conectando..." else "Acceder"

            // Si tienes un ProgressBar en el XML, puedes mostrarlo aquí:
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Suscripción a eventos de error para renderizar feedback (Toast/Snackbar).
        viewModel.mensajeError.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Suscripción al evento de éxito para disparar la navegación transversal.
        viewModel.loginExitoso.observe(this) { isSuccess ->
            if (isSuccess) {
                navigateToMain(guardarSesion = true)
            }
        }
    }

    /**
     * Enruta al usuario hacia la Host Activity principal.
     * Gestiona la persistencia condicional de la sesión y la limpieza del Backstack.
     *
     * @param guardarSesion Flag que indica si se debe actualizar el estado en SharedPreferences.
     */
    private fun navigateToMain(guardarSesion: Boolean) {

        // Persistencia de la decisión del usuario (Keep me logged in).
        // Nota: El Token JWT ya fue guardado en el SessionManager por el AuthRepository.
        if (guardarSesion && binding.cbRecordar.isChecked) {
            val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putBoolean("sesionGuardada", true)
                apply() // Ejecución asíncrona segura.
            }
        }

        // Configuración del Intent con flags de limpieza (Clear Top / New Task)
        // Evita que el usuario regrese a la pantalla de Login al presionar "Atrás".
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Destrucción explícita del contexto actual.
    }
}