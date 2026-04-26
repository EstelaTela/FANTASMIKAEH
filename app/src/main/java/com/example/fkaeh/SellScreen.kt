package com.example.fkaeh

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.IOException

@Composable
fun SellScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember(context) { context.getSharedPreferences("fkaeh_app", android.content.Context.MODE_PRIVATE) }

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var moneda by remember { mutableStateOf("€") }
    var estado by remember { mutableStateOf("Buen estado") }
    var showError by remember { mutableStateOf(false) }
    var categoriaExpanded by remember { mutableStateOf(false) }
    var estadoExpanded by remember { mutableStateOf(false) }
    var monedaExpanded by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var fotoUriTemp by remember { mutableStateOf<Uri?>(null) }
    val fotoUris = remember { mutableStateListOf<Uri>() }

    val categorias = CategoryCatalog.options.map { it.name }
    val estados = listOf("Perfecto", "Buen estado", "Mediocre", "Mal")
    val monedas = listOf("€", "$", "£")
    val precioValido = precio.toDoubleOrNull()?.let { it > 0 } ?: false

    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val uriCaptura = fotoUriTemp
        if (exito && uriCaptura != null && fotoUris.none { it == uriCaptura }) {
            fotoUris.add(uriCaptura)
        } else if (!exito && uriCaptura != null) {
            runCatching { context.contentResolver.delete(uriCaptura, null, null) }
        }
        cameraError = null
        fotoUriTemp = null
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.take(6).forEach { uri ->
                if (fotoUris.none { it == uri }) fotoUris.add(uri)
            }
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamaraSegura(
            context = context,
            onUriCreada = { uri ->
                cameraError = null
                fotoUriTemp = uri
                camaraLauncher.launch(uri)
            },
            onError = { cameraError = it }
        )
    }

    val permisoGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        prefs.edit().putBoolean("gallery_permission_prompted", true).apply()
        if (concedido) {
            cameraError = null
            galeriaLauncher.launch("image/*")
        } else {
            cameraError = "Permiso de galería denegado"
        }
    }

    fun abrirCamara() {
        abrirCamaraSegura(
            context = context,
            onUriCreada = { uri ->
                cameraError = null
                fotoUriTemp = uri
                camaraLauncher.launch(uri)
            },
            onError = { permisoCamaraLauncher.launch(Manifest.permission.CAMERA) }
        )
    }

    fun abrirGaleria() {
        cameraError = null
        val permisoGaleria = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val yaSePidio = prefs.getBoolean("gallery_permission_prompted", false)
        val tienePermiso = ContextCompat.checkSelfPermission(context, permisoGaleria) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!yaSePidio && !tienePermiso) {
            permisoGaleriaLauncher.launch(permisoGaleria)
        } else {
            galeriaLauncher.launch("image/*")
        }
    }

    LaunchedEffect(vm.venderExitoso) {
        if (vm.venderExitoso) {
            nombre = ""
            descripcion = ""
            categoria = ""
            precio = ""
            estado = "Buen estado"
            fotoUris.clear()
            showError = false
            vm.resetVenderEstado()
        }
    }

    val sellFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF111111),
        unfocusedContainerColor = Color(0xFF111111),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedPlaceholderColor = Color(0xFF555555),
        unfocusedPlaceholderColor = Color(0xFF555555),
        cursorColor = Purple
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 16.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp
                )
        ) {
            Text(
                "Subir producto",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Purple.copy(0.6f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF111111))
                    .clickable { abrirGaleria() },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUris.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(fotoUris.first()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.25f))
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = Purple,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${fotoUris.size} foto(s) añadida(s)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(0.5f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            null,
                            tint = Purple.copy(0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Añade fotos del producto", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                }
            }

            if (fotoUris.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fotoUris.forEachIndexed { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (index == 0) {
                                Text(
                                    "Principal",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(0.55f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { fotoUris.remove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(0.6f), CircleShape)
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { abrirCamara() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { abrirGaleria() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; showError = false },
                placeholder = { Text("Nombre del producto") },
                modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = sellFieldColors,
                singleLine = true,
                enabled = !vm.isLoading,
                isError = showError && nombre.isBlank()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it; showError = false },
                placeholder = { Text("Descripción del producto") },
                modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = sellFieldColors,
                minLines = 3,
                maxLines = 5,
                enabled = !vm.isLoading,
                isError = showError && descripcion.isBlank()
            )
            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    placeholder = { Text("Escoger categoría") },
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = sellFieldColors,
                    readOnly = true,
                    singleLine = true,
                    enabled = !vm.isLoading,
                    isError = showError && categoria.isBlank(),
                    trailingIcon = {
                        IconButton(onClick = { categoriaExpanded = true }) {
                            Text("›", fontSize = 20.sp, color = Color(0xFF666666))
                        }
                    }
                )
                DropdownMenu(
                    expanded = categoriaExpanded,
                    onDismissRequest = { categoriaExpanded = false },
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                ) {
                    categorias.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op, color = Color.White) },
                            onClick = { categoria = op; categoriaExpanded = false; showError = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = estado,
                    onValueChange = {},
                    placeholder = { Text("Estado de la prenda") },
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = sellFieldColors,
                    readOnly = true,
                    singleLine = true,
                    enabled = !vm.isLoading,
                    trailingIcon = {
                        IconButton(onClick = { estadoExpanded = true }) {
                            Text("›", fontSize = 20.sp, color = Color(0xFF666666))
                        }
                    }
                )
                DropdownMenu(
                    expanded = estadoExpanded,
                    onDismissRequest = { estadoExpanded = false },
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                ) {
                    estados.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op, color = Color.White) },
                            onClick = { estado = op; estadoExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            precio = it
                            showError = false
                        }
                    },
                    placeholder = { Text("Precio") },
                    modifier = Modifier.weight(2f).border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = sellFieldColors,
                    singleLine = true,
                    enabled = !vm.isLoading,
                    isError = showError && !precioValido
                )
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = moneda,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(0.6f), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = sellFieldColors,
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { monedaExpanded = true }) {
                                Text("›", fontSize = 20.sp, color = Color(0xFF666666))
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = monedaExpanded,
                        onDismissRequest = { monedaExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A1A1A))
                    ) {
                        monedas.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op, color = Color.White) },
                                onClick = { moneda = op; monedaExpanded = false }
                            )
                        }
                    }
                }
            }

            if (showError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        nombre.isBlank() -> "Introduce un nombre"
                        descripcion.isBlank() -> "Introduce una descripción"
                        categoria.isBlank() -> "Selecciona una categoría"
                        !precioValido -> "Introduce un precio válido"
                        fotoUris.isEmpty() -> "Añade al menos una foto"
                        else -> "Completa todos los campos"
                    },
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            if (vm.venderError != null) {
                Spacer(Modifier.height(6.dp))
                Text(vm.venderError!!, color = Color.Red, fontSize = 12.sp)
            }
            if (cameraError != null) {
                Spacer(Modifier.height(6.dp))
                Text(cameraError!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull()
                    if (
                        nombre.isNotBlank() &&
                        descripcion.isNotBlank() &&
                        categoria.isNotBlank() &&
                        precioDouble != null &&
                        precioDouble > 0 &&
                        fotoUris.isNotEmpty()
                    ) {
                        vm.venderEnBD(nombre, descripcion, precioDouble, categoria, estado, fotoUris.toList())
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !vm.isLoading
            ) {
                if (vm.isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBIR PRODUCTO", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Text("→", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
        }
    }
}

private fun abrirCamaraSegura(
    context: android.content.Context,
    onUriCreada: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val fotoFile = File.createTempFile("foto_producto_", ".jpg", context.cacheDir).apply {
            deleteOnExit()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", fotoFile)
        onUriCreada(uri)
    } catch (_: IllegalArgumentException) {
        onError("No se pudo preparar la cámara")
    } catch (_: IOException) {
        onError("No se pudo crear el archivo de la foto")
    } catch (_: Exception) {
        onError("La cámara no está disponible ahora mismo")
    }
}
