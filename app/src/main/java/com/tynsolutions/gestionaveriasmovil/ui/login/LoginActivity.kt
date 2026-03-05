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

class LoginActivity : AppCompatActivity() {

    // Instancia del ViewModel delegada al ciclo de vida
    private val viewModel: LoginViewModel by viewModels()

    // Declaración del ViewBinding
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Habilitar la visualización de borde a borde
        enableEdgeToEdge()

        // 2. Inflar la vista usando ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Aplicar los Insets usando ViewBinding
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Comprobar sesión guardada
        // Abrimos nuestra "libreta" interna de Android
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        val estaLogueado = sharedPref.getBoolean("sesionGuardada", false)

        if (estaLogueado) {
            // Si ya estaba logueado, saltamos al Main directamente sin pedir datos
            navigateToMain(guardarSesion = false)
            return // Cortamos la ejecución del onCreate aquí para que no cargue la vista de Login
        }

        // 5. Configurar la lógica de la vista si no hay sesión guardada
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Al pulsar el botón Entrar
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            viewModel.validarLogin(email, password)
        }
    }

    private fun setupObservers() {
        // Observamos los errores para mostrar un Toast
        viewModel.mensajeError.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        // Observamos el éxito para navegar a la MainActivity
        viewModel.loginExitoso.observe(this) { isSuccess ->
            if (isSuccess) {
                // Aquí llamamos a la navegación (que ahora se encarga de guardar si hace falta)
                navigateToMain(guardarSesion = true)
            }
        }
    }

    // Le añadimos un parámetro para saber si venimos de un inicio de sesión nuevo o uno recordado
    private fun navigateToMain(guardarSesion: Boolean) {

        // Guardar sesión si el CheckBox está marcado
        if (guardarSesion && binding.cbRecordar.isChecked) {
            val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putBoolean("sesionGuardada", true)
                apply() // Guarda en segundo plano de forma segura
            }
        }

        // Navegación limpia hacia la pantalla principal
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Destruimos el Login
    }
}