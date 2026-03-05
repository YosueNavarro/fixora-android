package com.tynsolutions.gestionaveriasmovil.data.local

import com.tynsolutions.gestionaveriasmovil.domain.model.Averia
import com.tynsolutions.gestionaveriasmovil.domain.model.Usuario

object FakeDataSource {

    // El usuario que tenemos en LoginViewModel
    val usuarioSimulado = Usuario(
        email = "tecnico@taller.com",
        password = "1234",
        activo = true
    )

    // La lista de averías de prueba (para fase 1)
    val averias = listOf(
        Averia(
            id = 1,
            titulo = "Fuga de presión hidráulica",
            descripcion = "El brazo articulado pierde fuerza al levantar carga máxima.",
            estado = "Nueva",
            fechaAsignacion = "10/03/2026",
            maquinaria = "Excavadora Volvo A40"
        ),
        Averia(
            id = 2,
            titulo = "Fallo de arranque",
            descripcion = "El motor hace ruido pero no llega a arrancar. Posible fallo en el alternador o batería.",
            estado = "Recibida",
            fechaAsignacion = "09/03/2026",
            maquinaria = "Generador Himoinsa 100kVA"
        ),
        Averia(
            id = 3,
            titulo = "Cambio de filtros",
            descripcion = "Mantenimiento preventivo. Toca cambio de filtro de aceite y aire.",
            estado = "Nueva",
            fechaAsignacion = "11/03/2026",
            maquinaria = "Pala Cargadora CAT 950"
        )
    )
}