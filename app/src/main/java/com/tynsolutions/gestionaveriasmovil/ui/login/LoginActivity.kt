package com.tynsolutions.gestionaveriasmovil.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityLoginBinding
import com.tynsolutions.gestionaveriasmovil.ui.main.MainActivity

/**
 * Punto de entrada de la aplicación (Entry Point).
 * Implementa el patrón Passive View (Vista Pasiva): no contiene lógica de negocio,
 * delegando la validación al ViewModel y reaccionando a los cambios de estado.
 */
class LoginActivity : AppCompatActivity() {

    // Inicialización Lazy del ViewModel ligado al ciclo de vida de la Activity.
    private val viewModel: LoginViewModel by viewModels()

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

        // --- Verificación de Persistencia de Sesión ---
        // Accedemos al almacenamiento local cifrado (o plano vía SharedPreferences)
        // para evaluar si existe un token o flag de sesión activa.
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        val estaLogueado = sharedPref.getBoolean("sesionGuardada", false)

        if (estaLogueado) {
            // Bypass del flujo de autenticación si la sesión ya existe.
            navigateToMain(guardarSesion = false)
            return // Prevención de carga innecesaria de listeners/observers.
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
        // Suscripción a eventos de error para renderizar feedback (Toast/Snackbar).
        viewModel.mensajeError.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
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