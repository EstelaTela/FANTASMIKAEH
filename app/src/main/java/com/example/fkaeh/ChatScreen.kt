package com.example.fkaeh

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay
import kotlin.math.max

private data class OfferConversationUi(
    val thread: OfferThread,
    val counterpartName: String,
    val isSellerView: Boolean
)

@Composable
fun ChatScreen(
    vm: AppViewModel,
    onExploreHome: () -> Unit = {},
    onGoToCart: () -> Unit = {}
) {
    val currentUserId = vm.currentUser?.id_usuario
    val conversations = vm.offerThreadsForCurrentUser().map { thread ->
        OfferConversationUi(
            thread = thread,
            counterpartName = if (thread.sellerId == currentUserId) thread.buyerName else thread.sellerName,
            isSellerView = thread.sellerId == currentUserId
        )
    }
    val selectedThread = vm.activeOfferThread()

    BackHandler(enabled = selectedThread != null) { vm.closeOfferThread() }

    LaunchedEffect(currentUserId, selectedThread?.threadId) {
        if (currentUserId == null) return@LaunchedEffect
        while (true) {
            vm.cargarOfferThreads(showErrors = false)
            delay(if (selectedThread != null) 2000L else 4000L)
        }
    }

    if (selectedThread != null) {
        OfferThreadScreen(
            vm = vm,
            thread = selectedThread,
            isSellerView = selectedThread.sellerId == currentUserId,
            onBack = { vm.closeOfferThread() },
            onGoToCart = onGoToCart
        )
    } else {
        OfferInboxScreen(
            conversations = conversations,
            onOpen = { vm.openOfferThread(it.thread.threadId) },
            onExploreHome = onExploreHome
        )
    }
}

@Composable
private fun OfferInboxScreen(
    conversations: List<OfferConversationUi>,
    onOpen: (OfferConversationUi) -> Unit,
    onExploreHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.subirproducto),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(id = R.drawable.mancha),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(145.dp),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .padding(horizontal = 25.dp, vertical = 15.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text(
                        text = "Bandeja de Entrada",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (conversations.isEmpty()) {
                EmptyOfferInbox(onExploreHome = onExploreHome)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(conversations, key = { it.thread.threadId }) { conversation ->
                        OfferInboxRow(conversation = conversation, onClick = { onOpen(conversation) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOfferInbox(onExploreHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sin conversaciones abiertas",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Solo aparecerán aquí cuando alguien envíe una oferta real desde un producto.",
            color = Color(0xFFD0D0D0),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onExploreHome,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = customPurple, contentColor = Color.White)
        ) {
            Text("Explorar inicio", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OfferInboxRow(conversation: OfferConversationUi, onClick: () -> Unit) {
    val latestOffer = conversation.thread.latestOffer()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBadge(label = conversation.counterpartName, size = 58.dp)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = conversation.counterpartName,
                color = customPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = conversation.thread.productName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = latestOffer?.let { previewLabel(it.status, it.amount, conversation.isSellerView) } ?: "",
                color = Color(0xFFB8B8B8),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = latestOffer?.let { formatPrice(it.amount) } ?: formatPrice(conversation.thread.productBasePrice),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            StatusChip(status = latestOffer?.status ?: OfferStatus.PENDING)
        }
    }
}

@Composable
private fun OfferThreadScreen(
    vm: AppViewModel,
    thread: OfferThread,
    isSellerView: Boolean,
    onBack: () -> Unit,
    onGoToCart: () -> Unit
) {
    val latestOffer = thread.latestOffer()
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.carrito),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            OfferHeader(
                counterpartName = if (isSellerView) thread.buyerName else thread.sellerName,
                subtitle = thread.productName,
                onBack = onBack
            )

            OfferProductCard(thread = thread, latestOffer = latestOffer)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(thread.offers, key = { it.id }) { offer ->
                            OfferBubble(
                                offer = offer,
                                alignEnd = !isSellerView,
                                buyerName = thread.buyerName
                            )
                        }
                    }

                    if (isSellerView) {
                        SellerOfferActions(
                            latestOffer = latestOffer,
                            onAccept = { vm.acceptLatestOffer(thread.threadId) },
                            onReject = { vm.rejectLatestOffer(thread.threadId) }
                        )
                    } else {
                        BuyerOfferActions(
                            vm = vm,
                            thread = thread,
                            latestOffer = latestOffer,
                            onGoToCart = onGoToCart
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferHeader(
    counterpartName: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }
        AvatarBadge(label = counterpartName, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = counterpartName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun OfferProductCard(thread: OfferThread, latestOffer: OfferEntry?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumb(product = thread.toProducto())
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = thread.productName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Precio base ${formatPrice(thread.productBasePrice)}",
                    color = Color(0xFFD0D0D0),
                    fontSize = 13.sp
                )
                if (latestOffer != null) {
                    Text(
                        text = "Última oferta ${formatPrice(latestOffer.amount)}",
                        color = customPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (latestOffer != null) {
                StatusChip(status = latestOffer.status)
            }
        }
    }
}

@Composable
private fun OfferBubble(
    offer: OfferEntry,
    alignEnd: Boolean,
    buyerName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(
                text = buyerName,
                color = Color(0xFF7D7D7D),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (alignEnd) 18.dp else 6.dp,
                            bottomEnd = if (alignEnd) 6.dp else 18.dp
                        )
                    )
                    .background(
                        when (offer.status) {
                            OfferStatus.PENDING -> customPurple
                            OfferStatus.ACCEPTED -> Color(0xFF1A8F4A)
                            OfferStatus.REJECTED -> Color(0xFFB83838)
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Oferta ${formatPrice(offer.amount)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (offer.status) {
                            OfferStatus.PENDING -> "Pendiente de respuesta"
                            OfferStatus.ACCEPTED -> "Aceptada"
                            OfferStatus.REJECTED -> "Rechazada"
                        },
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerOfferActions(
    latestOffer: OfferEntry?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (latestOffer?.status) {
            OfferStatus.PENDING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = customPurple, contentColor = Color.White)
                    ) {
                        Text("ACEPTAR", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D1D1D), contentColor = Color.White)
                    ) {
                        Text("RECHAZAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
            OfferStatus.ACCEPTED -> {
                SellerInfoBanner("Oferta aceptada. El comprador ya puede pasar a la pasarela.")
            }
            OfferStatus.REJECTED -> {
                SellerInfoBanner("Oferta rechazada. El comprador puede enviar una nueva.")
            }
            null -> {
                SellerInfoBanner("Todavía no hay ofertas en este hilo.")
            }
        }
    }
}

@Composable
private fun SellerInfoBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF0F0F0))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = text, color = Color(0xFF323232), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun BuyerOfferActions(
    vm: AppViewModel,
    thread: OfferThread,
    latestOffer: OfferEntry?,
    onGoToCart: () -> Unit
) {
    val draft = vm.offerDraftFor(thread.threadId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (latestOffer?.status) {
            OfferStatus.PENDING -> {
                SellerInfoBanner("Tu oferta está pendiente. Espera a que el vendedor la acepte o la rechace.")
            }
            OfferStatus.ACCEPTED -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF4F1FB))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Procede al pago",
                        color = Color(0xFF171717),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tu oferta ha sido aceptada. Continúa con la dirección de envío y finaliza la compra.",
                        color = Color(0xFF575757),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = {
                            if (vm.startCheckoutFromAcceptedOffer(thread.threadId)) {
                                onGoToCart()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = customPurple, contentColor = Color.White)
                    ) {
                        Text("PROCEDER", fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { vm.updateOfferDraft(thread.threadId, it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Hacer oferta") },
                        placeholder = { Text("Ej. 95€") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F1F1),
                            unfocusedContainerColor = Color(0xFFF1F1F1),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = customPurple,
                            unfocusedLabelColor = Color(0xFF777777),
                            cursorColor = customPurple
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(customPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { vm.sendOfferInChat(thread.threadId, draft) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar oferta",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductThumb(product: Producto) {
    val context = LocalContext.current
    if (!product.fotoUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(product.fotoUrl)
                .crossfade(true)
                .build(),
            contentDescription = product.nombre,
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF202020)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF242424)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = product.nombre.take(1).uppercase(),
                color = customPurple,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AvatarBadge(label: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(customPurple.copy(alpha = 0.18f))
            .border(1.dp, customPurple.copy(alpha = 0.40f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildInitials(label),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (size > 50.dp) 20.sp else 16.sp
        )
    }
}

@Composable
private fun StatusChip(status: OfferStatus) {
    val (bg, fg, label) = when (status) {
        OfferStatus.PENDING -> Triple(customPurple.copy(alpha = 0.14f), customPurple, "Pendiente")
        OfferStatus.ACCEPTED -> Triple(Color(0xFF1A8F4A).copy(alpha = 0.14f), Color(0xFF1A8F4A), "Aceptada")
        OfferStatus.REJECTED -> Triple(Color(0xFFB83838).copy(alpha = 0.14f), Color(0xFFB83838), "Rechazada")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun previewLabel(status: OfferStatus, amount: Double, isSellerView: Boolean): String {
    val prefix = when (status) {
        OfferStatus.PENDING -> if (isSellerView) "Oferta pendiente" else "Tu oferta"
        OfferStatus.ACCEPTED -> "Oferta aceptada"
        OfferStatus.REJECTED -> "Oferta rechazada"
    }
    return "$prefix · ${formatPrice(amount)}"
}

private fun buildInitials(label: String): String {
    val parts = label.split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "FK"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

private fun formatPrice(value: Double): String {
    return if (value % 1.0 == 0.0) {
        "${value.toInt()}€"
    } else {
        "%.2f€".format(value)
    }
}
