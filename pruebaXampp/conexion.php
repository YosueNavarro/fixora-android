<?php

/* 
* Este archivo se debe introducir en el directorio .../Xampp/htdocs/pruebaXampp/
* El directorio Xampp es el de la instalación de Xampp, normalmente suele estar en C:/
* El directorio pruebaXampp se debe crear manualmente
*/

// 1. Configurar la conexión
$servidor = "localhost";
$averia = "root"; // Usuario por defecto de XAMPP
$password = "";    // Contraseña por defecto (vacía)
$base_datos = "gestiondeaveriatynsolution";

$conexion = new mysqli($servidor, $averia, $password, $base_datos);

// Verificar si hay error
if ($conexion->connect_error) {
    die("Conexión fallida: " . $conexion->connect_error);
}

// 2. Hacer la consulta
$sql = "SELECT codigoAveria, descInicAveria FROM averia";
$resultado = $conexion->query($sql);

$averias = array();

// 3. Recorrer los resultados y guardarlos en un arreglo
if ($resultado->num_rows > 0) {
    while($fila = $resultado->fetch_assoc()) {
        $averias[] = $fila;
    }
}

// 4. Devolver los datos en formato JSON
header('Content-Type: application/json; charset=utf-8');
echo json_encode($averias);

$conexion->close();
?>