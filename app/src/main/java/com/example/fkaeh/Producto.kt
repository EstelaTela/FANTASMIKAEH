package com.example.fkaeh

const val ESTADO_CENSURADO_ADMIN = "CENSURADO_ADMIN"

data class Producto(
    val id: Int,
    val idVendedor: Int,
    val idCategoria: Int? = null,
    val categoriaNombre: String = "",
    val nombre: String,
    val precio: Double,
    val descripcion: String = "",
    val estadoPrenda: String = "",
    val fotoUrl: String? = null,
    val fotoUrls: List<String> = fotoUrl?.let { listOf(it) } ?: emptyList(),
    val nombreVendedor: String = "",
    val esFavorito: Boolean = false
)
