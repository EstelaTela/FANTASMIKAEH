package com.example.fkaeh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fkaeh.ui.theme.customPurple

@Composable
fun ProductoDetalleScreen(
    producto: Producto,
    vm: AppViewModel,
    onGoToCart: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fotos = remember(producto.fotoUrls, producto.fotoUrl) {
        if (producto.fotoUrls.isNotEmpty()) producto.fotoUrls else listOfNotNull(producto.fotoUrl)
    }
    var fotoSeleccionada by remember(producto.id) { mutableIntStateOf(0) }
    val indiceSeguro = fotoSeleccionada.coerceIn(0, (fotos.size - 1).coerceAtLeast(0))
    val fotoUrlCompleta = fotos.getOrNull(indiceSeguro)?.let {
        if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
    }

    val enCarrito = vm.carrito.any { it.producto.id == producto.id }
    val esPropio = vm.currentUser?.id_usuario == producto.idVendedor
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                if (fotoUrlCompleta != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fotoUrlCompleta)
                            .crossfade(true)
                            .addHeader("ngrok-skip-browser-warning", "true")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = producto.nombre.take(1).uppercase(),
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Black,
                            color = Purple.copy(alpha = 0.3f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, BlackBg),
                                startY = 600f
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(
                            top = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 8.dp,
                            start = 8.dp
                        )
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (fotos.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fotos.forEachIndexed { index, foto ->
                            val url = if (foto.startsWith("http")) foto else ApiClient.getBaseUrl(context) + foto
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF151515))
                                    .clickable { fotoSeleccionada = index },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = producto.nombre,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (producto.nombreVendedor.isNotBlank()) "Subido por ${producto.nombreVendedor}" else "FKAEH",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.0f€".format(producto.precio),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        IconButton(onClick = { vm.toggleFavorito(producto) }) {
                            Icon(
                                imageVector = if (producto.esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (producto.esFavorito) Color(0xFFFF5A7A) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (enCarrito) onGoToCart() else vm.comprar(producto)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            esPropio -> Color(0xFF444444)
                            enCarrito -> Color.White
                            else -> Purple
                        }
                    ),
                    shape = RoundedCornerShape(50.dp),
                    enabled = !esPropio
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                esPropio -> "ES TU PRODUCTO"
                                enCarrito -> "✓ AÑADIDO AL CARRITO"
                                else -> "PÍDELO YA"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (enCarrito && !esPropio) Color.Black else Color.White
                        )
                        Text(
                            text = if (esPropio) "·" else "→",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (enCarrito && !esPropio) Color.Black else Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (producto.categoriaNombre.isNotBlank() || producto.estadoPrenda.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (producto.categoriaNombre.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = customPurple)) { append("Categoría: ") }
                                        append(producto.categoriaNombre)
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (producto.estadoPrenda.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = customPurple)) { append("Estado: ") }
                                        append(producto.estadoPrenda)
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = producto.descripcion,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 21.sp
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
