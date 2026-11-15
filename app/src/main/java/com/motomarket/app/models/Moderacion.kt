package com.motomarket.app.models

data class Moderacion(
    val id: String,
    val nombreProducto: String,
    val vendedor: String,
    val estado: EstadoProducto,
    val fecha: String = "",
    val motivo: String = ""
)
