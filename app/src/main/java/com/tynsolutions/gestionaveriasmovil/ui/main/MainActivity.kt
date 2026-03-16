package com.tynsolutions.gestionaveriasmovil.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityMainBinding
import com.tynsolutions.gestionaveriasmovil.ui.listado.ListadoAveriasFragment
import android.content.Context
import android.content.Intent

/**
 * Host Activity principal de la aplicación.
 * Actúa como contenedor raíz para la arquitectura Single-Activity o Multi-Fragment,
 * orquestando la navegación primaria y funciones globales como el cierre de sesión.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de la jerarquía de vistas
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevención de solapamiento de Fragmentos (Fragment Overlapping).
        // Solo inyectamos el fragmento inicial si el savedInstanceState es null
        // (lo que significa que la Activity se crea por primera vez, no por un cambio de configuración como rotar la pantalla).
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, ListadoAveriasFragment())
                .commit()
        }
    }

    /**
     * Invalida la sesión actual del usuario, purgando las preferencias locales
     * y retornando la aplicación a su estado desautenticado de forma segura.
     */
    fun cerrarSesion() {
        // 1. Invalidación de tokens/flags en el almacenamiento persistente.
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("sesionGuardada", false)
            apply() // Ejecución asíncrona recomendada frente a commit()
        }

        // 2. Construcción de la ruta de salida.
        val intent = Intent(this, com.tynsolutions.gestionaveriasmovil.ui.login.LoginActivity::class.java)

        // 3. Saneamiento del Backstack.
        // FLAG_ACTIVITY_CLEAR_TASK asegura que todas las activities previas sean destruidas.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // 4. Ejecución del enrutamiento y destrucción del Host actual.
        startActivity(intent)
        finish()
    }
}