package com.example.fkaeh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ProductoItem(
    producto: Producto,
    onAddToCart: () -> Unit,
    onToggleFavorito: () -> Unit,
    isEnCarrito: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adaptive = rememberAdaptiveLayout()
    val context = LocalContext.current
    val imageHeight = if (adaptive.isCompactWidth) 160.dp else 180.dp
    val titleSize = if (adaptive.isCompactWidth) 12.sp else 13.sp
    val metaSize = if (adaptive.isCompactWidth) 10.sp else 11.sp
    val priceSize = if (adaptive.isCompactWidth) 12.sp else 13.sp

    val fotoUrlCompleta = remember(producto.fotoUrls) {
        producto.fotoUrls.firstOrNull()?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        } ?: producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))) {
                    if (fotoUrlCompleta != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(fotoUrlCompleta)
                                .crossfade(true)
                                .build(),
                            contentDescription = producto.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(imageHeight)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = producto.nombre.take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Purple.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorito,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (adaptive.isCompactWidth) 6.dp else 8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (producto.esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (producto.esFavorito) Color(0xFFFF5A7A) else Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    text = producto.nombre,
                    fontSize = titleSize,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (producto.nombreVendedor.isNotBlank()) {
                    Text(
                        text = "de ${producto.nombreVendedor}",
                        fontSize = metaSize,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "%.0f€".format(producto.precio),
                    fontSize = priceSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (isEnCarrito) {
                    Text(
                        text = "En carrito",
                        fontSize = metaSize,
                        color = Purple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
