package com.tynsolutions.gestionaveriasmovil.data.local

import com.tynsolutions.gestionaveriasmovil.domain.model.Averia

object FakeDataSource {
    val averias = listOf(
        // 1. Una avería "Nueva" (Tiene asignación, pero no aceptación)
        Averia(
            id = 1,
            titulo = "Fuga de presión hidráulica",
            descripcion = "El brazo articulado pierde fuerza al levantar carga máxima.",
            maquinaria = "Excavadora Volvo A40",
            estadoMaquinaria = "Averiada",
            fechaInforme = "09/03/2026",
            fechaAsignacion = "10/03/2026",
            fechaAceptacion = null,
            fechaFinalizacion = null
        ),
        // 2. Una avería "Recibida" (Ya tiene fecha de aceptación)
        Averia(
            id = 2,
            titulo = "Fallo de arranque",
            descripcion = "El motor hace ruido pero no llega a arrancar.",
            maquinaria = "Generador Himoinsa 100kVA",
            estadoMaquinaria = "Fuera de servicio",
            fechaInforme = "08/03/2026",
            fechaAsignacion = "08/03/2026",
            fechaAceptacion = "09/03/2026",
            fechaFinalizacion = null
        ),
        // 3. Otra avería "Nueva"
        Averia(
            id = 3,
            titulo = "Cambio de filtros",
            descripcion = "Mantenimiento preventivo. Toca cambio de filtro de aceite y aire.",
            maquinaria = "Pala Cargadora CAT 950",
            estadoMaquinaria = "En mantenimiento",
            fechaInforme = "11/03/2026",
            fechaAsignacion = "11/03/2026",
            fechaAceptacion = null,
            fechaFinalizacion = null
        )
    )
}