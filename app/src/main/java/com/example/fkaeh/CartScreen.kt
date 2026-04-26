package com.example.fkaeh

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

private enum class CartStep { CART, ADDRESS }

@Composable
fun CartScreen(vm: AppViewModel) {
    var step by remember { mutableStateOf(CartStep.CART) }
    var alias by remember { mutableStateOf("Casa") }
    var nombreCompleto by remember(vm.currentUser?.nombre) { mutableStateOf(vm.currentUser?.nombre.orEmpty()) }
    var telefono by remember(vm.currentUser?.telefono) { mutableStateOf(vm.currentUser?.telefono.orEmpty()) }
    var direccion by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }
    var direccionSeleccionada by remember(vm.direccionesGuardadas) { mutableStateOf<DireccionGuardada?>(vm.direccionesGuardadas.firstOrNull()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.carrito),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                if (step == CartStep.CART) vm.text("cart") else "Dirección de envío",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(36.dp))

            when (step) {
                CartStep.CART -> {
                    if (vm.carrito.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛒", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(vm.text("cart_empty"), fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(vm.text("add_from_home"), fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vm.carrito) { item ->
                                CartItem(item = item, onEliminar = { vm.eliminarDelCarrito(item) })
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OrderSummaryCard(
                            vm = vm,
                            buttonText = "Continuar",
                            onClick = { step = CartStep.ADDRESS }
                        )
                    }
                }

                CartStep.ADDRESS -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (vm.direccionesGuardadas.isNotEmpty()) {
                            item {
                                Text("Direcciones guardadas", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            items(vm.direccionesGuardadas) { saved ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = direccionSeleccionada == saved,
                                            onClick = { direccionSeleccionada = saved }
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(saved.alias, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(saved.nombreCompleto, color = Color(0xFFE0E0E0), fontSize = 13.sp)
                                            Text(saved.resumen(), color = Color.Gray, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = {
                                            if (direccionSeleccionada == saved) direccionSeleccionada = null
                                            vm.eliminarDireccion(saved)
                                        }) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text("O añade una nueva", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text("Alias") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = nombreCompleto, onValueChange = { nombreCompleto = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = telefono, onValueChange = { telefono = it.filter { ch -> ch.isDigit() }.take(15) }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = ciudad, onValueChange = { ciudad = it }, label = { Text("Ciudad") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = provincia, onValueChange = { provincia = it }, label = { Text("Provincia") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                        item {
                            OutlinedTextField(value = codigoPostal, onValueChange = { codigoPostal = it }, label = { Text("Código postal") }, modifier = Modifier.fillMaxWidth(), colors = BlackFieldColors)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val nuevaDireccionCompleta = direccion.isNotBlank() && ciudad.isNotBlank() && codigoPostal.isNotBlank()
                    val direccionFinal = direccionSeleccionada ?: if (nuevaDireccionCompleta) {
                        DireccionGuardada(
                            alias = alias.ifBlank { "Dirección" },
                            nombreCompleto = nombreCompleto.ifBlank { vm.currentUser?.nombre.orEmpty() },
                            telefono = telefono.ifBlank { vm.currentUser?.telefono.orEmpty() },
                            direccion = direccion,
                            ciudad = ciudad,
                            codigoPostal = codigoPostal,
                            provincia = provincia
                        )
                    } else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    direccionFinal?.let {
                                        if (direccionSeleccionada == null) vm.guardarDireccion(it)
                                        vm.finalizarCompraDemo(it)
                                        step = CartStep.CART
                                        alias = "Casa"
                                        direccion = ""
                                        ciudad = ""
                                        codigoPostal = ""
                                        provincia = ""
                                        direccionSeleccionada = vm.direccionesGuardadas.firstOrNull()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                                shape = RoundedCornerShape(22.dp),
                                enabled = direccionFinal != null && vm.carrito.isNotEmpty()
                            ) {
                                Text("Finalizar compra", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { step = CartStep.CART },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Volver al carrito", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(vm: AppViewModel, buttonText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                vm.text("order_summary"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))

            vm.carrito.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.producto.nombre, fontSize = 13.sp, color = Color.White.copy(0.8f), modifier = Modifier.weight(1f))
                    Text("x1", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                    Text("%.2f€".format(item.producto.precio), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF333333))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${vm.text("total")}:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("%.2f€".format(vm.totalCarrito()), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Purple)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(buttonText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}

@Composable
fun CartItem(item: ItemCarrito, onEliminar: () -> Unit) {
    val context = LocalContext.current
    val producto = item.producto
    val fotoUrl = remember(producto.fotoUrl) {
        producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Purple)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                        drawCircle(color = Purple)
                        drawCircle(color = Color.White, radius = 4.dp.toPx())
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            if (fotoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(fotoUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = producto.nombre.take(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Purple.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(producto.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text("%.2f€".format(producto.precio), fontSize = 14.sp, color = Purple, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, null, tint = Color.Red.copy(alpha = 0.65f))
            }
        }
    }
}
