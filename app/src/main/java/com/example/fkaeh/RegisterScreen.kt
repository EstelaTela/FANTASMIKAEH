package com.example.fkaeh

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fkaeh.ui.theme.customPurple

@Composable
fun RegisterScreen(
    vm: AppViewModel,
    onRegistroSuccess: () -> Unit,
    onIrALogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var aceptaTerminos by remember { mutableStateOf(false) }
    val telefonoValido = telefono.filter { it.isDigit() }.length >= 9

    if (vm.isLoggedIn) { LaunchedEffect(Unit) { onRegistroSuccess() } }

    Box(modifier = Modifier.fillMaxSize().background(BlackBg)) {
        Image(
            painter = painterResource(id = R.drawable.register),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentScale = ContentScale.FillBounds
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 28.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; vm.limpiarErrorRegistro() },
                placeholder = { Text("Nombre") },
                leadingIcon = { Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = DarkFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it; vm.limpiarErrorRegistro() },
                placeholder = { Text("Email") },
                leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = DarkFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = telefono,
                onValueChange = {
                    telefono = it.filter { ch -> ch.isDigit() }.take(15)
                    vm.limpiarErrorRegistro()
                },
                placeholder = { Text("Número de teléfono") },
                leadingIcon = { Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = DarkFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )
            Spacer(Modifier.height(10.dp))

            if (telefono.isNotBlank() && !telefonoValido) {
                Text("Introduce al menos 9 dígitos", color = Color.Red, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it; vm.limpiarErrorRegistro() },
                placeholder = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = DarkFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmarContrasena,
                onValueChange = { confirmarContrasena = it; vm.limpiarErrorRegistro() },
                placeholder = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = confirmarContrasena.isNotBlank() && contrasena != confirmarContrasena,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = DarkFieldColors,
                singleLine = true,
                enabled = !vm.isLoading
            )

            Spacer(Modifier.height(10.dp))

            if (confirmarContrasena.isNotBlank() && contrasena != confirmarContrasena) {
                Spacer(Modifier.height(4.dp))
                Text("Las contraseñas no coinciden", color = Color.Red, fontSize = 12.sp)
            }
            if (vm.registroError != null) {
                Spacer(Modifier.height(4.dp))
                Text(vm.registroError!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = { aceptaTerminos = it },
                    colors = CheckboxDefaults.colors(checkedColor = customPurple, uncheckedColor = Color.White),
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontSize = 11.sp)) { append("He leído y acepto los ") }
                        withStyle(SpanStyle(color = Color.White, fontSize = 11.sp, textDecoration = TextDecoration.Underline)) { append("Condiciones de uso y compra") }
                        withStyle(SpanStyle(color = Color.White, fontSize = 11.sp)) { append(" y el tratamiento de mis datos personales explicado en la ") }
                        withStyle(SpanStyle(color = Color.White, fontSize = 11.sp, textDecoration = TextDecoration.Underline)) { append("Política de Privacidad.") }
                    },
                    lineHeight = 15.sp,
                    style = shadowTextStyle
                )
            }

            Spacer(Modifier.height(24.dp))

            if (vm.isLoading) {
                CircularProgressIndicator(color = Purple, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val nombreFinal = nombre.ifBlank { correo.substringBefore("@") }
                    if (contrasena == confirmarContrasena) {
                        vm.registrar(nombreFinal, correo, contrasena, telefono)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                enabled = !vm.isLoading &&
                    correo.isNotBlank() &&
                    telefonoValido &&
                    contrasena.isNotBlank() &&
                    contrasena == confirmarContrasena &&
                    aceptaTerminos
            ) {
                Text("Regístrate", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "¿Ya tienes una cuenta? Inicia sesión aquí",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onIrALogin() }
            )

            Spacer(Modifier.height(36.dp))
        }
    }
}
