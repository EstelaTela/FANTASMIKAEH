package com.example.fkaeh

// Data class para el carrito (puedes añadirlo en el mismo archivo que Producto)
data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int = 1
)
