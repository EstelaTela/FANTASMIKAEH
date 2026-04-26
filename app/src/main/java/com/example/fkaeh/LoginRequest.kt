package com.example.fkaeh

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class LoginResponse(
    val success: Boolean,
    // 🔴 CORREGIDO: UsuriousBD -> UsuarioBD
    val usuario: UsuarioBD? = null,
    val error: String? = null
)
