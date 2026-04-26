package com.example.fkaeh

data class UsuarioBD(
    val id_usuario: Int,
    val nombre: String,
    val correo: String,
    val telefono: String = "",
    val activo: Boolean? = null,
    val id_rol: Int,
    val nombre_rol: String? = null
)
