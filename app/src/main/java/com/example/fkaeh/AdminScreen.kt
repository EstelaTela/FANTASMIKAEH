package com.example.fkaeh

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fkaeh.ui.theme.customPurple

private enum class AdminSection {
    USERS,
    PRODUCTS
}

@Composable
fun AdminScreen(vm: AppViewModel) {
    var usuarioEditando by remember { mutableStateOf<UsuarioBD?>(null) }
    var productoAccion by remember { mutableStateOf<Producto?>(null) }
    var accionProducto by remember { mutableStateOf<ProductAdminAction?>(null) }
    var section by remember { mutableStateOf(AdminSection.USERS) }

    LaunchedEffect(Unit) {
        vm.cargarUsuariosAdmin()
        vm.cargarProductosDesdeBD()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Panel de administración", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Gestiona usuarios y revisa todos los detalles de los productos publicados.",
                color = Color(0xFFD0D0D0),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            AdminTabs(
                selected = section,
                usersCount = vm.usuariosAdmin.size,
                productsCount = vm.productos.size,
                onSelect = { section = it }
            )

            Spacer(Modifier.height(16.dp))

            when (section) {
                AdminSection.USERS -> UsersAdminList(
                    usuarios = vm.usuariosAdmin,
                    onEditar = { usuarioEditando = it }
                )
                AdminSection.PRODUCTS -> ProductsAdminList(
                    productos = vm.productos,
                    onCensurar = {
                        productoAccion = it
                        accionProducto = ProductAdminAction.CENSOR
                    },
                    onEliminar = {
                        productoAccion = it
                        accionProducto = ProductAdminAction.DELETE
                    }
                )
            }
        }
    }

    usuarioEditando?.let { usuario ->
        EditarUsuarioDialog(
            usuario = usuario,
            onDismiss = { usuarioEditando = null },
            onGuardar = { nombre, correo, telefono ->
                vm.actualizarUsuarioComoAdmin(usuario.copy(nombre = nombre, correo = correo, telefono = telefono))
                usuarioEditando = null
            }
        )
    }

    if (productoAccion != null && accionProducto != null) {
        ConfirmarAccionProductoDialog(
            producto = productoAccion!!,
            accion = accionProducto!!,
            onDismiss = {
                productoAccion = null
                accionProducto = null
            },
            onConfirmar = {
                val producto = productoAccion ?: return@ConfirmarAccionProductoDialog
                when (accionProducto) {
                    ProductAdminAction.CENSOR -> vm.censurarProductoComoAdmin(producto)
                    ProductAdminAction.DELETE -> vm.eliminarProductoComoAdmin(producto)
                    null -> Unit
                }
                productoAccion = null
                accionProducto = null
            }
        )
    }
}

private enum class ProductAdminAction {
    CENSOR,
    DELETE
}

@Composable
private fun AdminTabs(
    selected: AdminSection,
    usersCount: Int,
    productsCount: Int,
    onSelect: (AdminSection) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminTabButton(
            text = "Usuarios",
            count = usersCount,
            selected = selected == AdminSection.USERS,
            onClick = { onSelect(AdminSection.USERS) },
            modifier = Modifier.weight(1f)
        )
        AdminTabButton(
            text = "Productos",
            count = productsCount,
            selected = selected == AdminSection.PRODUCTS,
            onClick = { onSelect(AdminSection.PRODUCTS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminTabButton(
    text: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) customPurple else Color.Black.copy(alpha = 0.72f)
    val contentColor = if (selected) Color.White else Color(0xFFE4E4E4)

    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("$count registros", fontSize = 11.sp, color = contentColor.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun UsersAdminList(
    usuarios: List<UsuarioBD>,
    onEditar: (UsuarioBD) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(usuarios, key = { it.id_usuario }) { usuario ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(usuario.nombre, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            EstadoCuentaChip(activo = usuario.activo)
                        }
                        Text("ID usuario: ${usuario.id_usuario}", color = Color(0xFFAFAFAF), fontSize = 12.sp)
                        Text(usuario.correo, color = Color(0xFFC8C8C8), fontSize = 13.sp)
                        Text(
                            if (usuario.telefono.isNotBlank()) "Teléfono: ${usuario.telefono}" else "Teléfono: sin dato",
                            color = customPurple,
                            fontSize = 12.sp
                        )
                        Text(
                            "Rol: ${usuario.nombre_rol?.ifBlank { null } ?: "Sin rol"} · ID rol: ${usuario.id_rol}",
                            color = Color(0xFFD8D8D8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { onEditar(usuario) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsAdminList(
    productos: List<Producto>,
    onCensurar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit
) {
    if (productos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Text(
                "No hay productos cargados ahora mismo.",
                color = Color(0xFFD0D0D0),
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(productos, key = { it.id }) { producto ->
            ProductAdminCard(
                producto = producto,
                onCensurar = onCensurar,
                onEliminar = onEliminar
            )
        }
    }
}

@Composable
private fun ProductAdminCard(
    producto: Producto,
    onCensurar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit
) {
    val context = LocalContext.current
    val fotoUrl = remember(producto.fotoUrls, producto.fotoUrl) {
        producto.fotoUrls.firstOrNull()?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        } ?: producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
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
                            producto.nombre.take(1).uppercase(),
                            color = customPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            producto.nombre,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (producto.estadoPrenda.equals(ESTADO_CENSURADO_ADMIN, ignoreCase = true)) {
                            EstadoProductoChip("Censurado", Color(0xFF5A1C1C), Color(0xFFFF8A8A))
                        }
                    }
                    Text("ID producto: ${producto.id}", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                    Text(
                        "Precio: ${"%.2f€".format(producto.precio)}",
                        color = customPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Vendedor: ${producto.nombreVendedor.ifBlank { "Sin nombre" }} · ID ${producto.idVendedor}",
                        color = Color(0xFFD7D7D7),
                        fontSize = 12.sp
                    )
                }
            }

            ProductInfoLine("Categoría", producto.categoriaNombre.ifBlank { "Sin categoría" })
            ProductInfoLine("ID categoría", producto.idCategoria?.toString() ?: "Sin dato")
            ProductInfoLine("Estado", producto.estadoPrenda.ifBlank { "Sin dato" })
            ProductInfoLine("Fotos", "${producto.fotoUrls.size.coerceAtLeast(if (producto.fotoUrl != null) 1 else 0)} imagen(es)")
            ProductInfoLine(
                "Descripción",
                producto.descripcion.ifBlank { "Sin descripción" },
                allowWrap = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onCensurar(producto) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A2B2B))
                ) {
                    Icon(Icons.Outlined.GppBad, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Censurar", color = Color.White)
                }
                Button(
                    onClick = { onEliminar(producto) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Eliminar", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EstadoProductoChip(texto: String, fondo: Color, textoColor: Color) {
    Box(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(texto, color = textoColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProductInfoLine(label: String, value: String, allowWrap: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            color = Color(0xFF9F9F9F),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = if (allowWrap) Int.MAX_VALUE else 1,
            overflow = if (allowWrap) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EstadoCuentaChip(activo: Boolean?) {
    val (texto, fondo, textoColor) = when (activo) {
        true -> Triple("Activa", customPurple.copy(alpha = 0.18f), customPurple)
        false -> Triple("Desactivada", Color(0xFF5A1C1C), Color(0xFFFF8A8A))
        null -> Triple("Sin dato", Color(0xFF242424), Color(0xFFBDBDBD))
    }

    Box(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = texto,
            color = textoColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ConfirmarAccionProductoDialog(
    producto: Producto,
    accion: ProductAdminAction,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    val titulo = if (accion == ProductAdminAction.CENSOR) "Censurar producto" else "Eliminar producto"
    val mensaje = if (accion == ProductAdminAction.CENSOR) {
        "Se ocultará este producto del catálogo público, se borrarán sus fotos visibles y quedará marcado como censurado en administración."
    } else {
        "Se eliminará este producto de forma permanente junto con sus referencias visibles en la app."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD0D0D0),
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (accion == ProductAdminAction.CENSOR) Color(0xFF7A2B2B) else Color(0xFFD50000)
                )
            ) {
                Text(if (accion == ProductAdminAction.CENSOR) "Censurar" else "Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFFBDBDBD)) }
        },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(producto.nombre, color = Color.White, fontWeight = FontWeight.Bold)
                Text(mensaje)
            }
        }
    )
}

@Composable
private fun EditarUsuarioDialog(
    usuario: UsuarioBD,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String) -> Unit
) {
    var nombre by remember(usuario.id_usuario) { mutableStateOf(usuario.nombre) }
    var correo by remember(usuario.id_usuario) { mutableStateOf(usuario.correo) }
    var telefono by remember(usuario.id_usuario) { mutableStateOf(usuario.telefono) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD0D0D0),
        confirmButton = {
            Button(onClick = { onGuardar(nombre.trim(), correo.trim(), telefono.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFFBDBDBD)) }
        },
        title = { Text("Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, colors = BlackFieldColors)
                OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo") }, colors = BlackFieldColors)
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it.filter { ch -> ch.isDigit() }.take(15) },
                    label = { Text("Teléfono") },
                    colors = BlackFieldColors
                )
            }
        }
    )
}
