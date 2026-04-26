package com.example.fkaeh

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fkaeh.ui.theme.customPurple
import java.io.File
import androidx.compose.foundation.text.KeyboardOptions

private val ProfileCard = Color(0xD9101010)
private val ProfileBorder = Color(0xFF2A2A2A)
private val ProfileText = Color.White
private val ProfileMuted = Color(0xFFB7B7B7)
private val DialogContainer = Color(0xFF151515)

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onRequestNotificationPermission: () -> Unit,
    onLogout: () -> Unit
) {
    val user = vm.currentUser
    var openSection by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.updateProfilePhoto(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.26f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() } + 18.dp,
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 24.dp
                )
        ) {
            Text(
                text = vm.text("profile"),
                color = ProfileText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(14.dp))

            ProfileHeaderCard(
                name = user?.nombre ?: "Usuario",
                email = user?.correo.orEmpty(),
                photoPath = vm.profilePhotoPath,
                onPhotoClick = { photoPicker.launch("image/*") }
            )

            Spacer(Modifier.height(24.dp))

            SectionTitle("Mi cuenta")
            Spacer(Modifier.height(10.dp))

            ProfileMenuItem(
                icon = Icons.Outlined.Person,
                title = "Mis datos",
                expanded = openSection == "datos",
                onClick = { openSection = openSection.toggle("datos") }
            ) {
                MisDatosContent(vm, user)
            }

            ProfileMenuItem(
                icon = Icons.Outlined.FavoriteBorder,
                title = "Artículos Favoritos",
                expanded = openSection == "favoritos",
                onClick = { openSection = openSection.toggle("favoritos") }
            ) {
                FavoritosContent(vm)
            }

            ProfileMenuItem(
                icon = Icons.Outlined.ShoppingBag,
                title = "Mis compras",
                expanded = openSection == "compras",
                onClick = { openSection = openSection.toggle("compras") }
            ) {
                MisComprasContent(vm)
            }

            ProfileMenuItem(
                icon = Icons.Outlined.Person,
                title = "Direcciones guardadas",
                expanded = openSection == "direcciones",
                onClick = { openSection = openSection.toggle("direcciones") }
            ) {
                DireccionesContent(vm)
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Otros accesos")
            Spacer(Modifier.height(10.dp))

            ProfileMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Ajustes del dispositivo",
                expanded = openSection == "config",
                onClick = { openSection = openSection.toggle("config") }
            ) {
                ConfiguracionContent(vm, user, onRequestNotificationPermission, onLogout)
            }

            ProfileMenuItem(
                icon = Icons.Outlined.Email,
                title = "Newsletter",
                expanded = openSection == "newsletter",
                onClick = { openSection = openSection.toggle("newsletter") }
            ) {
                PlaceholderContent("Próximamente podrás gestionar tu suscripción")
            }

            ProfileMenuItem(
                icon = Icons.Outlined.Email,
                title = "Ayuda y contacto",
                expanded = openSection == "ayuda",
                onClick = { openSection = openSection.toggle("ayuda") }
            ) {
                PlaceholderContent("Escríbenos a soporte@fkaeh.com")
            }

            ProfileMenuItem(
                icon = Icons.Outlined.Info,
                title = "Información legal",
                expanded = openSection == "legal",
                onClick = { openSection = openSection.toggle("legal") }
            ) {
                PlaceholderContent("FKAEH · Términos, privacidad y uso de la plataforma")
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = vm.text("logout"),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    email: String,
    photoPath: String?,
    onPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(ProfileCard)
            .clickable { onPhotoClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(name = name, photoPath = photoPath, onClick = onPhotoClick)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = ProfileText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = email,
                    color = ProfileText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, photoPath: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(customPurple.copy(0.82f), Color(0xFF35146A))
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = ProfileText,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = ProfileText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ProfileCard)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = customPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = ProfileText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = ProfileText,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = ProfileBorder)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun MisDatosContent(vm: AppViewModel, user: UsuarioBD?) {
    if (user == null) {
        PlaceholderContent(vm.text("no_data"))
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F1F1F)),
            contentAlignment = Alignment.Center
        ) {
            if (vm.profilePhotoPath != null) {
                AsyncImage(
                    model = File(vm.profilePhotoPath!!),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(user.nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
        }

        DatoCentrado("Nombre", user.nombre)
        DatoCentrado("Correo", user.correo)
        DatoCentrado("Teléfono", user.telefono.ifBlank { "No indicado" })
    }
}

@Composable
private fun DatoCentrado(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            color = ProfileMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = ProfileText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MisComprasContent(vm: AppViewModel) {
    val historial = vm.historialCompras

    if (historial.isEmpty()) {
        PlaceholderContent(vm.text("no_purchases"))
        return
    }

    val totalGastado = historial.sumOf { it.producto.precio }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ResumenChip(label = vm.text("purchases"), value = historial.size.toString(), modifier = Modifier.weight(1f))
        ResumenChip(label = vm.text("spent_total"), value = "%.2f€".format(totalGastado), modifier = Modifier.weight(1f))
    }

    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        historial.forEach { item -> CompraHistorialItem(item) }
    }
}

@Composable
private fun FavoritosContent(vm: AppViewModel) {
    val context = LocalContext.current
    val favoritos = vm.productos.filter { it.esFavorito }

    if (favoritos.isEmpty()) {
        PlaceholderContent(vm.text("wishlist_empty"))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        favoritos.forEach { producto ->
            val fotoUrl = remember(producto.fotoUrls, producto.fotoUrl) {
                producto.fotoUrls.firstOrNull()?.let {
                    if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
                } ?: producto.fotoUrl?.let {
                    if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF171717))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF242424)),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(fotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = producto.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = producto.nombre.take(1).uppercase(),
                            color = customPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        color = ProfileText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (producto.nombreVendedor.isNotBlank()) {
                        Text(
                            text = "de ${producto.nombreVendedor}",
                            color = ProfileMuted,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "%.2f€".format(producto.precio),
                        color = customPurple,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = { vm.toggleFavorito(producto) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = customPurple)
                }
            }
        }
    }
}

@Composable
private fun DireccionesContent(vm: AppViewModel) {
    if (vm.direccionesGuardadas.isEmpty()) {
        PlaceholderContent("Todavía no tienes direcciones guardadas")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.direccionesGuardadas.forEach { direccion ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF171717))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(direccion.alias, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(direccion.nombreCompleto, color = Color(0xFFD4D4D4), fontSize = 13.sp)
                        Text(direccion.telefono, color = customPurple, fontSize = 12.sp)
                        Text(direccion.resumen(), color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { vm.eliminarDireccion(direccion) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfiguracionContent(
    vm: AppViewModel,
    user: UsuarioBD?,
    onRequestNotificationPermission: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var passwordActual by remember { mutableStateOf("") }
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var passwordEliminarCuenta by remember { mutableStateOf("") }
    var languageExpanded by remember { mutableStateOf(false) }
    var showDesactivarDialog by remember { mutableStateOf(false) }
    var showEliminarDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Email, contentDescription = null, tint = customPurple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = vm.text("email").uppercase(),
                    color = ProfileMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = user?.correo.orEmpty(), color = ProfileText, fontSize = 14.sp)
            }
        }

        Box {
            OutlinedTextField(
                value = vm.currentLanguage.label,
                onValueChange = {},
                readOnly = true,
                label = { Text(vm.text("language")) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { languageExpanded = true }) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = customPurple)
                    }
                }
            )
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = {
                            vm.setLanguage(language)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notificaciones", color = ProfileText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Avisos de carrito, favoritos y publicaciones",
                    color = ProfileMuted,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = vm.notificationsEnabled,
                onCheckedChange = { enabled ->
                    vm.updateNotificationsEnabled(enabled)
                    if (enabled) onRequestNotificationPermission()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = customPurple
                )
            )
        }

        Text("Cambiar contraseña", color = Color.White, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = passwordActual,
            onValueChange = { passwordActual = it },
            label = { Text("Contraseña actual") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = customPurple)
            }
        )

        OutlinedTextField(
            value = nuevaPassword,
            onValueChange = { nuevaPassword = it },
            label = { Text("Nueva contraseña") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = customPurple)
            }
        )

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmación de contraseña") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = customPurple)
            }
        )

        Button(
            onClick = {
                vm.cambiarMiPassword(passwordActual, nuevaPassword, confirmarPassword)
                passwordActual = ""
                nuevaPassword = ""
                confirmarPassword = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = customPurple),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("${vm.text("change_password")} · ${vm.text("save")}")
        }

        Spacer(Modifier.height(8.dp))
        Text("Cuenta", color = Color.White, fontWeight = FontWeight.Bold)

        Button(
            onClick = { showDesactivarDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Desactivar cuenta", color = Color.White)
        }

        Button(
            onClick = { showEliminarDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Eliminar cuenta", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showDesactivarDialog) {
        AlertDialog(
            onDismissRequest = { showDesactivarDialog = false },
            containerColor = DialogContainer,
            titleContentColor = ProfileText,
            textContentColor = ProfileMuted,
            confirmButton = {
                Button(
                    onClick = {
                        showDesactivarDialog = false
                        vm.desactivarMiCuenta { ok ->
                            if (ok) onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E))
                ) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDesactivarDialog = false },
                ) {
                    Text("Cancelar", color = ProfileMuted)
                }
            },
            title = { Text("Desactivar cuenta") },
            text = { Text("Podrás dejar de usar esta cuenta sin borrarla definitivamente.") }
        )
    }

    if (showEliminarDialog) {
        var eliminarPasswordVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                passwordEliminarCuenta = ""
                showEliminarDialog = false
            },
            containerColor = DialogContainer,
            titleContentColor = ProfileText,
            textContentColor = ProfileMuted,
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminarMiCuenta(passwordEliminarCuenta) { ok ->
                            if (ok) onLogout()
                            if (ok) {
                                passwordEliminarCuenta = ""
                                showEliminarDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        passwordEliminarCuenta = ""
                        showEliminarDialog = false
                    },
                ) {
                    Text("Cancelar", color = ProfileMuted)
                }
            },
            title = { Text("Eliminar cuenta", color = ProfileText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Esta acción intentará borrar tu cuenta de la base de datos y no se puede deshacer.",
                        color = ProfileMuted
                    )
                    OutlinedTextField(
                        value = passwordEliminarCuenta,
                        onValueChange = { passwordEliminarCuenta = it },
                        label = { Text("Contraseña actual") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = customPurple)
                                IconButton(onClick = { eliminarPasswordVisible = !eliminarPasswordVisible }) {
                                    Icon(
                                        imageVector = if (eliminarPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = ProfileMuted
                                    )
                                }
                            }
                        },
                        visualTransformation = if (eliminarPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = BlackFieldColors
                    )
                }
            }
        )
    }
}

@Composable
private fun ResumenChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(customPurple.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = customPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = ProfileMuted, fontSize = 10.sp)
    }
}

@Composable
private fun CompraHistorialItem(item: ItemCarrito) {
    val context = LocalContext.current
    val fotoUrl = remember(item.producto.fotoUrl, item.producto.fotoUrls) {
        item.producto.fotoUrls.firstOrNull()?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        } ?: item.producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF171717))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF232323)),
            contentAlignment = Alignment.Center
        ) {
            if (fotoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.producto.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(item.producto.nombre.take(1).uppercase(), color = customPurple, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.producto.nombre,
                color = ProfileText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "Comprado", color = ProfileMuted, fontSize = 11.sp)
        }

        Text(
            text = "%.2f€".format(item.producto.precio),
            color = customPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlaceholderContent(text: String) {
    Text(
        text = text,
        color = ProfileMuted,
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
}

private fun String?.toggle(target: String): String? = if (this == target) null else target
