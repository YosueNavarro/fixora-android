package com.tynsolutions.gestionaveriasmovil.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tynsolutions.gestionaveriasmovil.R
import com.tynsolutions.gestionaveriasmovil.databinding.ActivityMainBinding
import com.tynsolutions.gestionaveriasmovil.ui.listado.ListadoAveriasFragment // Asegúrate de que esta clase exista, aunque esté vacía
import android.content.Context
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflamos la vista con ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargamos el Fragmento del listado solo la primera vez (evita duplicados al girar la pantalla)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, ListadoAveriasFragment())
                .commit()
        }
    }

    fun cerrarSesion() {
        // 1. Abrimos la libreta y borramos el dato de la sesión
        val sharedPref = getSharedPreferences("PrefsTaller", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("sesionGuardada", false)
            apply() // Guardamos los cambios
        }

        // 2. Preparamos el viaje de vuelta al LoginActivity
        val intent = Intent(this, com.tynsolutions.gestionaveriasmovil.ui.login.LoginActivity::class.java)
        // 3. Limpiamos la pila de pantallas para que no pueda volver atrás dándole al botón del móvil
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // 4. Saltamos y destruimos el Main
        startActivity(intent)
        finish()
    }
}