package com.example.fkaeh

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class ProductoRepository(private val context: Context) {

    suspend fun loginUsuario(correo: String, contrasena: String): Result<UsuarioBD> {
        return try {
            val authBody = JSONObject().apply {
                put("email", correo)
                put("password", contrasena)
            }

            val (authCode, authResponse) = ApiClient.postRaw(
                context,
                "/auth/v1/token?grant_type=password",
                authBody
            )

            if (authCode !in 200..299) {
                val errorDesc = runCatching {
                    JSONObject(authResponse).optString("error_description", "")
                }.getOrDefault("")
                return Result.failure(
                    Exception(
                        if (errorDesc.contains("Invalid", ignoreCase = true))
                            "Email o contraseña incorrectos"
                        else "Error de autenticación"
                    )
                )
            }

            val accessToken = JSONObject(authResponse).optString("access_token", "")
            if (accessToken.isBlank()) {
                return Result.failure(Exception("No se recibió token de acceso"))
            }

            val userEndpoint = "/rest/v1/usuarios" +
                    "?correo=eq.${URLEncoder.encode(correo, "UTF-8")}" +
                    "&select=*,roles(nombre_rol)" +
                    "&limit=1"

            val userArray = ApiClient.getArrayWithToken(context, userEndpoint, accessToken)

            if (userArray.length() == 0) {
                return Result.failure(Exception("Usuario no encontrado en base de datos"))
            }

            Result.success(parseUsuario(userArray.getJSONObject(0)))

        } catch (e: Exception) {
            android.util.Log.e("LOGIN_DEBUG", "${e.javaClass.simpleName}: ${e.message}")
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    private suspend fun fetchCategoryOptions(): List<CategoryOption> {
        val endpoints = listOf(
            "/rest/v1/categorias?select=id_categoria,nombre_categoria&order=id_categoria.asc",
            "/rest/v1/categorias?select=id_categoria,nombre&order=id_categoria.asc",
            "/rest/v1/categorias?select=id,nombre&order=id.asc"
        )

        for (endpoint in endpoints) {
            runCatching {
                val array = ApiClient.getArray(context, endpoint)
                val categories = mutableListOf<CategoryOption>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = when {
                        item.has("id_categoria") -> item.optInt("id_categoria", -1)
                        item.has("id") -> item.optInt("id", -1)
                        else -> -1
                    }
                    val name = item.optString("nombre_categoria")
                        .ifBlank { item.optString("nombre") }
                        .trim()

                    if (id > 0 && name.isNotBlank()) {
                        categories.add(CategoryOption(id, name))
                    }
                }
                categories
            }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                CategoryCatalog.updateOptions(it)
                return it
            }
        }

        return emptyList()
    }

    // 🔧 REEMPLAZA ESTE MÉTODO en ProductoRepository.kt

    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        telefono: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {

            val signupBody = JSONObject().apply {
                put("email", correo)
                put("password", contrasena)
            }

            val signupUrl = "${ApiClient.getBaseUrl(context)}/auth/v1/signup"

            val conn = (URL(signupUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("apikey", ApiClient.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
            }

            val bodyString = signupBody.toString()

            conn.outputStream.use { output ->
                output.write(bodyString.toByteArray())
            }

            val code = conn.responseCode

            val responseText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            conn.disconnect()

            if (code !in 200..299) {
                val msg = runCatching { JSONObject(responseText).optString("msg", "") }.getOrDefault("")
                return@withContext Result.failure(
                    Exception(
                        if (msg.contains("already registered", true)) "Correo ya registrado"
                        else "Error al registrar: $code"
                    )
                )
            }

            val userBody = JSONObject().apply {
                put("nombre", nombre)
                put("correo", correo)
                put("telefono", telefono)
                put("id_rol", 2)
            }
            val array = ApiClient.postArray(context, "/rest/v1/usuarios", userBody)

            val maybeError = array.optJSONObject(0)
            if (maybeError != null && (maybeError.has("code") || maybeError.has("message"))) {
                val msg = maybeError.optString("message", "")
                if (msg.contains("unique", true) || msg.contains("duplicate", true)) {
                    return@withContext Result.success(Unit)
                }
                return@withContext Result.failure(Exception(msg))
            }

            Result.success(Unit)

        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Timeout: El servidor tardó demasiado en responder"))
        } catch (e: java.net.ConnectException) {
            android.util.Log.e("SIGNUP_DEBUG", "❌ ERROR DE CONEXIÓN: No se puede conectar a Supabase")
            android.util.Log.e("SIGNUP_DEBUG", "Verifica: 1) Internet funciona  2) URL es correcta  3) Supabase está online")
            Result.failure(Exception("No se puede conectar a Supabase"))
        } catch (e: Exception) {
            android.util.Log.e("SIGNUP_DEBUG", "❌ EXCEPCIÓN: ${e.javaClass.simpleName}")
            android.util.Log.e("SIGNUP_DEBUG", "Mensaje: ${e.message}")
            Result.failure(Exception("No se pudo conectar al servidor: ${e.message}"))
        }
    }

    suspend fun getAllProductosFromBD(): Result<List<ProductoBD>> {
        return try {
            fetchCategoryOptions()
            val endpointConVendedor = "/rest/v1/productos" +
                    "?select=*,usuarios(nombre),fotos_producto(url_foto,es_principal)" +
                    "&order=fecha_publicacion.desc"
            val endpointConVendedorAlt = "/rest/v1/productos" +
                    "?select=*,Usuarios(nombre),fotos_producto(url_foto,es_principal)" +
                    "&order=fecha_publicacion.desc"
            val endpointSimple = "/rest/v1/productos" +
                    "?select=*,fotos_producto(url_foto,es_principal)" +
                    "&order=fecha_publicacion.desc"

            val array = runCatching { ApiClient.getArray(context, endpointConVendedor) }
                .recoverCatching { ApiClient.getArray(context, endpointConVendedorAlt) }
                .recoverCatching { ApiClient.getArray(context, endpointSimple) }
                .getOrThrow()

            val userNamesById = if (containsMissingSellerNames(array)) {
                fetchUserNamesById()
            } else {
                emptyMap()
            }

            Result.success(parseProductos(array, userNamesById))
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE", "Error cargando productos: ${e.message}", e)
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun crearProducto(
        idVendedor: Int,
        nombre: String,
        descripcion: String,
        precioBase: Double,
        categoria: String,
        estadoPrenda: String,
        fotoUris: List<Uri> = emptyList()
    ): Result<Int> {
        return try {
            val remoteCategories = fetchCategoryOptions()
            val categoryId = remoteCategories.firstOrNull { it.name.equals(categoria, ignoreCase = true) }?.id
                ?: CategoryCatalog.idFor(categoria)

            val body = JSONObject().apply {
                put("id_vendedor", idVendedor)
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("precio_base", precioBase)
                put("estado_prenda", estadoPrenda)
                categoryId?.let { put("id_categoria", it) }
            }

            val array = ApiClient.postArray(context, "/rest/v1/productos", body)

            if (array.length() == 0) {
                Result.failure(Exception("No se pudo crear el producto"))
            } else {
                val p = array.getJSONObject(0)
                if (p.has("code") || p.has("message")) {
                    Result.failure(Exception(p.optString("message", "Error al crear producto")))
                } else {
                    val idProducto = p.getInt("id_producto")
                    if (fotoUris.isNotEmpty()) {
                        val resultadoFotos = subirFotos(idProducto, fotoUris)
                        if (resultadoFotos.isFailure) {
                            eliminarProducto(idProducto)
                            return Result.failure(
                                resultadoFotos.exceptionOrNull() ?: Exception("No se pudieron subir las fotos")
                            )
                        }
                    }
                    Result.success(idProducto)
                }
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun getFavoritosIds(idUsuario: Int): Result<Set<Int>> {
        return try {
            val endpoint = "/rest/v1/favoritos?id_usuario=eq.$idUsuario&select=id_producto"
            val array = ApiClient.getArray(context, endpoint)
            val ids = mutableSetOf<Int>()
            for (i in 0 until array.length()) {
                ids.add(array.getJSONObject(i).getInt("id_producto"))
            }
            Result.success(ids)
        } catch (_: Exception) {
            Result.success(emptySet())
        }
    }

    suspend fun uploadProfilePhoto(userId: Int, photoUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val photoBytes = context.contentResolver.openInputStream(photoUri)?.readBytes()
                    ?: return@withContext Result.failure(Exception("No se pudo leer la foto"))

                val mimeType = context.contentResolver.getType(photoUri)?.takeIf { it.isNotBlank() } ?: "image/jpeg"
                val objectPath = profilePhotoObjectPath(userId)
                val bucketUrl = "${ApiClient.getBaseUrl(context)}/storage/v1/object/fotos-productos/$objectPath"

                val conn = (URL(bucketUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    setRequestProperty("apikey", ApiClient.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${ApiClient.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", mimeType)
                    setRequestProperty("x-upsert", "true")
                }

                conn.outputStream.use { it.write(photoBytes) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                conn.disconnect()

                if (code in 200..299) {
                    Result.success(cacheBustedProfilePhotoUrl(userId))
                } else {
                    Result.failure(Exception("Error al subir foto de perfil: $code ${text.take(180)}".trim()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resolveProfilePhotoUrl(userId: Int): String? = withContext(Dispatchers.IO) {
        val publicUrl = profilePhotoPublicUrl(userId)
        runCatching {
            if (remoteFileExists(publicUrl)) cacheBustedProfilePhotoUrl(userId) else null
        }.getOrNull()
    }

    suspend fun agregarFavorito(idUsuario: Int, idProducto: Int): Result<Unit> {
        return try {
            val body = JSONObject().apply {
                put("id_usuario", idUsuario)
                put("id_producto", idProducto)
            }
            val array = ApiClient.postArray(context, "/rest/v1/favoritos", body)
            val maybeError = array.optJSONObject(0)
            if (maybeError != null && (maybeError.has("code") || maybeError.has("message"))) {
                val msg = maybeError.optString("message", "No se pudo guardar en favoritos")
                if (msg.contains("duplicate", true) || msg.contains("unique", true)) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(msg))
                }
            } else {
                Result.success(Unit)
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo guardar en favoritos"))
        }
    }

    suspend fun eliminarFavorito(idUsuario: Int, idProducto: Int): Result<Unit> {
        return try {
            ApiClient.delete(context, "/rest/v1/favoritos?id_usuario=eq.$idUsuario&id_producto=eq.$idProducto")
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo quitar de favoritos"))
        }
    }

    suspend fun eliminarProducto(idProducto: Int): Result<Unit> {
        return try {
            ApiClient.delete(context, "/rest/v1/favoritos?id_producto=eq.$idProducto")
            ApiClient.delete(context, "/rest/v1/fotos_producto?id_producto=eq.$idProducto")
            ApiClient.delete(context, "/rest/v1/productos?id_producto=eq.$idProducto")
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo eliminar el producto"))
        }
    }

    suspend fun censurarProducto(idProducto: Int): Result<Unit> {
        return try {
            ApiClient.delete(context, "/rest/v1/favoritos?id_producto=eq.$idProducto")
            ApiClient.delete(context, "/rest/v1/fotos_producto?id_producto=eq.$idProducto")

            val body = JSONObject().apply {
                put("nombre", "Producto retirado por administracion")
                put("descripcion", "Contenido ocultado por incumplir las normas de la plataforma.")
                put("estado_prenda", ESTADO_CENSURADO_ADMIN)
            }

            val code = ApiClient.patch(context, "/rest/v1/productos?id_producto=eq.$idProducto", body)
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("No se pudo censurar el producto"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo censurar el producto"))
        }
    }

    suspend fun solicitarCodigoRecuperacion(correo: String, telefono: String): Result<Unit> {
        return try {
            val correoNormalizado = correo.trim().lowercase()
            val correoEnc = URLEncoder.encode(correoNormalizado, "UTF-8")
            val telefonoEnc = URLEncoder.encode(telefono, "UTF-8")
            val endpoint = "/rest/v1/usuarios" +
                    "?correo=eq.$correoEnc" +
                    "&telefono=eq.$telefonoEnc" +
                    "&select=id_usuario" +
                    "&limit=1"

            val array = ApiClient.getArray(context, endpoint)
            if (array.length() == 0) {
                return Result.failure(Exception("Usuario no encontrado"))
            }

            val body = JSONObject().apply { put("email", correoNormalizado) }
            val (code, responseText) = ApiClient.postRaw(
                context,
                "/auth/v1/recover",
                body
            )

            if (code in 200..299) {
                Result.success(Unit)
            } else {
                val msg = runCatching {
                    val json = JSONObject(responseText)
                    json.optString("msg").ifBlank { json.optString("error_description") }.ifBlank { json.optString("error") }
                }.getOrDefault("")
                Result.failure(Exception(msg.ifBlank { "No se pudo enviar el codigo" }))
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun restablecerPasswordConCodigo(
        correo: String,
        codigo: String,
        nuevaContrasena: String
    ): Result<Unit> {
        return try {
            val verifyBody = JSONObject().apply {
                put("email", correo.trim().lowercase())
                put("token", codigo.trim())
                put("type", "recovery")
            }
            val (verifyCode, verifyResponseText) = ApiClient.postRaw(
                context,
                "/auth/v1/verify",
                verifyBody
            )

            if (verifyCode !in 200..299) {
                val msg = runCatching {
                    val json = JSONObject(verifyResponseText)
                    json.optString("msg").ifBlank { json.optString("error_description") }.ifBlank { json.optString("error") }
                }.getOrDefault("")
                return Result.failure(Exception(msg.ifBlank { "Codigo incorrecto o expirado" }))
            }

            val accessToken = JSONObject(verifyResponseText).optString("access_token")
            if (accessToken.isBlank()) {
                return Result.failure(Exception("No se pudo abrir la sesion de recuperacion"))
            }

            val updateBody = JSONObject().apply {
                put("password", nuevaContrasena)
            }
            val (updateCode, updateResponseText) = ApiClient.putRawWithToken(
                context,
                "/auth/v1/user",
                accessToken,
                updateBody
            )

            if (updateCode in 200..299) {
                Result.success(Unit)
            } else {
                val msg = runCatching {
                    val json = JSONObject(updateResponseText)
                    json.optString("msg").ifBlank { json.optString("error_description") }.ifBlank { json.optString("error") }
                }.getOrDefault("")
                Result.failure(Exception(msg.ifBlank { "No se pudo actualizar la contrasena" }))
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun verificarPasswordActual(idUsuario: Int, passwordActual: String): Result<Boolean> {
        return try {
            // CAMBIO: Ahora verificamos la contraseña contra Supabase Auth
            // Necesitaremos el email del usuario para esto
            // Por ahora, devolvemos un placeholder - esto se debe implementar mejor
            // consultando al usuario primero y luego intentando autenticarse
            Result.failure(Exception("Verifica la contraseña a través de Supabase Auth"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo verificar la contraseña actual"))
        }
    }

    suspend fun cambiarPassword(idUsuario: Int, nuevaContrasena: String): Result<Unit> {
        return try {
            // CAMBIO: Ahora cambias la contraseña a través de Supabase Auth
            // Necesitarías el email y hacer una llamada a /auth/v1/user
            // con el accessToken del usuario logueado
            Result.failure(Exception("Cambio de contraseña debe hacerse vía Supabase Auth"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun getDirecciones(idUsuario: Int): Result<List<DireccionGuardada>> {
        return try {
            val array = ApiClient.getArray(
                context,
                "/rest/v1/direcciones?id_usuario=eq.$idUsuario&select=*&order=id_direccion.desc"
            )
            val direcciones = buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        DireccionGuardada(
                            alias = item.optString("alias").ifBlank { "Direccion" },
                            nombreCompleto = item.optString("nombre_completo"),
                            telefono = item.optString("telefono"),
                            direccion = item.optString("calle"),
                            ciudad = item.optString("ciudad"),
                            codigoPostal = item.optString("cod_postal"),
                            provincia = item.optString("provincia").ifBlank { item.optString("pais") },
                            idDireccion = item.optInt("id_direccion").takeIf { it > 0 }
                        )
                    )
                }
            }
            Result.success(direcciones)
        } catch (_: Exception) {
            Result.failure(Exception("No se pudieron cargar las direcciones"))
        }
    }

    suspend fun guardarDireccion(idUsuario: Int, direccion: DireccionGuardada): Result<DireccionGuardada> {
        return try {
            val body = JSONObject().apply {
                put("id_usuario", idUsuario)
                put("calle", direccion.direccion)
                put("ciudad", direccion.ciudad)
                put("cod_postal", direccion.codigoPostal)
                put("pais", direccion.provincia.ifBlank { "Espana" })
                put("alias", direccion.alias)
                put("nombre_completo", direccion.nombreCompleto)
                put("telefono", direccion.telefono)
                put("provincia", direccion.provincia)
            }
            val array = ApiClient.postArray(context, "/rest/v1/direcciones", body)
            val first = array.optJSONObject(0)
            if (first == null || first.has("code") || first.has("message")) {
                Result.failure(Exception(first?.optString("message", "No se pudo guardar la direccion") ?: "No se pudo guardar la direccion"))
            } else {
                Result.success(
                    direccion.copy(
                        idDireccion = first.optInt("id_direccion").takeIf { it > 0 }
                    )
                )
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo guardar la direccion"))
        }
    }

    suspend fun eliminarDireccion(idUsuario: Int, direccion: DireccionGuardada): Result<Unit> {
        return try {
            val endpoint = direccion.idDireccion?.let {
                "/rest/v1/direcciones?id_usuario=eq.$idUsuario&id_direccion=eq.$it"
            } ?: run {
                val calle = URLEncoder.encode(direccion.direccion, "UTF-8")
                val ciudad = URLEncoder.encode(direccion.ciudad, "UTF-8")
                val codigoPostal = URLEncoder.encode(direccion.codigoPostal, "UTF-8")
                "/rest/v1/direcciones?id_usuario=eq.$idUsuario&calle=eq.$calle&ciudad=eq.$ciudad&cod_postal=eq.$codigoPostal"
            }
            val code = ApiClient.delete(context, endpoint)
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("No se pudo eliminar la direccion"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo eliminar la direccion"))
        }
    }

    suspend fun getUsuarios(): Result<List<UsuarioBD>> {
        return try {
            val array = ApiClient.getArray(
                context,
                "/rest/v1/usuarios?select=*,roles(nombre_rol)&order=id_usuario.asc"
            )
            val usuarios = buildList {
                for (i in 0 until array.length()) {
                    add(parseUsuario(array.getJSONObject(i)))
                }
            }
            Result.success(usuarios)
        } catch (_: Exception) {
            Result.failure(Exception("No se pudieron cargar los usuarios"))
        }
    }

    suspend fun actualizarUsuario(usuario: UsuarioBD): Result<Unit> {
        return try {
            val body = JSONObject().apply {
                put("nombre", usuario.nombre)
                put("correo", usuario.correo)
                put("telefono", usuario.telefono)
                // NO actualizar contraseña aquí
            }
            val code = ApiClient.patch(context, "/rest/v1/usuarios?id_usuario=eq.${usuario.id_usuario}", body)
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("No se pudo actualizar el usuario"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo actualizar el usuario"))
        }
    }

    suspend fun desactivarUsuario(idUsuario: Int): Result<Unit> {
        return try {
            val body = JSONObject().apply { put("activo", false) }
            val code = ApiClient.patch(context, "/rest/v1/usuarios?id_usuario=eq.$idUsuario", body)
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("No se pudo desactivar la cuenta. Añade la columna activo a Usuarios."))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo desactivar la cuenta. Añade la columna activo a Usuarios."))
        }
    }

    suspend fun eliminarUsuario(idUsuario: Int): Result<Unit> {
        return try {
            val productIds = getIds(
                "/rest/v1/productos?id_vendedor=eq.$idUsuario&select=id_producto",
                "id_producto"
            )
            val variationIds = if (productIds.isNotEmpty()) {
                getIds(
                    "/rest/v1/variaciones?id_producto=in.(${productIds.joinToString(",")})&select=id_variacion",
                    "id_variacion"
                )
            } else {
                emptyList()
            }

            val addressIds = getIds(
                "/rest/v1/direcciones?id_usuario=eq.$idUsuario&select=id_direccion",
                "id_direccion"
            )
            val pedidoIdsComprador = getIds(
                "/rest/v1/pedidos?id_comprador=eq.$idUsuario&select=id_pedido",
                "id_pedido"
            )
            val pedidoIdsDireccion = if (addressIds.isNotEmpty()) {
                getIds(
                    "/rest/v1/pedidos?id_direccion_envio=in.(${addressIds.joinToString(",")})&select=id_pedido",
                    "id_pedido"
                )
            } else {
                emptyList()
            }
            val pedidoIds = (pedidoIdsComprador + pedidoIdsDireccion).distinct()

            if (variationIds.isNotEmpty()) {
                ApiClient.delete(context, "/rest/v1/carrito_items?id_variacion=in.(${variationIds.joinToString(",")})")
                ApiClient.delete(context, "/rest/v1/variaciones?id_variacion=in.(${variationIds.joinToString(",")})")
            }

            if (productIds.isNotEmpty()) {
                ApiClient.delete(context, "/rest/v1/favoritos?id_producto=in.(${productIds.joinToString(",")})")
                ApiClient.delete(context, "/rest/v1/productos?id_producto=in.(${productIds.joinToString(",")})")
            }

            if (pedidoIds.isNotEmpty()) {
                ApiClient.delete(context, "/rest/v1/pagos?id_pedido=in.(${pedidoIds.joinToString(",")})")
                ApiClient.delete(context, "/rest/v1/pedidos?id_pedido=in.(${pedidoIds.joinToString(",")})")
            }

            ApiClient.delete(context, "/rest/v1/carrito_items?id_usuario=eq.$idUsuario")
            ApiClient.delete(context, "/rest/v1/favoritos?id_usuario=eq.$idUsuario")

            if (addressIds.isNotEmpty()) {
                ApiClient.delete(context, "/rest/v1/direcciones?id_direccion=in.(${addressIds.joinToString(",")})")
            }

            val code = ApiClient.delete(context, "/rest/v1/usuarios?id_usuario=eq.$idUsuario")
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("No se pudo eliminar la cuenta"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo eliminar la cuenta"))
        }
    }

    suspend fun getOfferThreads(idUsuario: Int): Result<List<OfferThread>> {
        return try {
            val endpoint = "/rest/v1/oferta_conversaciones" +
                    "?or=(id_comprador.eq.$idUsuario,id_vendedor.eq.$idUsuario)" +
                    "&select=*,oferta_mensajes(*)" +
                    "&order=created_at.desc"
            val array = ApiClient.getArray(context, endpoint)
            val threads = buildList {
                for (i in 0 until array.length()) {
                    add(parseOfferThread(array.getJSONObject(i)))
                }
            }
            Result.success(threads.sortedByDescending { it.latestOffer()?.createdAt ?: 0L })
        } catch (_: Exception) {
            Result.failure(Exception("No se pudieron cargar las ofertas"))
        }
    }

    suspend fun ensureOfferThread(producto: Producto, comprador: UsuarioBD): Result<String> {
        return try {
            val lookup = "/rest/v1/oferta_conversaciones" +
                    "?id_producto=eq.${producto.id}" +
                    "&id_comprador=eq.${comprador.id_usuario}" +
                    "&id_vendedor=eq.${producto.idVendedor}" +
                    "&select=id_conversacion" +
                    "&limit=1"
            val existing = ApiClient.getArray(context, lookup)
            if (existing.length() > 0) {
                return Result.success(existing.getJSONObject(0).getLong("id_conversacion").toString())
            }

            val body = JSONObject().apply {
                put("id_producto", producto.id)
                put("nombre_producto", producto.nombre)
                put("precio_base", producto.precio)
                put("foto_url", producto.fotoUrl)
                put("id_comprador", comprador.id_usuario)
                put("nombre_comprador", comprador.nombre)
                put("id_vendedor", producto.idVendedor)
                put("nombre_vendedor", producto.nombreVendedor)
                put("created_at", System.currentTimeMillis())
                put("updated_at", System.currentTimeMillis())
            }
            val created = ApiClient.postArray(context, "/rest/v1/oferta_conversaciones", body)
            val first = created.optJSONObject(0)
            if (first == null || first.has("code") || first.has("message")) {
                val refetched = ApiClient.getArray(context, lookup)
                if (refetched.length() > 0) {
                    Result.success(refetched.getJSONObject(0).getLong("id_conversacion").toString())
                } else {
                    Result.failure(Exception(first?.optString("message", "No se pudo crear la conversación") ?: "No se pudo crear la conversación"))
                }
            } else {
                Result.success(first.getLong("id_conversacion").toString())
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo preparar la conversación"))
        }
    }

    suspend fun createOffer(threadId: String, buyerId: Int, amount: Double): Result<Unit> {
        return try {
            val body = JSONObject().apply {
                put("id_conversacion", threadId.toLong())
                put("id_comprador", buyerId)
                put("importe", amount)
                put("estado", OfferStatus.PENDING.name)
                put("created_at", System.currentTimeMillis())
            }
            val created = ApiClient.postArray(context, "/rest/v1/oferta_mensajes", body)
            val first = created.optJSONObject(0)
            if (first != null && (first.has("code") || first.has("message"))) {
                Result.failure(Exception(first.optString("message", "No se pudo enviar la oferta")))
            } else {
                val patch = JSONObject().apply { put("updated_at", System.currentTimeMillis()) }
                ApiClient.patch(context, "/rest/v1/oferta_conversaciones?id_conversacion=eq.$threadId", patch)
                Result.success(Unit)
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo enviar la oferta"))
        }
    }

    suspend fun updateOfferStatus(offerId: String, threadId: String, status: OfferStatus): Result<Unit> {
        return try {
            val body = JSONObject().apply { put("estado", status.name) }
            val code = ApiClient.patch(context, "/rest/v1/oferta_mensajes?id_oferta=eq.$offerId", body)
            if (code in 200..299) {
                val patch = JSONObject().apply { put("updated_at", System.currentTimeMillis()) }
                ApiClient.patch(context, "/rest/v1/oferta_conversaciones?id_conversacion=eq.$threadId", patch)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo actualizar la oferta"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo actualizar la oferta"))
        }
    }

    suspend fun verificarOtp(correo: String, codigo: String): Result<UsuarioBD> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val tipos = listOf("email", "signup")

                for (tipo in tipos) {
                    val verifyBody = JSONObject().apply {
                        put("type", tipo)
                        put("email", correo)
                        put("token", codigo)
                    }

                    android.util.Log.e("OTP_DEBUG", "Intentando tipo='$tipo' token='$codigo' email='$correo'")

                    val verifyUrl = "${ApiClient.getBaseUrl(context)}/auth/v1/verify"
                    val conn = (URL(verifyUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 40_000
                        readTimeout = 40_000
                        setRequestProperty("apikey", ApiClient.SUPABASE_ANON_KEY)
                        setRequestProperty("Content-Type", "application/json")
                    }
                    conn.outputStream.use { it.write(verifyBody.toString().toByteArray()) }
                    val code = conn.responseCode
                    val responseText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    conn.disconnect()

                    android.util.Log.e("OTP_DEBUG", "tipo='$tipo' -> HTTP $code | Body: $responseText")

                    if (code in 200..299) {
                        val correoEnc = URLEncoder.encode(correo, "UTF-8")
                        val array = ApiClient.getArray(
                            context,
                            "/rest/v1/usuarios?correo=eq.$correoEnc&select=*,roles(nombre_rol)&limit=1"
                        )
                        return@withContext if (array.length() == 0) {
                            Result.failure(Exception("Usuario no encontrado tras verificar"))
                        } else {
                            Result.success(parseUsuario(array.getJSONObject(0)))
                        }
                    }
                }

                Result.failure(Exception("Código incorrecto o expirado"))
            } catch (e: Exception) {
                android.util.Log.e("OTP_DEBUG", "Excepción: ${e.javaClass.simpleName} - ${e.message}")
                Result.failure(Exception("Código incorrecto o expirado"))
            }
        }


    private suspend fun getIds(endpoint: String, field: String): List<Int> {
        return try {
            val array = ApiClient.getArray(context, endpoint)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i).getInt(field))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun subirFotos(idProducto: Int, fotoUris: List<Uri>): Result<List<String>> {
        val urls = mutableListOf<String>()
        fotoUris.forEachIndexed { index, uri ->
            val result = subirFoto(idProducto, uri, esPrincipal = index == 0)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("No se pudo subir una foto"))
            }
            urls.add(result.getOrThrow())
        }
        return Result.success(urls)
    }

    private fun profilePhotoObjectPath(userId: Int): String = "profiles/profile_$userId"

    private fun profilePhotoPublicUrl(userId: Int): String =
        "${ApiClient.getBaseUrl(context)}/storage/v1/object/public/fotos-productos/${profilePhotoObjectPath(userId)}"

    private fun cacheBustedProfilePhotoUrl(userId: Int): String =
        "${profilePhotoPublicUrl(userId)}?t=${System.currentTimeMillis()}"

    private fun remoteFileExists(url: String): Boolean {
        val methods = listOf("HEAD", "GET")
        methods.forEach { method ->
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val code = conn.responseCode
                if (method == "GET") {
                    runCatching { conn.inputStream?.close() }
                    runCatching { conn.errorStream?.close() }
                }
                conn.disconnect()
                code in 200..299
            }.getOrNull()?.let { if (it) return true }
        }
        return false
    }

    private fun parseOfferThread(json: JSONObject): OfferThread {
        val offersJson = json.optJSONArray("oferta_mensajes")
        val offers = buildList {
            if (offersJson != null) {
                for (i in 0 until offersJson.length()) {
                    val item = offersJson.getJSONObject(i)
                    add(
                        OfferEntry(
                            id = item.optLong("id_oferta").toString(),
                            amount = item.optDouble("importe"),
                            buyerId = item.optInt("id_comprador"),
                            createdAt = item.optLong("created_at"),
                            status = runCatching {
                                OfferStatus.valueOf(item.optString("estado", OfferStatus.PENDING.name))
                            }.getOrDefault(OfferStatus.PENDING)
                        )
                    )
                }
            }
        }.sortedBy { it.createdAt }

        return OfferThread(
            threadId = json.optLong("id_conversacion").toString(),
            productId = json.optInt("id_producto"),
            productName = json.optString("nombre_producto"),
            productBasePrice = json.optDouble("precio_base"),
            productPhotoUrl = json.optString("foto_url").takeIf { it.isNotBlank() },
            sellerId = json.optInt("id_vendedor"),
            sellerName = json.optString("nombre_vendedor"),
            buyerId = json.optInt("id_comprador"),
            buyerName = json.optString("nombre_comprador"),
            offers = offers
        )
    }

    private suspend fun subirFoto(idProducto: Int, fotoUri: Uri, esPrincipal: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fotoBytes = context.contentResolver.openInputStream(fotoUri)?.readBytes()
                    ?: return@withContext Result.failure(Exception("No se pudo leer la foto"))

                val mimeType = context.contentResolver.getType(fotoUri)?.takeIf { it.isNotBlank() } ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val fileName = "producto_${idProducto}_${System.currentTimeMillis()}.$extension"
                val bucketUrl = "${ApiClient.getBaseUrl(context)}/storage/v1/object/fotos-productos/$fileName"

                val conn = (URL(bucketUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    setRequestProperty("apikey", ApiClient.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${ApiClient.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", mimeType)
                    setRequestProperty("x-upsert", "true")
                }

                conn.outputStream.use { it.write(fotoBytes) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                conn.disconnect()

                if (code in 200..299) {
                    val publicUrl = "${ApiClient.getBaseUrl(context)}/storage/v1/object/public/fotos-productos/$fileName"
                    val body = JSONObject().apply {
                        put("id_producto", idProducto)
                        put("url_foto", publicUrl)
                        put("es_principal", esPrincipal)
                    }
                    val insertResult = ApiClient.postArray(context, "/rest/v1/fotos_producto", body)
                    val insertError = insertResult.optJSONObject(0)
                    if (insertError != null && (insertError.has("code") || insertError.has("message"))) {
                        return@withContext Result.failure(
                            Exception(insertError.optString("message", "No se pudo guardar la foto del producto"))
                        )
                    }
                    Result.success(publicUrl)
                } else {
                    Result.failure(Exception("Error al subir foto: $code ${text.take(180)}".trim()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseUsuario(json: JSONObject): UsuarioBD {
        val rol = json.optJSONObject("roles")
        return UsuarioBD(
            id_usuario = json.getInt("id_usuario"),
            nombre = json.optString("nombre"),
            correo = json.optString("correo"),
            telefono = json.optString("telefono"),
            activo = if (json.has("activo") && !json.isNull("activo")) json.optBoolean("activo") else null,
            id_rol = json.optInt("id_rol", 2),
            nombre_rol = rol?.optString("nombre_rol") ?: ""
        )
    }

    private fun extraerFotos(fotos: JSONArray?): List<String> {
        if (fotos == null) return emptyList()
        val urls = mutableListOf<String>()
        for (i in 0 until fotos.length()) {
            val foto = fotos.getJSONObject(i)
            val url = foto.optString("url_foto").takeIf { it.isNotBlank() } ?: continue
            urls.add(url)
        }
        return urls
    }

    private fun encontrarFotoPrincipal(fotos: JSONArray?): String? {
        if (fotos == null) return null
        for (i in 0 until fotos.length()) {
            val foto = fotos.getJSONObject(i)
            if (foto.optBoolean("es_principal", false)) {
                return foto.optString("url_foto").takeIf { it.isNotBlank() }
            }
        }
        return fotos.optJSONObject(0)?.optString("url_foto")?.takeIf { it.isNotBlank() }
    }

    private fun containsMissingSellerNames(array: JSONArray): Boolean {
        for (i in 0 until array.length()) {
            val producto = array.getJSONObject(i)
            val vendedor = producto.optJSONObject("usuarios") ?: producto.optJSONObject("Usuarios")
            val nombre = vendedor?.optString("nombre").orEmpty().trim()
            if (nombre.isBlank()) return true
        }
        return false
    }

    private suspend fun fetchUserNamesById(): Map<Int, String> {
        val endpoints = listOf(
            "/rest/v1/usuarios?select=id_usuario,nombre",
            "/rest/v1/Usuarios?select=id_usuario,nombre"
        )

        for (endpoint in endpoints) {
            runCatching {
                val array = ApiClient.getArray(context, endpoint)
                buildMap {
                    for (i in 0 until array.length()) {
                        val usuario = array.getJSONObject(i)
                        val id = usuario.optInt("id_usuario", -1)
                        val nombre = usuario.optString("nombre").trim()
                        if (id > 0 && nombre.isNotBlank()) put(id, nombre)
                    }
                }
            }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        return emptyMap()
    }

    private fun parseProductos(array: JSONArray, userNamesById: Map<Int, String> = emptyMap()): List<ProductoBD> {
        val lista = mutableListOf<ProductoBD>()
        for (i in 0 until array.length()) {
            val p = array.getJSONObject(i)
            val fotosJson = p.optJSONArray("fotos_producto")
            val fotos = extraerFotos(fotosJson)
            val vendedor = p.optJSONObject("usuarios") ?: p.optJSONObject("Usuarios")
            val idVendedor = p.getInt("id_vendedor")
            val nombreVendedor = vendedor?.optString("nombre").orEmpty().trim()
                .ifBlank { userNamesById[idVendedor].orEmpty() }

            lista.add(
                ProductoBD(
                    idProducto = p.getInt("id_producto"),
                    idVendedor = idVendedor,
                    idCategoria = if (p.isNull("id_categoria")) null else p.getInt("id_categoria"),
                    nombre = p.getString("nombre"),
                    descripcion = p.optString("descripcion", ""),
                    precioBase = p.getDouble("precio_base"),
                    estadoPrenda = p.optString("estado_prenda", ""),
                    fechaPublicacion = p.optString("fecha_publicacion", ""),
                    fotoPrincipal = encontrarFotoPrincipal(fotosJson),
                    fotos = fotos,
                    nombreVendedor = nombreVendedor
                )
            )
        }
        return lista
    }
}
