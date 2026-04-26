package com.example.fkaeh

data class ProductoBD(
    val idProducto: Int,
    val idVendedor: Int,
    val idCategoria: Int?,
    val nombre: String,
    val descripcion: String?,
    val precioBase: Double,
    val estadoPrenda: String?,
    val fechaPublicacion: String,
    val fotoPrincipal: String? = null,
    val fotos: List<String> = fotoPrincipal?.let { listOf(it) } ?: emptyList(),
    val nombreVendedor: String = ""
) {
    fun toProductoApp(esFavorito: Boolean = false): Producto {
        return Producto(
            id = idProducto,
            idVendedor = idVendedor,
            idCategoria = idCategoria,
            categoriaNombre = CategoryCatalog.nameFor(idCategoria).orEmpty(),
            nombre = nombre,
            precio = precioBase,
            descripcion = descripcion ?: "",
            estadoPrenda = estadoPrenda.orEmpty(),
            fotoUrl = fotoPrincipal,
            fotoUrls = fotos,
            nombreVendedor = nombreVendedor,
            esFavorito = esFavorito
        )
    }
}
