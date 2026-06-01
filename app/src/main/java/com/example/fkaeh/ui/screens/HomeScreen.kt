package com.example.fkaeh.ui.screens

import com.example.fkaeh.core.*
import com.example.fkaeh.data.models.*
import com.example.fkaeh.data.repository.*
import com.example.fkaeh.ui.common.*
import com.example.fkaeh.ui.components.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fkaeh.AppViewModel
import com.example.fkaeh.data.models.Producto
import com.example.fkaeh.ui.components.ProductoItem
import com.example.fkaeh.ui.common.Purple
import com.example.fkaeh.R

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onProductoClick: (Producto) -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {

    LaunchedEffect(Unit) {
        vm.cargarProductosDesdeBD()
        vm.cargarFavoritos()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Image(
                        painter = painterResource(id = R.drawable.banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.FillBounds
                    )
                }

                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .padding(18.dp)
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Buscar",
                        tint = Color.Black,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (vm.esAdmin) {
                    Text(
                        text = "Modo administrador",
                        color = Purple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                when {
                    vm.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Purple)
                        }
                    }

                    vm.errorMessage != null -> {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Sin conexión", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { vm.cargarProductosDesdeBD() },
                                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                                shape = RoundedCornerShape(50.dp)
                            ) { Text("Reintentar") }
                        }
                    }

                    vm.productos.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Aún no hay productos", color = Color.White, fontSize = 18.sp)
                                Text("¡Sé el primero en vender!", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(vm.productos, key = { it.id }) { producto ->
                                val enCarrito = vm.carrito.any { it.producto.id == producto.id }
                                ProductoItem(
                                    producto = producto,
                                    isEnCarrito = enCarrito,
                                    onAddToCart = { vm.comprar(producto) },
                                    onToggleFavorito = { vm.toggleFavorito(producto) },
                                    onClick = { onProductoClick(producto) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
