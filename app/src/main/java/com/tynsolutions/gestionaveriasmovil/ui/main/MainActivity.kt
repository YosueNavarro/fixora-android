package com.tynsolutions.gestionaveriasmovil.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.data.network.SessionManager
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityMainBinding
import com.tynsolutions.gestionaveriasmovil.ui.listado.ListadoAveriasFragment
import com.tynsolutions.gestionaveriasmovil.ui.login.LoginActivity

/**
 * Host Activity principal (Root Container).
 * Implementa la arquitectura Single-Activity para la orquestación de fragmentos,
 * centralizando el control del ciclo de vida de la sesión y la navegación transversal.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val PREFS_NAME = "PrefsTaller"
        private const val KEY_REMEMBER = "sesionGuardada"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitación de renderizado inmersivo para consistencia con el Login
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDiseñoInmersivo()
        inicializarFragmentoPrincipal(savedInstanceState)
    }

    /**
     * Ajusta los márgenes internos para evitar colisiones con la barra de estado y navegación.
     */
    private fun configurarDiseñoInmersivo() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Inyecta el punto de entrada visual (Listado) garantizando la persistencia
     * ante cambios de configuración (Fragment Overlapping Protection).
     */
    private fun inicializarFragmentoPrincipal(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, ListadoAveriasFragment())
                .commit()
        }
    }

    /**
     * Ejecuta el protocolo de desautenticación global.
     * Realiza una purga multinivel: borra el flag de "Recordar", destruye el token JWT
     * en el [SessionManager] y sanea la pila de actividades (Backstack).
     */
    fun cerrarSesion() {
        // 1. Invalida la preferencia de auto-login
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(KEY_REMEMBER, false).apply()

        // 2. Seguridad Crítica: Borra el token JWT y el ID del técnico
        val sessionManager = SessionManager(this)
        sessionManager.clearSession()

        // 3. Redirección táctica al punto de entrada seguro (Login)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish() // Destrucción del contexto principal
    }
}