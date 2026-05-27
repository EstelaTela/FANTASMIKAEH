package com.example.fkaeh

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.Normalizer

val Purple = Color(0xFF8C52FF)
val BlackBg = Color(0xFF0A0A0A)
private val HomePreferredProductOrder = listOf(
    "Jersey urban FKAEH",
    "Camiseta urban",
    "Top basico FKAEH",
    "Cazadora Crop FKAEH"
).map { it.normalizedHomeKey() }

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onProductoClick: (Producto) -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val adaptive = rememberAdaptiveLayout()

    LaunchedEffect(Unit) {
        vm.cargarProductosDesdeBD()
        vm.cargarFavoritos()
    }

    val orderedProducts = vm.productos
        .mapIndexed { index, producto -> index to producto }
        .sortedWith(
            compareBy<Pair<Int, Producto>>(
                { homePreferredRank(it.second) },
                { it.first }
            )
        )
        .map { it.second }
    val productRows = orderedProducts.chunked(adaptive.gridColumns)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg)
    ) {
        when {
            vm.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            }

            vm.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Sin conexión", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.cargarProductosDesdeBD() },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            vm.productos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BlackBg)
                ) {
                    HomeHero(
                        adaptive = adaptive,
                        onOpenSearch = onOpenSearch
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aún no hay productos", color = Color.White, fontSize = 18.sp)
                            Text("Sé el primero en vender algo brutal", color = Color(0xFF909090), fontSize = 14.sp)
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BlackBg)
                ) {
                    HomeHero(
                        adaptive = adaptive,
                        onOpenSearch = onOpenSearch
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = if (adaptive.isCompactWidth) 78.dp else 86.dp),
                        verticalArrangement = Arrangement.spacedBy(if (adaptive.isCompactWidth) 14.dp else 18.dp)
                    ) {
                        if (vm.esAdmin) {
                            item {
                                Text(
                                    text = "Modo administrador",
                                    color = Purple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        itemsIndexed(productRows, key = { index, _ -> "home_row_$index" }) { rowIndex, row ->
                            if (adaptive.gridColumns == 1) {
                                row.firstOrNull()?.let { producto ->
                                    HomeProductTile(
                                        producto = producto,
                                        imageHeight = homeCardHeightForRow(
                                            rowIndex = rowIndex,
                                            columns = adaptive.gridColumns,
                                            compact = adaptive.isCompactWidth
                                        ),
                                        compact = adaptive.isCompactWidth,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = adaptive.horizontalPadding),
                                        onClick = { onProductoClick(producto) }
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = adaptive.horizontalPadding),
                                    horizontalArrangement = Arrangement.spacedBy(adaptive.gridSpacing)
                                ) {
                                    row.forEach { producto ->
                                        HomeProductTile(
                                            producto = producto,
                                            imageHeight = homeCardHeightForRow(
                                                rowIndex = rowIndex,
                                                columns = adaptive.gridColumns,
                                                compact = adaptive.isCompactWidth
                                            ),
                                            compact = adaptive.isCompactWidth,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onProductoClick(producto) }
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    adaptive: AdaptiveLayout,
    onOpenSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clipToBounds()
    ) {
        Image(
            painter = painterResource(id = R.drawable.banner),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopStart
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        IconButton(
            onClick = onOpenSearch,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = adaptive.heroButtonInsetTop,
                    end = adaptive.heroButtonInsetEnd
                )
                .size(adaptive.heroButtonSize)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = Color.Black,
                modifier = Modifier.size(adaptive.heroIconSize)
            )
        }
    }
}

@Composable
private fun HomeProductTile(
    producto: Producto,
    imageHeight: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fotoUrlCompleta = remember(producto.fotoUrls, producto.fotoUrl) {
        producto.fotoUrls.firstOrNull()?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        } ?: producto.fotoUrl?.let {
            if (it.startsWith("http")) it else ApiClient.getBaseUrl(context) + it
        }
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2F2F2))
        ) {
            if (fotoUrlCompleta != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fotoUrlCompleta)
                        .crossfade(true)
                        .build(),
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = producto.nombre.take(1).uppercase(),
                        color = Purple,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = producto.nombre,
                color = Color.White,
                fontSize = if (compact) 14.sp else 15.sp,
                lineHeight = if (compact) 17.sp else 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "%.0f€".format(producto.precio),
                color = Color.White,
                fontSize = if (compact) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun homeCardHeightForRow(rowIndex: Int, columns: Int, compact: Boolean): Dp {
    if (columns == 1) {
        return if (compact) 220.dp else 240.dp
    }
    return if (rowIndex % 2 == 0) {
        if (compact) 160.dp else 180.dp
    } else {
        if (compact) 196.dp else 224.dp
    }
}

private fun homePreferredRank(producto: Producto): Int {
    val normalizedName = producto.nombre.normalizedHomeKey()
    return HomePreferredProductOrder.indexOf(normalizedName).takeIf { it >= 0 } ?: Int.MAX_VALUE
}

private fun String.normalizedHomeKey(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
}
