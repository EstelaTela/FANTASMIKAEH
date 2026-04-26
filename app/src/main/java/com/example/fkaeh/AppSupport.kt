package com.example.fkaeh

enum class AppLanguage(val code: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ES
    }
}

object AppText {
    private val es = mapOf(
        "language" to "Idioma",
        "password_updated" to "Contraseña actualizada",
        "password_error" to "No se pudo actualizar la contraseña",
        "cart" to "Carrito",
        "cart_empty" to "Tu carrito está vacío",
        "add_from_home" to "Añade productos desde inicio",
        "order_summary" to "RESUMEN DEL PEDIDO",
        "total" to "Total",
        "checkout" to "Finalizar compra",
        "profile" to "Perfil",
        "logout" to "Cerrar sesión",
        "no_data" to "No hay datos disponibles",
        "name" to "Nombre",
        "email" to "Correo",
        "no_purchases" to "Todavía no has comprado nada",
        "purchases" to "Compras",
        "spent_total" to "Gastado",
        "wishlist_empty" to "No tienes favoritos guardados",
        "new_password" to "Nueva contraseña",
        "change_password" to "Cambiar contraseña",
        "save" to "Guardar"
    )

    fun get(language: AppLanguage, key: String): String {
        return when (language) {
            AppLanguage.ES -> es[key]
            AppLanguage.EN -> null
        } ?: es[key] ?: key
    }
}

data class CategoryOption(val id: Int, val name: String)

object CategoryCatalog {
    private val fallbackOptions = listOf(
        CategoryOption(1, "Camisetas"),
        CategoryOption(2, "Pantalones"),
        CategoryOption(3, "Sudaderas"),
        CategoryOption(4, "Vestidos"),
        CategoryOption(5, "Chaquetas"),
        CategoryOption(6, "Zapatos"),
        CategoryOption(7, "Accesorios")
    )

    private var currentOptions: List<CategoryOption> = fallbackOptions

    val options: List<CategoryOption>
        get() = currentOptions

    fun updateOptions(newOptions: List<CategoryOption>) {
        if (newOptions.isNotEmpty()) currentOptions = newOptions
    }

    fun idFor(name: String): Int? = currentOptions.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id

    fun nameFor(id: Int?): String? = currentOptions.firstOrNull { it.id == id }?.name
}

data class DireccionGuardada(
    val alias: String,
    val nombreCompleto: String,
    val telefono: String,
    val direccion: String,
    val ciudad: String,
    val codigoPostal: String,
    val provincia: String
) {
    fun resumen(): String = listOf(direccion, codigoPostal, ciudad, provincia)
        .filter { it.isNotBlank() }
        .joinToString(", ")
}
