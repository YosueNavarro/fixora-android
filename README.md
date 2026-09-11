# Fixora FieldTech - Android Client[cite: 4]

Aplicación móvil nativa en Android desarrollada para la gestión del mantenimiento correctivo de maquinaria industrial y el soporte operativo de técnicos de campo[cite: 4].

Este repositorio contiene exclusivamente la arquitectura del cliente móvil (Frontend)[cite: 4]. El sistema está diseñado para consumir una API RESTful, actuando como la interfaz móvil de un sistema de gestión empresarial centralizado[cite: 4].

## Arquitectura del Sistema

La aplicación implementa el patrón de arquitectura **Model-View-ViewModel (MVVM)** para garantizar la separación de responsabilidades, escalabilidad y facilidad de mantenimiento[cite: 4]:

*   **Capa de Presentación (View):** Compuesta por Activities y Fragments, utilizando layouts en XML y `RecyclerView` para el renderizado eficiente de listas dinámicas[cite: 4].
*   **Capa de Lógica (ViewModel):** Gestiona el estado de la interfaz y las operaciones en segundo plano, desacoplando la lógica de negocio del ciclo de vida de Android[cite: 4].
*   **Capa de Datos (Repository):** Centraliza el acceso a los datos, gestionando la comunicación con la capa de red y mapeando la información mediante objetos de transferencia de datos (DTOs)[cite: 4].

## Stack Tecnológico

*   **Lenguaje Base:** Kotlin[cite: 4]
*   **Construcción y Dependencias:** Gradle[cite: 4]
*   **Red y HTTP:** Retrofit, OkHttp[cite: 4]
*   **Autenticación:** JWT (JSON Web Tokens) transmitidos mediante cabeceras de autorización[cite: 4]
*   **Formatos de Datos:** JSON[cite: 4]

## Funcionalidades Principales

*   **Autenticación Segura:** Sistema de inicio de sesión basado en tokens (JWT) con persistencia de sesión local[cite: 4].
*   **Sincronización de Flujos:** Obtención de listados de averías asignadas segmentadas por estado de resolución (Nuevas, Recibidas, Finalizadas)[cite: 4].
*   **Trazabilidad Operativa:** Registro en texto libre de intervenciones técnicas e informes de mantenimiento[cite: 4].
*   **Actualización de Estados:** Modificación remota del estado operativo de la maquinaria (ej. *Operativa* o *Fuera de servicio*) con sincronización inmediata en la base de datos[cite: 4].

## Integración Backend

Este cliente móvil depende de una API REST independiente respaldada por una base de datos relacional MariaDB[cite: 4]. La infraestructura backend gestiona la persistencia de datos, los roles de usuario y la validación de las reglas de negocio[cite: 4].

> El código fuente del backend se mantiene de forma separada. Para consultar la implementación del servidor, revise el repositorio de la [API REST de Fixora](https://github.com/DAM-Nereida-Rodriguez-Orenes/API-REST-TYN-Solutions-PI).

## Documentación de Interfaz (UI)

*(Inserte aquí capturas de pantalla de la interfaz de usuario)*
*   `[Interfaz de Autenticación](docs/login.png)`
*   `[Panel de Averías Asignadas](docs/listado.png)`
*   `[Registro de Intervenciones](docs/intervencion.png)`