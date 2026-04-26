package com.example.fkaeh

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fkaeh_app", Context.MODE_PRIVATE)

    private val _productos = mutableStateListOf<Producto>()
    val productos: List<Producto> = _productos

    private val _carrito = mutableStateListOf<ItemCarrito>()
    val carrito: List<ItemCarrito> = _carrito

    private val _historialCompras = mutableStateListOf<ItemCarrito>()
    val historialCompras: List<ItemCarrito> = _historialCompras

    private val _favoritos = mutableStateListOf<Int>()
    val favoritos: List<Int> = _favoritos

    private val _direccionesGuardadas = mutableStateListOf<DireccionGuardada>()
    val direccionesGuardadas: List<DireccionGuardada> = _direccionesGuardadas

    private val _usuariosAdmin = mutableStateListOf<UsuarioBD>()
    val usuariosAdmin: List<UsuarioBD> = _usuariosAdmin

    var currentLanguage by mutableStateOf(AppLanguage.fromCode(prefs.getString("language", AppLanguage.ES.code)))
        private set
    var notificationsEnabled by mutableStateOf(NotificationHelper.isEnabled(application))
        private set
    var profilePhotoPath by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var uiMessage by mutableStateOf<String?>(null)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set
    var currentUser by mutableStateOf<UsuarioBD?>(null)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    var registroError by mutableStateOf<String?>(null)
        private set

    var venderExitoso by mutableStateOf(false)
        private set
    var venderError by mutableStateOf<String?>(null)
        private set

    private val productoRepository = ProductoRepository(application.applicationContext)

    init {
        NotificationHelper.ensureChannel(application)
    }

    val esAdmin: Boolean
        get() = currentUser?.nombre_rol?.contains("admin", ignoreCase = true) == true || currentUser?.id_rol == 1

    fun text(key: String): String = AppText.get(currentLanguage, key)

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        prefs.edit().putString("language", language.code).apply()
        uiMessage = "${text("language")}: ${language.label}"
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        NotificationHelper.setEnabled(getApplication(), enabled)
        uiMessage = if (enabled) "Notificaciones activadas" else "Notificaciones desactivadas"
    }

    fun comprar(producto: Producto) {
        val userId = currentUser?.id_usuario
        if (userId != null && producto.idVendedor == userId) {
            uiMessage = "No puedes comprar tu propio producto"
            return
        }
        if (_carrito.none { it.producto.id == producto.id }) {
            _carrito.add(ItemCarrito(producto))
            uiMessage = "Producto añadido al carrito"
            NotificationHelper.show(
                getApplication(),
                "Carrito actualizado",
                "${producto.nombre} se ha añadido al carrito"
            )
        }
    }

    fun eliminarDelCarrito(item: ItemCarrito) {
        _carrito.remove(item)
    }

    fun vaciarCarrito() {
        _carrito.clear()
    }

    fun totalCarrito(): Double = _carrito.sumOf { it.producto.precio }

    fun finalizarCompraDemo(direccion: DireccionGuardada) {
        if (_carrito.isEmpty()) return
        uiMessage = "Redirigiendo al pago de prueba para ${direccion.alias}"
        confirmarCompra()
    }

    private fun confirmarCompra() {
        if (_carrito.isEmpty()) return

        val propios = _carrito.filter { it.producto.idVendedor == currentUser?.id_usuario }
        if (propios.isNotEmpty()) {
            uiMessage = "No puedes comprar productos tuyos"
            _carrito.removeAll(propios)
            return
        }

        val itemsComprados = _carrito.toList()
        _historialCompras.addAll(0, itemsComprados)
        guardarHistorial()
        itemsComprados.forEach { _productos.removeAll { producto -> producto.id == it.producto.id } }
        _carrito.clear()
        uiMessage = "Compra realizada con éxito"
        NotificationHelper.show(
            getApplication(),
            "Compra realizada",
            "Tu pedido de ${itemsComprados.size} producto(s) se ha completado"
        )

        viewModelScope.launch {
            itemsComprados.forEach { productoRepository.eliminarProducto(it.producto.id) }
            cargarProductosDesdeBD()
        }
    }

    fun login(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            loginError = "Completa todos los campos"
            return
        }
        viewModelScope.launch {
            isLoading = true
            loginError = null
            productoRepository.loginUsuario(correo, contrasena)
                .onSuccess {
                    isLoggedIn = true
                    currentUser = it
                    profilePhotoPath = resolveProfilePhotoPath(it.id_usuario)
                    cargarHistorial()
                    cargarDirecciones()
                    cargarProductosDesdeBD()
                    cargarFavoritos()
                }
                .onFailure {
                    loginError = if (it.message?.contains("Credenciales") == true) {
                        "Email o contraseña incorrectos"
                    } else {
                        "Error de conexión al servidor"
                    }
                }
            isLoading = false
        }
    }

    fun limpiarErrorLogin() {
        loginError = null
    }

    fun registrar(nombre: String, correo: String, contrasena: String, telefono: String) {
        val telefonoNormalizado = telefono.filter { it.isDigit() }
        when {
            !correo.contains("@") -> {
                registroError = "Introduce un correo válido"
                return
            }
            contrasena.length < 6 -> {
                registroError = "La contraseña debe tener al menos 6 caracteres"
                return
            }
            telefonoNormalizado.length < 9 -> {
                registroError = "El teléfono debe tener al menos 9 dígitos"
                return
            }
        }
        viewModelScope.launch {
            isLoading = true
            registroError = null
            productoRepository.registrarUsuario(nombre, correo, contrasena, telefonoNormalizado)
                .onSuccess {
                    isLoggedIn = true
                    currentUser = it
                    profilePhotoPath = resolveProfilePhotoPath(it.id_usuario)
                    cargarHistorial()
                    cargarDirecciones()
                    cargarProductosDesdeBD()
                    cargarFavoritos()
                }
                .onFailure {
                    registroError = if (it.message?.contains("registrado") == true) {
                        "Este correo ya tiene una cuenta"
                    } else {
                        "Error de conexión al servidor"
                    }
                }
            isLoading = false
        }
    }

    fun limpiarErrorRegistro() {
        registroError = null
    }

    fun logout() {
        isLoggedIn = false
        currentUser = null
        loginError = null
        registroError = null
        _productos.clear()
        _carrito.clear()
        _historialCompras.clear()
        _favoritos.clear()
        _direccionesGuardadas.clear()
        _usuariosAdmin.clear()
        profilePhotoPath = null
        uiMessage = null
    }

    fun updateProfilePhoto(uri: Uri) {
        val userId = currentUser?.id_usuario ?: return
        val context = getApplication<Application>().applicationContext
        runCatching {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.takeIf { it.isNotBlank() }
                ?: "jpg"
            context.filesDir.listFiles()
                ?.filter { it.name.startsWith("profile_photo_$userId.") }
                ?.forEach { it.delete() }
            val destFile = File(context.filesDir, "profile_photo_$userId.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("No se pudo leer la imagen")
            destFile.absolutePath
        }.onSuccess { savedPath ->
            profilePhotoPath = savedPath
            prefs.edit().putString(profilePhotoKey(userId), savedPath).apply()
            uiMessage = "Foto de perfil actualizada"
        }.onFailure {
            uiMessage = "No se pudo actualizar la foto de perfil"
        }
    }

    fun cargarProductosDesdeBD() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            productoRepository.getAllProductosFromBD()
                .onSuccess { lista ->
                    val favoritosSet = _favoritos.toSet()
                    val listaVisible = if (esAdmin) {
                        lista
                    } else {
                        lista.filter { !it.estadoPrenda.orEmpty().equals(ESTADO_CENSURADO_ADMIN, ignoreCase = true) }
                    }
                    _productos.clear()
                    listaVisible.forEach { _productos.add(it.toProductoApp(esFavorito = favoritosSet.contains(it.idProducto))) }
                }
                .onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun cargarFavoritos() {
        val idUsuario = currentUser?.id_usuario ?: return
        viewModelScope.launch {
            productoRepository.getFavoritosIds(idUsuario).onSuccess { ids ->
                _favoritos.clear()
                _favoritos.addAll(ids)
                actualizarFavoritosEnProductos()
            }
        }
    }

    fun toggleFavorito(producto: Producto) {
        val idUsuario = currentUser?.id_usuario ?: run {
            uiMessage = "Inicia sesión para usar favoritos"
            return
        }

        viewModelScope.launch {
            val estaba = _favoritos.contains(producto.id)
            val result = if (estaba) {
                productoRepository.eliminarFavorito(idUsuario, producto.id)
            } else {
                productoRepository.agregarFavorito(idUsuario, producto.id)
            }

            result.onSuccess {
                if (estaba) _favoritos.remove(producto.id) else _favoritos.add(producto.id)
                actualizarFavoritosEnProductos()
                uiMessage = if (estaba) "Quitado de favoritos" else "Añadido a favoritos"
                NotificationHelper.show(
                    getApplication(),
                    "Lista de deseos",
                    if (estaba) "${producto.nombre} se ha quitado de favoritos" else "${producto.nombre} se ha guardado en favoritos"
                )
            }.onFailure {
                uiMessage = it.message ?: "No se pudo actualizar favoritos"
            }
        }
    }

    fun venderEnBD(
        nombre: String,
        descripcion: String,
        precio: Double,
        categoria: String,
        estado: String,
        fotoUris: List<Uri> = emptyList()
    ) {
        val idVendedor = currentUser?.id_usuario ?: return
        viewModelScope.launch {
            isLoading = true
            venderError = null
            venderExitoso = false
            productoRepository.crearProducto(idVendedor, nombre, descripcion, precio, categoria, estado, fotoUris)
                .onSuccess {
                    venderExitoso = true
                    uiMessage = "Producto subido correctamente"
                    NotificationHelper.show(
                        getApplication(),
                        "Producto publicado",
                        "$nombre ya está disponible en FKAEH"
                    )
                    cargarProductosDesdeBD()
                }
                .onFailure {
                    venderError = it.message ?: "No se pudo subir el producto. Intenta de nuevo"
                }
            isLoading = false
        }
    }

    fun resetVenderEstado() {
        venderExitoso = false
        venderError = null
    }

    fun clearUiMessage() {
        uiMessage = null
    }

    fun verificarParaRecuperar(correo: String, telefono: String, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            productoRepository.verificarParaRecuperar(correo, telefono)
                .onSuccess { onResult(it) }
                .onFailure { onResult(null) }
        }
    }

    fun cambiarPassword(idUsuario: Int, nuevaContrasena: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(productoRepository.cambiarPassword(idUsuario, nuevaContrasena).isSuccess)
        }
    }

    fun cambiarMiPassword(
        passwordActual: String,
        nuevaContrasena: String,
        confirmacion: String
    ) {
        val idUsuario = currentUser?.id_usuario ?: return
        when {
            passwordActual.isBlank() || nuevaContrasena.isBlank() || confirmacion.isBlank() -> {
                uiMessage = "Completa todos los campos"
                return
            }
            nuevaContrasena.length < 6 -> {
                uiMessage = "La nueva contraseña debe tener al menos 6 caracteres"
                return
            }
            nuevaContrasena != confirmacion -> {
                uiMessage = "La confirmación no coincide"
                return
            }
        }
        viewModelScope.launch {
            val passwordOk = productoRepository.verificarPasswordActual(idUsuario, passwordActual).getOrDefault(false)
            if (!passwordOk) {
                uiMessage = "La contraseña actual no coincide"
                return@launch
            }
            val ok = productoRepository.cambiarPassword(idUsuario, nuevaContrasena).isSuccess
            uiMessage = if (ok) text("password_updated") else text("password_error")
        }
    }

    fun guardarDireccion(direccion: DireccionGuardada) {
        val existe = _direccionesGuardadas.any {
            it.alias.equals(direccion.alias, ignoreCase = true) && it.resumen() == direccion.resumen()
        }
        if (!existe) {
            _direccionesGuardadas.add(0, direccion)
            persistirDirecciones()
            uiMessage = "Dirección guardada"
        }
    }

    fun eliminarDireccion(direccion: DireccionGuardada) {
        _direccionesGuardadas.remove(direccion)
        persistirDirecciones()
        uiMessage = "Dirección eliminada"
    }

    fun cargarDirecciones() {
        _direccionesGuardadas.clear()
        val raw = prefs.getString(addressesKey(), null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                _direccionesGuardadas.add(
                    DireccionGuardada(
                        alias = item.optString("alias"),
                        nombreCompleto = item.optString("nombreCompleto"),
                        telefono = item.optString("telefono"),
                        direccion = item.optString("direccion"),
                        ciudad = item.optString("ciudad"),
                        codigoPostal = item.optString("codigoPostal"),
                        provincia = item.optString("provincia")
                    )
                )
            }
        }
    }

    fun cargarUsuariosAdmin() {
        if (!esAdmin) return
        viewModelScope.launch {
            productoRepository.getUsuarios()
                .onSuccess {
                    _usuariosAdmin.clear()
                    _usuariosAdmin.addAll(it)
                }
                .onFailure {
                    uiMessage = it.message ?: "No se pudieron cargar los usuarios"
                }
        }
    }

    fun actualizarUsuarioComoAdmin(usuario: UsuarioBD) {
        viewModelScope.launch {
            productoRepository.actualizarUsuario(usuario)
                .onSuccess {
                    val index = _usuariosAdmin.indexOfFirst { it.id_usuario == usuario.id_usuario }
                    if (index >= 0) _usuariosAdmin[index] = usuario
                    if (currentUser?.id_usuario == usuario.id_usuario) {
                        currentUser = usuario
                    }
                    uiMessage = "Usuario actualizado"
                }
                .onFailure {
                    uiMessage = it.message ?: "No se pudo actualizar el usuario"
                }
        }
    }

    fun eliminarProductoComoAdmin(producto: Producto) {
        if (!esAdmin) return
        viewModelScope.launch {
            val result = productoRepository.eliminarProducto(producto.id)
            if (result.isSuccess) {
                _productos.removeAll { it.id == producto.id }
                _favoritos.remove(producto.id)
                uiMessage = "Producto eliminado"
                cargarProductosDesdeBD()
            } else {
                uiMessage = result.exceptionOrNull()?.message ?: "No se pudo eliminar el producto"
            }
        }
    }

    fun censurarProductoComoAdmin(producto: Producto) {
        if (!esAdmin) return
        viewModelScope.launch {
            val result = productoRepository.censurarProducto(producto.id)
            if (result.isSuccess) {
                uiMessage = "Producto censurado"
                cargarProductosDesdeBD()
            } else {
                uiMessage = result.exceptionOrNull()?.message ?: "No se pudo censurar el producto"
            }
        }
    }

    fun desactivarMiCuenta(onResult: (Boolean) -> Unit) {
        val idUsuario = currentUser?.id_usuario ?: run {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = productoRepository.desactivarUsuario(idUsuario)
            if (result.isSuccess) {
                uiMessage = "Cuenta desactivada"
                onResult(true)
            } else {
                uiMessage = result.exceptionOrNull()?.message ?: "No se pudo desactivar la cuenta"
                onResult(false)
            }
        }
    }

    fun eliminarMiCuenta(passwordActual: String, onResult: (Boolean) -> Unit) {
        val idUsuario = currentUser?.id_usuario ?: run {
            onResult(false)
            return
        }
        if (passwordActual.isBlank()) {
            uiMessage = "Introduce tu contraseña actual"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val passwordOk = productoRepository.verificarPasswordActual(idUsuario, passwordActual).getOrDefault(false)
            if (!passwordOk) {
                uiMessage = "La contraseña actual no coincide"
                onResult(false)
                return@launch
            }
            val result = productoRepository.eliminarUsuario(idUsuario)
            if (result.isSuccess) {
                uiMessage = "Cuenta eliminada"
                onResult(true)
            } else {
                uiMessage = result.exceptionOrNull()?.message ?: "No se pudo eliminar la cuenta"
                onResult(false)
            }
        }
    }

    private fun actualizarFavoritosEnProductos() {
        val actualizados = _productos.map { producto ->
            producto.copy(esFavorito = _favoritos.contains(producto.id))
        }
        _productos.clear()
        _productos.addAll(actualizados)
    }

    private fun historialKey(): String {
        val userId = currentUser?.id_usuario ?: return "history_guest"
        return "history_$userId"
    }

    private fun addressesKey(): String {
        val userId = currentUser?.id_usuario ?: return "addresses_guest"
        return "addresses_$userId"
    }

    private fun profilePhotoKey(userId: Int): String = "profile_photo_$userId"

    private fun resolveProfilePhotoPath(userId: Int): String? {
        val storedPath = prefs.getString(profilePhotoKey(userId), null)
        if (!storedPath.isNullOrBlank() && File(storedPath).exists()) {
            return storedPath
        }
        val fallback = getApplication<Application>().filesDir.listFiles()
            ?.firstOrNull { it.name.startsWith("profile_photo_$userId.") }
            ?.absolutePath
        if (fallback != null) {
            prefs.edit().putString(profilePhotoKey(userId), fallback).apply()
        }
        return fallback
    }

    private fun guardarHistorial() {
        val array = JSONArray()
        _historialCompras.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.producto.id)
                    put("idVendedor", item.producto.idVendedor)
                    put("idCategoria", item.producto.idCategoria)
                    put("categoriaNombre", item.producto.categoriaNombre)
                    put("nombre", item.producto.nombre)
                    put("precio", item.producto.precio)
                    put("descripcion", item.producto.descripcion)
                    put("estadoPrenda", item.producto.estadoPrenda)
                    put("fotoUrl", item.producto.fotoUrl)
                    put("nombreVendedor", item.producto.nombreVendedor)
                }
            )
        }
        prefs.edit().putString(historialKey(), array.toString()).apply()
    }

    private fun cargarHistorial() {
        _historialCompras.clear()
        val raw = prefs.getString(historialKey(), null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val producto = Producto(
                    id = obj.optInt("id"),
                    idVendedor = obj.optInt("idVendedor"),
                    idCategoria = obj.optInt("idCategoria").takeIf { it != 0 },
                    categoriaNombre = obj.optString("categoriaNombre"),
                    nombre = obj.optString("nombre"),
                    precio = obj.optDouble("precio"),
                    descripcion = obj.optString("descripcion"),
                    estadoPrenda = obj.optString("estadoPrenda"),
                    fotoUrl = obj.optString("fotoUrl").takeIf { it.isNotBlank() },
                    nombreVendedor = obj.optString("nombreVendedor")
                )
                _historialCompras.add(ItemCarrito(producto))
            }
        }
    }

    private fun persistirDirecciones() {
        val array = JSONArray()
        _direccionesGuardadas.forEach { direccion ->
            array.put(
                JSONObject().apply {
                    put("alias", direccion.alias)
                    put("nombreCompleto", direccion.nombreCompleto)
                    put("telefono", direccion.telefono)
                    put("direccion", direccion.direccion)
                    put("ciudad", direccion.ciudad)
                    put("codigoPostal", direccion.codigoPostal)
                    put("provincia", direccion.provincia)
                }
            )
        }
        prefs.edit().putString(addressesKey(), array.toString()).apply()
    }
}
