package com.example.fkaeh.ui.screens

import com.example.fkaeh.core.*
import com.example.fkaeh.data.models.*
import com.example.fkaeh.data.repository.*
import com.example.fkaeh.ui.common.*
import com.example.fkaeh.ui.components.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fkaeh.core.ApiClient
import com.example.fkaeh.AppViewModel
import com.example.fkaeh.ui.common.BlackFieldColors
import com.example.fkaeh.data.models.CheckoutTarget
import com.example.fkaeh.core.DireccionGuardada
import com.example.fkaeh.data.models.ItemCarrito
import com.example.fkaeh.ui.common.Purple
import com.example.fkaeh.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class CartStep { CART, ADDRESS, PAYMENT }

private const val Demo3dsCode = "482913"

@Composable
fun CartScreen(vm: AppViewModel) {
    val paymentScope = rememberCoroutineScope()
    var step by remember { mutableStateOf(CartStep.CART) }
    var alias by remember { mutableStateOf("Casa") }
    var nombreCompleto by remember(vm.currentUser?.nombre) { mutableStateOf(vm.currentUser?.nombre.orEmpty()) }
    var telefono by remember(vm.currentUser?.telefono) { mutableStateOf(vm.currentUser?.telefono.orEmpty()) }
    var direccion by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }
    var direccionSeleccionada by remember(vm.direccionesGuardadas) { mutableStateOf<DireccionGuardada?>(vm.direccionesGuardadas.firstOrNull()) }
    var direccionPreparada by remember { mutableStateOf<DireccionGuardada?>(null) }
    var guardarDireccionNueva by remember { mutableStateOf(false) }
    var referenciaPago by remember { mutableStateOf(generatePaymentReference()) }
    var titularTarjeta by remember(vm.currentUser?.nombre) { mutableStateOf(vm.currentUser?.nombre.orEmpty()) }
    var numeroTarjeta by remember { mutableStateOf("") }
    var fechaCaducidad by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var codigo3ds by remember { mutableStateOf("") }
    var error3ds by remember { mutableStateOf<String?>(null) }
    var mostrar3ds by remember { mutableStateOf(false) }
    var procesandoPago by remember { mutableStateOf(false) }
    var estadoPago by remember { mutableStateOf("Esperando autenticacion segura") }
    var lastHandledCheckoutRequest by remember { mutableStateOf(0L) }

    val resetCheckoutState = {
        step = CartStep.CART
        alias = "Casa"
        nombreCompleto = vm.currentUser?.nombre.orEmpty()
        telefono = vm.currentUser?.telefono.orEmpty()
        direccion = ""
        ciudad = ""
        codigoPostal = ""
        provincia = ""
        direccionSeleccionada = vm.direccionesGuardadas.firstOrNull()
        direccionPreparada = null
        guardarDireccionNueva = false
        referenciaPago = generatePaymentReference()
        titularTarjeta = vm.currentUser?.nombre.orEmpty()
        numeroTarjeta = ""
        fechaCaducidad = ""
        cvv = ""
        codigo3ds = ""
        error3ds = null
        mostrar3ds = false
        procesandoPago = false
        estadoPago = "Esperando autenticacion segura"
    }

    val resetPaymentAttempt = {
        referenciaPago = generatePaymentReference()
        codigo3ds = ""
        error3ds = null
        mostrar3ds = false
        procesandoPago = false
        estadoPago = "Esperando autenticacion segura"
    }

    val selectSavedAddress: (DireccionGuardada) -> Unit = { saved ->
        direccionSeleccionada = saved
        direccionPreparada = saved
        guardarDireccionNueva = false
    }

    LaunchedEffect(vm.checkoutRequestId, vm.direccionesGuardadas) {
        if (vm.checkoutRequestId == 0L || vm.checkoutRequestId == lastHandledCheckoutRequest) return@LaunchedEffect

        when (vm.checkoutTarget) {
            CheckoutTarget.CART -> step = CartStep.CART
            CheckoutTarget.ADDRESS -> step = CartStep.ADDRESS
            CheckoutTarget.PAYMENT -> {
                val saved = vm.direccionesGuardadas.firstOrNull()
                if (saved != null) {
                    selectSavedAddress(saved)
                    resetPaymentAttempt()
                    step = CartStep.PAYMENT
                } else {
                    step = CartStep.ADDRESS
                }
            }
        }

        lastHandledCheckoutRequest = vm.checkoutRequestId
    }

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
                when (step) {
                    CartStep.CART -> vm.text("cart")
                    CartStep.ADDRESS -> "Direccion de envio"
                    CartStep.PAYMENT -> "Pasarela segura"
                },
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
                            onClick = {
                                direccionPreparada = null
                                step = CartStep.ADDRESS
                            }
                        )
                    }
                }

                CartStep.ADDRESS -> {
                    val camposNuevaDireccionHabilitados = direccionSeleccionada == null
                    val addressFieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111),
                        errorContainerColor = Color(0xFF111111),
                        focusedIndicatorColor = Purple,
                        unfocusedIndicatorColor = Purple.copy(alpha = 0.58f),
                        errorIndicatorColor = Color(0xFFFF8A80),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Purple.copy(alpha = 0.95f),
                        unfocusedLabelColor = Color(0xFFE1D4FF),
                        focusedPlaceholderColor = Color(0xFFC3B8DC),
                        unfocusedPlaceholderColor = Color(0xFFA99DBF),
                        cursorColor = Purple
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (vm.direccionesGuardadas.isNotEmpty()) {
                            item {
                                Text("Direcciones guardadas", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            items(vm.direccionesGuardadas) { saved ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.48f)),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Purple.copy(alpha = 0.28f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectSavedAddress(saved) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = direccionSeleccionada == saved,
                                            onClick = { selectSavedAddress(saved) }
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(saved.alias, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(saved.nombreCompleto, color = Color(0xFFE0E0E0), fontSize = 13.sp)
                                            Text(saved.resumen(), color = Color.Gray, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = {
                                            if (direccionSeleccionada == saved) direccionSeleccionada = null
                                            if (direccionPreparada == saved) direccionPreparada = null
                                            vm.eliminarDireccion(saved)
                                        }) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(Modifier.height(6.dp))
                                if (camposNuevaDireccionHabilitados) {
                                    Text("O añade una nueva", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Button(
                                        onClick = {
                                            direccionSeleccionada = null
                                            direccionPreparada = null
                                            guardarDireccionNueva = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        shape = RoundedCornerShape(22.dp)
                                    ) {
                                        Text("Usar una direccion nueva", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (camposNuevaDireccionHabilitados) {
                            item {
                                OutlinedTextField(
                                    value = alias,
                                    onValueChange = { alias = it },
                                    label = { Text("Alias") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = nombreCompleto,
                                    onValueChange = {
                                        nombreCompleto = it
                                        if (it.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Nombre completo") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = telefono,
                                    onValueChange = {
                                        telefono = it.filter { ch -> ch.isDigit() }.take(15)
                                        if (telefono.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Telefono") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    )
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = direccion,
                                    onValueChange = {
                                        direccion = it
                                        if (it.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Direccion") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = ciudad,
                                    onValueChange = {
                                        ciudad = it
                                        if (it.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Ciudad") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = provincia,
                                    onValueChange = {
                                        provincia = it
                                        if (it.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Provincia") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = codigoPostal,
                                    onValueChange = {
                                        codigoPostal = it.filter { ch -> ch.isDigit() }.take(5)
                                        if (codigoPostal.isNotBlank()) direccionSeleccionada = null
                                    },
                                    label = { Text("Codigo postal") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = addressFieldColors,
                                    shape = RoundedCornerShape(18.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }
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
                                        direccionPreparada = it
                                        guardarDireccionNueva = direccionSeleccionada == null
                                        resetPaymentAttempt()
                                        step = CartStep.PAYMENT
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                                shape = RoundedCornerShape(22.dp),
                                enabled = direccionFinal != null && vm.carrito.isNotEmpty()
                            ) {
                                Text("Ir a la pasarela", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

                CartStep.PAYMENT -> {
                    val cardDigits = numeroTarjeta.filter { it.isDigit() }
                    val cardBrand = identifyCardBrand(cardDigits)
                    val cardValid = isCardNumberValid(cardDigits)
                    val expiryValid = isExpiryValid(fechaCaducidad)
                    val cvvValid = cvv.length in 3..4
                    val holderValid = titularTarjeta.isNotBlank()
                    val paymentReady = direccionPreparada != null &&
                        holderValid &&
                        cardValid &&
                        expiryValid &&
                        cvvValid &&
                        vm.carrito.isNotEmpty()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            PaymentGatewayHeader(
                                amount = vm.totalCarrito(),
                                reference = referenciaPago,
                                brand = cardBrand,
                                address = direccionPreparada
                            )
                        }
                        item {
                            PaymentFormCard(
                                titularTarjeta = titularTarjeta,
                                onTitularChange = { titularTarjeta = it },
                                numeroTarjeta = numeroTarjeta,
                                onNumeroChange = { numeroTarjeta = formatCardNumber(it) },
                                fechaCaducidad = fechaCaducidad,
                                onFechaChange = { fechaCaducidad = formatExpiryDate(it) },
                                cvv = cvv,
                                onCvvChange = { cvv = it.filter { ch -> ch.isDigit() }.take(4) },
                                cardBrand = cardBrand,
                                cardValid = cardValid,
                                expiryValid = expiryValid,
                                cvvValid = cvvValid
                            )
                        }
                        item {
                            PaymentSecurityCard()
                        }
                        item {
                            PaymentSummaryCard(vm = vm, direccion = direccionPreparada)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.44f)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Purple.copy(alpha = 0.24f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (procesandoPago) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Purple,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(estadoPago, color = Color.White, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            Button(
                                onClick = {
                                    codigo3ds = ""
                                    error3ds = null
                                    mostrar3ds = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                                shape = RoundedCornerShape(22.dp),
                                enabled = paymentReady && !procesandoPago
                            ) {
                                Text(
                                    "Pagar ${"%.2f€".format(vm.totalCarrito())}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { step = CartStep.ADDRESS },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(22.dp),
                                enabled = !procesandoPago
                            ) {
                                Text("Volver a la direccion", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (mostrar3ds && direccionPreparada != null) {
                        SecureVerificationDialog(
                            amount = vm.totalCarrito(),
                            maskedCard = maskCardNumber(numeroTarjeta),
                            code = codigo3ds,
                            onCodeChange = {
                                codigo3ds = it.filter { ch -> ch.isDigit() }.take(6)
                                error3ds = null
                            },
                            errorMessage = error3ds,
                            processing = procesandoPago,
                            onDismiss = { if (!procesandoPago) mostrar3ds = false },
                            onConfirm = {
                                if (codigo3ds != Demo3dsCode) {
                                    error3ds = "El codigo no coincide con la autenticacion demo"
                                } else {
                                    paymentScope.launch {
                                        procesandoPago = true
                                        estadoPago = "Validando tarjeta con la entidad emisora"
                                        delay(1100)
                                        estadoPago = "Aplicando autenticacion 3D Secure"
                                        delay(900)
                                        estadoPago = "Autorizando operacion y cerrando pedido"
                                        delay(1200)
                                        direccionPreparada?.let { direccionConfirmada ->
                                            if (guardarDireccionNueva) vm.guardarDireccion(direccionConfirmada)
                                            vm.finalizarCompraDemo(direccionConfirmada)
                                        }
                                        resetCheckoutState()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentGatewayHeader(
    amount: Double,
    reference: String,
    brand: String,
    address: DireccionGuardada?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FKAEH Payments", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Cobro cifrado TLS 1.3", color = Color(0xFFBDBDBD), fontSize = 12.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Importe", color = Color.Gray, fontSize = 11.sp)
                    Text("%.2f€".format(amount), color = Purple, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pedido", color = Color.Gray, fontSize = 11.sp)
                    Text(reference, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = Color(0xFF2E2E2E))

            Text(
                "Metodo detectado: $brand",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Entrega: ${address?.resumen().orEmpty()}",
                color = Color(0xFFD0D0D0),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PaymentFormCard(
    titularTarjeta: String,
    onTitularChange: (String) -> Unit,
    numeroTarjeta: String,
    onNumeroChange: (String) -> Unit,
    fechaCaducidad: String,
    onFechaChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    cardBrand: String,
    cardValid: Boolean,
    expiryValid: Boolean,
    cvvValid: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.84f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Datos de pago", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Aceptamos Visa, Mastercard y American Express",
                color = Color.Gray,
                fontSize = 12.sp
            )

            HorizontalDivider(color = Color(0xFF252525))

            OutlinedTextField(
                value = titularTarjeta,
                onValueChange = onTitularChange,
                label = { Text("Titular de la tarjeta") },
                modifier = Modifier.fillMaxWidth(),
                colors = BlackFieldColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = numeroTarjeta,
                onValueChange = onNumeroChange,
                label = { Text("Numero de tarjeta") },
                modifier = Modifier.fillMaxWidth(),
                colors = BlackFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
            if (numeroTarjeta.isNotBlank()) {
                PaymentFieldStatus(
                    message = if (cardValid) "Formato valido · $cardBrand" else "Introduce 15 o 16 digitos validos",
                    color = if (cardValid) Color(0xFF7FD48B) else Color(0xFFFF8A80)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = fechaCaducidad,
                        onValueChange = onFechaChange,
                        label = { Text("Caducidad") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = BlackFieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    if (fechaCaducidad.isNotBlank()) {
                        PaymentFieldStatus(
                            message = if (expiryValid) "Fecha correcta" else "Usa MM/AA y una fecha futura",
                            color = if (expiryValid) Color(0xFF7FD48B) else Color(0xFFFF8A80)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = onCvvChange,
                        label = { Text("CVV") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = BlackFieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        )
                    )
                    if (cvv.isNotBlank()) {
                        PaymentFieldStatus(
                            message = if (cvvValid) "Codigo valido" else "El CVV debe tener 3 o 4 digitos",
                            color = if (cvvValid) Color(0xFF7FD48B) else Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentFieldStatus(message: String, color: Color) {
    Text(message, color = color, fontSize = 11.sp)
}

@Composable
private fun PaymentSecurityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Seguridad de la operacion", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Cifrado punto a punto", color = Color(0xFFD9D9D9), fontSize = 12.sp)
            Text("Tokenizacion de tarjeta", color = Color(0xFFD9D9D9), fontSize = 12.sp)
            Text("Verificacion reforzada con 3D Secure", color = Color(0xFFD9D9D9), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PaymentSummaryCard(vm: AppViewModel, direccion: DireccionGuardada?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen del cobro", color = Color.White, fontWeight = FontWeight.Bold)
            direccion?.let {
                Text("Enviar a ${it.nombreCompleto}", color = Color(0xFFD4D4D4), fontSize = 12.sp)
                Text(it.resumen(), color = Color.Gray, fontSize = 12.sp)
                if (it.telefono.isNotBlank()) {
                    Text("Telefono ${it.telefono}", color = Color.Gray, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = Color(0xFF2F2F2F))

            vm.carrito.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.producto.nombre, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Text("%.2f€".format(item.producto.precio), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = Color(0xFF2F2F2F))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("%.2f€".format(vm.totalCarrito()), color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SecureVerificationDialog(
    amount: Double,
    maskedCard: String,
    code: String,
    onCodeChange: (String) -> Unit,
    errorMessage: String?,
    processing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Autenticacion bancaria", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Confirma la operacion para completar el pago seguro.", color = Color(0xFFD0D0D0), fontSize = 13.sp)
                Text("Importe: ${"%.2f€".format(amount)}", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Tarjeta: $maskedCard", color = Color.White, fontSize = 13.sp)
                Text("Codigo demo 3D Secure: $Demo3dsCode", color = Color(0xFFFFD54F), fontSize = 12.sp)

                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text("Codigo SMS") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = BlackFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !processing
                )

                errorMessage?.let {
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(18.dp),
                        enabled = !processing
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        enabled = code.length == 6 && !processing
                    ) {
                        Text("Autorizar")
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

private fun formatCardNumber(input: String): String =
    input.filter { it.isDigit() }
        .take(16)
        .chunked(4)
        .joinToString(" ")

private fun formatExpiryDate(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length <= 2 -> digits
        else -> "${digits.take(2)}/${digits.drop(2)}"
    }
}

private fun isCardNumberValid(cardDigits: String): Boolean = when {
    cardDigits.matches(Regex("^3[47]\\d{13}$")) -> true
    cardDigits.length == 16 -> true
    else -> false
}

private fun identifyCardBrand(cardDigits: String): String = when {
    cardDigits.startsWith("4") -> "Visa"
    cardDigits.matches(Regex("^5[1-5].*")) -> "Mastercard"
    cardDigits.matches(Regex("^3[47].*")) -> "American Express"
    cardDigits.isBlank() -> "Pendiente"
    else -> "Tarjeta"
}

private fun isExpiryValid(expiry: String): Boolean {
    if (!Regex("""\d{2}/\d{2}""").matches(expiry)) return false
    val month = expiry.take(2).toIntOrNull() ?: return false
    val year = expiry.takeLast(2).toIntOrNull() ?: return false
    if (month !in 1..12) return false

    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR) % 100
    val currentMonth = now.get(Calendar.MONTH) + 1

    return year > currentYear || (year == currentYear && month >= currentMonth)
}

private fun maskCardNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    if (digits.isBlank()) return "**** **** **** ****"
    val suffix = digits.takeLast(4).padStart(4, '*')
    return "**** **** **** $suffix"
}

private fun generatePaymentReference(): String =
    "FK-${System.currentTimeMillis().toString().takeLast(8)}"

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
                    Canvas(modifier = Modifier.size(20.dp)) {
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
                if (producto.nombreVendedor.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "de ${producto.nombreVendedor}",
                        fontSize = 11.sp,
                        color = Color(0xFFC7C7C7)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text("%.2f€".format(producto.precio), fontSize = 14.sp, color = Purple, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, null, tint = Color.Red.copy(alpha = 0.65f))
            }
        }
    }
}
