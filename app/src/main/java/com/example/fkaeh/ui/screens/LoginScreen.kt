package com.example.fkaeh.ui.screens

import com.example.fkaeh.R
import com.example.fkaeh.AppViewModel
import com.example.fkaeh.core.*
import com.example.fkaeh.data.models.*
import com.example.fkaeh.data.repository.*
import com.example.fkaeh.ui.common.*
import com.example.fkaeh.ui.components.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun LoginScreen(
    vm: AppViewModel,
    onLoginSuccess: () -> Unit,
    onIrARegistro: () -> Unit = {}
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRecuperar   by remember { mutableStateOf(false) }

    if (vm.isLoggedIn) { LaunchedEffect(Unit) { onLoginSuccess() } }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 120.dp),      // ← padding arriba para el texto del fondo
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; vm.limpiarErrorLogin() },
                placeholder = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = vm.loginError != null,
                shape = RoundedCornerShape(50.dp),
                colors = WhiteFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; vm.limpiarErrorLogin() },
                placeholder = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                isError = vm.loginError != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                shape = RoundedCornerShape(50.dp),
                colors = WhiteFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )

            if (vm.loginError != null) {
                Text(
                    text = vm.loginError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // "¿Olvidaste tu contraseña?" con sombra negra
            TextButton(
                onClick = { showRecuperar = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "¿Olvidaste tu contraseña?",
                    color = Color.White,
                    fontSize = 12.sp,
                    style = shadowTextStyle
                )
            }

            Spacer(Modifier.height(8.dp))

            if (vm.isLoading) {
                CircularProgressIndicator(color = Color(0xFF9C27B0), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
            }

            // Botón estilo Sandra: negro con borde blanco, pill shape
            Button(
                onClick = { vm.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor   = Color.White
                ),
                border = BorderStroke(2.dp, Color.White),
                shape  = RoundedCornerShape(50.dp),
            ) {
                Text("Iniciar sesión", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(12.dp))

            // "¿No tienes cuenta?" con sombra negra
            TextButton(onClick = { onIrARegistro() }) {
                Text(
                    "¿No tienes cuenta? Regístrate aquí",
                    color = Color.White,
                    fontSize = 13.sp,
                    style = shadowTextStyle
                )
            }
        }
    }

    if (showRecuperar) {
        RecuperarPasswordDialog(
            vm = vm,
            onDismiss = { showRecuperar = false }
        )
    }

    if (vm.showReactivarCuentaDialog) {
        ReactivarCuentaDialog(vm = vm)
    }
}

@Composable
private fun ReactivarCuentaDialog(vm: AppViewModel) {
    var codigoEnviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!cargando) vm.cerrarDialogoReactivacion() },
        containerColor = Color(0xFF151515),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD4D4D4),
        title = {
            Text(
                if (codigoEnviado) "Verifica tu correo" else "Cuenta desactivada",
                fontWeight = FontWeight.Bold
            )

        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (codigoEnviado) {
                        "Introduce el código de 6 dígitos que te hemos enviado para activar tu cuenta de nuevo. \n"+
                        "\nCódigo demo: 123456"
                    } else {
                        "¿Desea activar tu cuenta de nuevo?"
                    },
                    color = Color(0xFFD4D4D4),
                    fontSize = 14.sp
                )
                if (codigoEnviado) {
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = {
                            codigo = it.filter { ch -> ch.isDigit() }.take(6)
                            error = ""
                        },
                        placeholder = { Text("Código") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = WhiteFieldColors
                    )
                }
                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF8A8A), fontSize = 12.sp)
                }
                if (cargando) {
                    CircularProgressIndicator(color = Purple, modifier = Modifier.size(28.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    cargando = true
                    error = ""
                    if (!codigoEnviado) {
                        vm.solicitarCodigoReactivacion { exito, mensaje ->
                            cargando = false
                            if (exito) {
                                codigoEnviado = true
                            } else {
                                error = mensaje ?: "No se pudo enviar el código"
                            }
                        }
                    } else {
                        if (codigo.length != 6) {
                            cargando = false
                            error = "Introduce el código de 6 dígitos"
                            return@Button
                        }
                        vm.reactivarCuentaConCodigo(codigo) { exito, mensaje ->
                            cargando = false
                            if (!exito) {
                                error = mensaje ?: "No se pudo activar la cuenta"
                            }
                        }
                    }
                },
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (codigoEnviado) "Activar" else "Sí")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { vm.cerrarDialogoReactivacion() },
                enabled = !cargando
            ) {
                Text("Cancelar", color = Color(0xFFBDBDBD))
            }
        }
    )
}

// ── Diálogo recuperar contraseña ──────────────────────────────
@Composable
fun RecuperarPasswordDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var paso             by remember { mutableStateOf("VERIFICAR") }
    var email            by remember { mutableStateOf("") }
    var telefono         by remember { mutableStateOf("") }
    var codigo           by remember { mutableStateOf("") }
    var nuevaPassword    by remember { mutableStateOf("") }
    var confirmaPassword by remember { mutableStateOf("") }
    var passVisible      by remember { mutableStateOf(false) }
    var confirmaVisible  by remember { mutableStateOf(false) }
    var error            by remember { mutableStateOf("") }
    var cargando         by remember { mutableStateOf(false) }

    val purpleColor = Color(0xFF8C52FF)

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor     = Color.White,
        unfocusedContainerColor   = Color.White,
        focusedIndicatorColor     = Color.Transparent,
        unfocusedIndicatorColor   = Color.Transparent,
        focusedTextColor          = Color.Black,
        unfocusedTextColor        = Color.Black,
        focusedPlaceholderColor   = Color.Gray,
        unfocusedPlaceholderColor = Color.Gray
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Paso 1: Verificar identidad ───────────────────────
                if (paso == "VERIFICAR") {
                    Text("Recuperar contraseña",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Introduce tu correo y teléfono. Te enviaremos un código de verificación.",
                        color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = "" },
                        placeholder = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it; error = "" },
                        placeholder = { Text("Número de teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    if (error.isNotBlank()) {
                        Text(error, color = Color.Red, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    if (cargando) {
                        CircularProgressIndicator(color = purpleColor, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || telefono.isBlank()) {
                                    error = "Rellena todos los campos"; return@Button
                                }
                                cargando = true
                                vm.solicitarCodigoRecuperacion(email, telefono.filter { it.isDigit() }) { exito, mensaje ->
                                    cargando = false
                                    if (exito) {
                                        paso = "NUEVA_PASSWORD"
                                        error = ""
                                    } else {
                                        error = mensaje ?: "No existe ninguna cuenta con esos datos"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            shape  = RoundedCornerShape(12.dp)
                        ) { Text("Verificar", color = Color.White) }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray, fontSize = 13.sp)
                    }
                }

                // ── Paso 2: Nueva contraseña ──────────────────────────
                if (paso == "NUEVA_PASSWORD") {
                    Text("Nueva contraseña",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Introduce el código recibido y tu nueva contraseña.",
                        color = Color(0xFF4CAF50), fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it.filter { ch -> ch.isDigit() }.take(6); error = "" },
                        placeholder = { Text("Código de 6 dígitos") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nuevaPassword,
                        onValueChange = { nuevaPassword = it; error = "" },
                        placeholder = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    null, tint = Color.Gray)
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmaPassword,
                        onValueChange = { confirmaPassword = it; error = "" },
                        placeholder = { Text("Confirmar contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true,
                        visualTransformation = if (confirmaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmaVisible = !confirmaVisible }) {
                                Icon(if (confirmaVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    null, tint = Color.Gray)
                            }
                        }
                    )

                    if (error.isNotBlank()) {
                        Text(error, color = Color.Red, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    if (cargando) {
                        CircularProgressIndicator(color = Purple, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Button(
                            onClick = {
                                when {
                                    codigo.length != 6 -> error = "Introduce el código de 6 dígitos"
                                    nuevaPassword.isBlank() -> error = "Introduce una contraseña"
                                    nuevaPassword.length < 4 -> error = "Mínimo 4 caracteres"
                                    nuevaPassword != confirmaPassword -> error = "Las contraseñas no coinciden"
                                    else -> {
                                        cargando = true
                                        vm.restablecerPasswordConCodigo(email, codigo, nuevaPassword) { exito, mensaje ->
                                            cargando = false
                                            if (exito) paso = "EXITO"
                                            else error = mensaje ?: "Error al guardar. Intenta de nuevo"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            shape  = RoundedCornerShape(12.dp)
                        ) { Text("Guardar contraseña", color = Color.White) }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray, fontSize = 13.sp)
                    }
                }

                // ── Paso 3: Éxito ─────────────────────────────────────
                if (paso == "EXITO") {
                    Spacer(Modifier.height(8.dp))
                    Text("✓", fontSize = 40.sp, color = Color(0xFF4CAF50))
                    Spacer(Modifier.height(12.dp))
                    Text("¡Contraseña actualizada!",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Ya puedes iniciar sesión con tu nueva contraseña.",
                        color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple),
                        shape  = RoundedCornerShape(12.dp)
                    ) { Text("Aceptar", color = Color.White) }
                }
            }
        }
    }
}
