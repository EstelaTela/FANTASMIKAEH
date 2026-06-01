package com.example.fkaeh.data.models

enum class OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

enum class CheckoutTarget {
    CART,
    ADDRESS,
    PAYMENT
}

data class OfferEntry(
    val id: String,
    val amount: Double,
    val buyerId: Int,
    val createdAt: Long,
    val status: OfferStatus = OfferStatus.PENDING
)

data class OfferThread(
    val threadId: String,
    val productId: Int,
    val productName: String,
    val productBasePrice: Double,
    val productPhotoUrl: String? = null,
    val sellerId: Int,
    val sellerName: String,
    val buyerId: Int,
    val buyerName: String,
    val offers: List<OfferEntry> = emptyList()
) {
    fun latestOffer(): OfferEntry? = offers.maxByOrNull { it.createdAt }

    fun toProducto(priceOverride: Double? = null): Producto {
        return Producto(
            id = productId,
            idVendedor = sellerId,
            nombre = productName,
            precio = priceOverride ?: productBasePrice,
            fotoUrl = productPhotoUrl,
            nombreVendedor = sellerName
        )
    }
}
