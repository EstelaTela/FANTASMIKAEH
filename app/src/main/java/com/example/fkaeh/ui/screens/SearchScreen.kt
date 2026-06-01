package com.example.fkaeh.ui.screens

import com.example.fkaeh.R
import com.example.fkaeh.AppViewModel
import com.example.fkaeh.core.*
import com.example.fkaeh.data.models.*
import com.example.fkaeh.data.repository.*
import com.example.fkaeh.ui.common.*
import com.example.fkaeh.ui.components.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fkaeh.ui.theme.customPurple

@Composable
fun SearchScreen(
    vm: AppViewModel,
    onProductoClick: (Producto) -> Unit,
    onBackHome: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val categorias = remember(vm.productos.size, CategoryCatalog.options.size) {
        val remotas = CategoryCatalog.options
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (remotas.isNotEmpty()) {
            remotas
        } else {
            vm.productos
                .map { it.categoriaNombre.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            }
    }

    val queryNormalizada = query.trim()
    val categoriasFiltradas = categorias.filter {
        queryNormalizada.isBlank() || it.contains(queryNormalizada, ignoreCase = true)
    }

    val productosFiltrados = vm.productos.filter { producto ->
        val coincideCategoria = categoriaSeleccionada?.let {
            producto.categoriaNombre.equals(it, ignoreCase = true)
        } ?: true

        val coincideBusqueda = queryNormalizada.isBlank() ||
            producto.nombre.contains(queryNormalizada, ignoreCase = true) ||
            producto.descripcion.contains(queryNormalizada, ignoreCase = true) ||
            producto.categoriaNombre.contains(queryNormalizada, ignoreCase = true)

        coincideCategoria && coincideBusqueda
    }

    val searchFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = Color.Black,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedPlaceholderColor = Color(0xFF888888),
        unfocusedPlaceholderColor = Color(0xFF888888),
        focusedLeadingIconColor = Color.Black,
        unfocusedLeadingIconColor = Color.Black
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (categoriaSeleccionada != null) {
                            categoriaSeleccionada = null
                            query = ""
                        } else {
                            onBackHome()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            if (categoriaSeleccionada == null) "Buscar categoria" else "Buscar en ${categoriaSeleccionada.orEmpty()}",
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    colors = searchFieldColors
                )
            }

            Spacer(Modifier.height(18.dp))

            if (categoriaSeleccionada == null) {
                if (categoriasFiltradas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay categorias disponibles", color = Color.White, fontSize = 17.sp)
                    }
                } else {
                    Text(
                        text = "Categorias disponibles",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Explora lo que hay publicado por tipo de prenda",
                        color = Color(0xFFB8B8B8),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categoriasFiltradas, key = { it }) { categoria ->
                            CategorySearchCard(
                                categoria = categoria,
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    query = ""
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = categoriaSeleccionada.orEmpty(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${productosFiltrados.size} producto(s)",
                    color = Color(0xFFB8B8B8),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(14.dp))

                if (productosFiltrados.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay productos en esta categoria", color = Color.White, fontSize = 16.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(productosFiltrados, key = { it.id }) { producto ->
                            ProductoItem(
                                producto = producto,
                                isEnCarrito = vm.carrito.any { it.producto.id == producto.id },
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

@Composable
private fun CategorySearchCard(categoria: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = customPurple.copy(alpha = 0.20f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010).copy(alpha = 0.92f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = categoria,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
