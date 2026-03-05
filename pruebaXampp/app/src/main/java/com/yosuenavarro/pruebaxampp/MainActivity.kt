package com.yosuenavarro.pruebaxampp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        val btnObtenerDatos = findViewById<Button>(R.id.btnObtenerDatos)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        btnObtenerDatos.setOnClickListener {
            obtenerDatosDesdeXAMPP(tvResultado)
        }
    }
    private fun obtenerDatosDesdeXAMPP(tvResultado: TextView) {
        // IMPORTANTE: 10.0.2.2 es el "localhost" para el Emulador de Android.
        // Si usas un celular físico conectado por cable o WiFi, cambia esto por la IP de tu PC (ej: 192.168.1.5)
        val url = "http://10.0.2.2/pruebaXampp/conexion.php"

        // Crear la cola de peticiones de Volley
        val queue = Volley.newRequestQueue(this)

        // Crear la petición JSON
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                // Si la conexión es exitosa, procesamos el JSON
                var textoMostrar = ""

                for (i in 0 until response.length()) {
                    // Le llamamos "averia" a la variable para que tenga más sentido
                    val averia = response.getJSONObject(i)

                    // Usamos los nombres EXACTOS que pusiste en tu SELECT de PHP
                    // (Usamos optString por seguridad, como te comenté antes)
                    val codigo = averia.optString("codigoAveria", "Sin código")
                    val descripcion = averia.optString("descInicAveria", "Sin descripción")

                    textoMostrar += "Código: $codigo \nDescripción: $descripcion\n\n"
                }

                // Mostrar en pantalla
                tvResultado.text = textoMostrar
            },
            { error ->
                // Si hay un error, lo mostramos
                tvResultado.text = "Error de conexión: ${error.message}"
                Toast.makeText(this, "Asegúrate de que XAMPP esté encendido", Toast.LENGTH_LONG).show()
            }
        )

        // Añadir la petición a la cola para que se ejecute
        queue.add(jsonArrayRequest)
    }
}