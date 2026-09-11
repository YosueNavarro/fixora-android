# Fixora FieldTech - Android Client

Aplicación móvil nativa en Android desarrollada para la gestión del mantenimiento correctivo de maquinaria industrial y el soporte operativo de técnicos de campo.

Este repositorio contiene exclusivamente la arquitectura del cliente móvil (Frontend). El sistema está diseñado para consumir una API RESTful, actuando como la interfaz móvil de un sistema de gestión empresarial centralizado.

## Arquitectura del Sistema

La aplicación implementa el patrón de arquitectura **Model-View-ViewModel (MVVM)** para garantizar la separación de responsabilidades, escalabilidad y facilidad de mantenimiento:

*   **Capa de Presentación (View):** Compuesta por Activities y Fragments, utilizando layouts en XML y `RecyclerView` para el renderizado eficiente de listas dinámicas.
*   **Capa de Lógica (ViewModel):** Gestiona el estado de la interfaz y las operaciones en segundo plano, desacoplando la lógica de negocio del ciclo de vida de Android.
*   **Capa de Datos (Repository):** Centraliza el acceso a los datos, gestionando la comunicación con la capa de red y mapeando la información mediante objetos de transferencia de datos (DTOs).

## Stack Tecnológico

*   **Lenguaje Base:** Kotlin
*   **Construcción y Dependencias:** Gradle
*   **Red y HTTP:** Retrofit, OkHttp
*   **Autenticación:** JWT (JSON Web Tokens) transmitidos mediante cabeceras de autorización
*   **Formatos de Datos:** JSON

## Funcionalidades Principales

*   **Autenticación Segura:** Sistema de inicio de sesión basado en tokens (JWT) con persistencia de sesión local.
*   **Sincronización de Flujos:** Obtención de listados de averías asignadas segmentadas por estado de resolución (Nuevas, Recibidas, Finalizadas).
*   **Trazabilidad Operativa:** Registro en texto libre de intervenciones técnicas e informes de mantenimiento.
*   **Actualización de Estados:** Modificación remota del estado operativo de la maquinaria (ej. *Operativa* o *Fuera de servicio*) con sincronización inmediata en la base de datos.

## Integración Backend

Este cliente móvil depende de una API REST independiente respaldada por una base de datos relacional MariaDB. La infraestructura backend gestiona la persistencia de datos, los roles de usuario y la validación de las reglas de negocio.

> El código fuente del backend se mantiene de forma separada. Para consultar la implementación del servidor, revise el repositorio de la [API REST de Fixora](https://github.com/DAM-Nereida-Rodriguez-Orenes/API-REST-TYN-Solutions-PI).

## Documentación de Interfaz (UI)

<img width="447" height="789" alt="imagen" src="https://github.com/user-attachments/assets/8d269511-f4e5-4279-b2c2-2bbde67c7951" />

<img width="463" height="730" alt="imagen" src="https://github.com/user-attachments/assets/d6f7eded-8b10-4c9a-a5f8-065db0b17759" />

<img width="496" height="806" alt="imagen" src="https://github.com/user-attachments/assets/23c09e5c-98be-4445-b62c-dffd8a2588f5" />
