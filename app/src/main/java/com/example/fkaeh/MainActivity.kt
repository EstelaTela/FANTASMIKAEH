package com.example.fkaeh

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fkaeh.ui.theme.FKAEHTheme
import com.example.fkaeh.ui.theme.customPurple

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FKAEHTheme { App() } }
    }
}

enum class Screen { LOGIN, REGISTER, MAIN }
enum class Tab { SELL, HOME, SEARCH, CART, PROFILE, ADMIN }

@Composable
fun App() {
    val vm: AppViewModel = viewModel()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    var currentTab by remember { mutableStateOf(Tab.HOME) }
    var productoSeleccionadoId by remember { mutableStateOf<Int?>(null) }

    val notificacionesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(currentScreen) {
        if (
            currentScreen == Screen.MAIN &&
            vm.notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm.uiMessage) {
        vm.uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearUiMessage()
        }
    }

    BackHandler(enabled = true) {
        when (currentScreen) {
            Screen.REGISTER -> currentScreen = Screen.LOGIN
            Screen.LOGIN -> activity?.moveTaskToBack(true)
            Screen.MAIN -> {
                when {
                    productoSeleccionadoId != null -> productoSeleccionadoId = null
                    currentTab != Tab.HOME -> currentTab = Tab.HOME
                    else -> activity?.moveTaskToBack(true)
                }
            }
        }
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            vm = vm,
            onLoginSuccess = { currentScreen = Screen.MAIN },
            onIrARegistro = { currentScreen = Screen.REGISTER }
        )

        Screen.REGISTER -> RegisterScreen(
            vm = vm,
            onRegistroSuccess = { currentScreen = Screen.MAIN },
            onIrALogin = { currentScreen = Screen.LOGIN }
        )

        Screen.MAIN -> Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                FkaehBottomBar(
                    current = currentTab,
                    carritoCount = vm.carrito.size,
                    esAdmin = vm.esAdmin,
                    onChange = {
                        productoSeleccionadoId = null
                        currentTab = it
                    }
                )
            },
            containerColor = BlackBg
        ) { padding ->
            Box(Modifier.padding(padding)) {
                val productoSeleccionado = vm.productos.firstOrNull { it.id == productoSeleccionadoId }
                if (productoSeleccionado != null) {
                    ProductoDetalleScreen(
                        producto = productoSeleccionado,
                        vm = vm,
                        onGoToCart = {
                            productoSeleccionadoId = null
                            currentTab = Tab.CART
                        },
                        onBack = { productoSeleccionadoId = null }
                    )
                } else {
                    when (currentTab) {
                        Tab.HOME -> HomeScreen(vm = vm, onProductoClick = { productoSeleccionadoId = it.id })
                        Tab.SEARCH -> SearchScreen(
                            vm = vm,
                            onProductoClick = { productoSeleccionadoId = it.id },
                            onBackHome = { currentTab = Tab.HOME }
                        )
                        Tab.SELL -> SellScreen(vm)
                        Tab.CART -> CartScreen(vm)
                        Tab.PROFILE -> ProfileScreen(
                            vm = vm,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onLogout = {
                                vm.logout()
                                productoSeleccionadoId = null
                                currentTab = Tab.HOME
                                currentScreen = Screen.LOGIN
                            }
                        )
                        Tab.ADMIN -> AdminScreen(vm = vm)
                    }
                }
            }
        }
    }
}

data class NavItem(val tab: Tab, val icon: ImageVector, val label: String)

@Composable
fun FkaehBottomBar(current: Tab, carritoCount: Int, esAdmin: Boolean, onChange: (Tab) -> Unit) {
    val items = buildList {
        add(NavItem(Tab.SELL, Icons.Outlined.AddCircle, "Vender"))
        add(NavItem(Tab.HOME, Icons.Outlined.Home, "Inicio"))
        add(NavItem(Tab.SEARCH, Icons.Outlined.Search, "Buscar"))
        add(NavItem(Tab.CART, Icons.Outlined.ShoppingCart, "Carrito"))
        if (esAdmin) add(NavItem(Tab.ADMIN, Icons.Outlined.AdminPanelSettings, "Admin"))
        add(NavItem(Tab.PROFILE, Icons.Outlined.Person, "Perfil"))
    }

    NavigationBar(
        containerColor = Color(0xFF0D0D0D),
        modifier = Modifier.height(62.dp),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = current == item.tab,
                onClick = { onChange(item.tab) },
                icon = {
                    Box {
                        androidx.compose.material3.Icon(
                            item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                        if (item.tab == Tab.CART && carritoCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 5.dp, y = (-3).dp)
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(customPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    carritoCount.toString(),
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = customPurple,
                    indicatorColor = Color.Transparent
                ),
                label = null
            )
        }
    }
}
