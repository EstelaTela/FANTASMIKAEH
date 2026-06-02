package com.example.fkaeh.ui.screens

import com.example.fkaeh.R
import com.example.fkaeh.core.ApiClient
import com.example.fkaeh.data.models.Producto
import com.example.fkaeh.ui.common.BlackBg
import com.example.fkaeh.ui.theme.customPurple

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun PublicProfileScreen(
    userId: Int,
    userName: String,
    photoUrl: String?,
    productos: List<Producto>,
    onBack: () -> Unit,
    onProductoClick: (Producto) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BlackBg)) {
        Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = "PERFIL",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.74f)
            ) {
                PublicProfileHeader(
                    name = userName.ifBlank { "Usuario #$userId" },
                    photoUrl = photoUrl,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, customPurple, RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .padding(11.dp)
            ) {
                Text(
                    text = "Productos subidos",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    Modifier.fillMaxWidth(),
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )
                Spacer(Modifier.height(12.dp))

                if (productos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Todavía no tiene productos subidos",
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(productos, key = { it.id }) { producto ->
                            PublicProfileProductTile(
                                producto = producto,
                                onClick = { onProductoClick(producto) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileHeader(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .border(2.dp, customPurple, RoundedCornerShape(30.dp))
            .background(Color.Black)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PublicAvatar(name = name, photoUrl = photoUrl, size = 100.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize =19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PublicProfileProductTile(producto: Producto, onClick: () -> Unit) {
    val context = LocalContext.current
    val fotoUrl = remember(producto.fotoUrls, producto.fotoUrl) {
        producto.fotoUrls.firstOrNull()?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        } ?: producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1C))
            .clickable(onClick = onClick)
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = producto.nombre.take(1).uppercase(),
                    color = customPurple,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${producto.nombre} · %.0f€".format(producto.precio),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PublicAvatar(name: String, photoUrl: String?, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(customPurple.copy(alpha = 0.18f))
            .border(1.dp, customPurple.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = profileInitials(name),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun profileInitials(name: String): String {
    val parts = name.split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "FK"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}
