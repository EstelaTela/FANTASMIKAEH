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

    suspend fun loginUsuario(correo: String, contrasena: String): Result<UsuarioBD> {
        return try {
            val correoEnc = URLEncoder.encode(correo, "UTF-8")
            val passEnc = URLEncoder.encode(contrasena, "UTF-8")
            val baseEndpoint = "/rest/v1/usuarios" +
                "?correo=eq.$correoEnc" +
                "&contrasena=eq.$passEnc" +
                "&select=*,roles(nombre_rol)" +
                "&limit=1"

            val array = try {
                ApiClient.getArray(context, "$baseEndpoint&activo=eq.true")
            } catch (_: Exception) {
                ApiClient.getArray(context, baseEndpoint)
            }

            if (array.length() == 0) {
                Result.failure(Exception("Credenciales incorrectas"))
            } else {
                Result.success(parseUsuario(array.getJSONObject(0)))
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        telefono: String
    ): Result<UsuarioBD> {
        return try {
            val body = JSONObject().apply {
                put("nombre", nombre)
                put("correo", correo)
                put("contrasena", contrasena)
                put("telefono", telefono)
                put("id_rol", 2)
            }
            val array = ApiClient.postArray(context, "/rest/v1/usuarios", body)

            if (array.length() == 0) {
                Result.failure(Exception("No se pudo crear el usuario"))
            } else {
                val u = array.getJSONObject(0)
                if (u.has("code") || u.has("message")) {
                    val msg = u.optString("message", "Error al registrar")
                    Result.failure(
                        Exception(
                            if (msg.contains("unique", true) || msg.contains("duplicate", true)) {
                                "Correo ya registrado"
                            } else {
                                msg
                            }
                        )
                    )
                } else {
                    Result.success(parseUsuario(u))
                }
            }
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun getAllProductosFromBD(): Result<List<ProductoBD>> {
        return try {
            fetchCategoryOptions()
            val endpointConVendedor = "/rest/v1/productos" +
                "?select=*,usuarios(nombre),fotos_producto(url_foto,es_principal)" +
                "&order=fecha_publicacion.desc"
            val endpointSimple = "/rest/v1/productos" +
                "?select=*,fotos_producto(url_foto,es_principal)" +
                "&order=fecha_publicacion.desc"

            val array = try {
                ApiClient.getArray(context, endpointConVendedor)
            } catch (_: Exception) {
                ApiClient.getArray(context, endpointSimple)
            }

            Result.success(parseProductos(array))
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

    suspend fun verificarParaRecuperar(correo: String, telefono: String): Result<Int> {
        return try {
            val correoEnc = URLEncoder.encode(correo, "UTF-8")
            val telefonoEnc = URLEncoder.encode(telefono, "UTF-8")
            val endpoint = "/rest/v1/usuarios" +
                "?correo=eq.$correoEnc" +
                "&telefono=eq.$telefonoEnc" +
                "&select=id_usuario" +
                "&limit=1"

            val array = ApiClient.getArray(context, endpoint)
            if (array.length() == 0) Result.failure(Exception("Usuario no encontrado"))
            else Result.success(array.getJSONObject(0).getInt("id_usuario"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        }
    }

    suspend fun verificarPasswordActual(idUsuario: Int, passwordActual: String): Result<Boolean> {
        return try {
            val passwordEnc = URLEncoder.encode(passwordActual, "UTF-8")
            val endpoint = "/rest/v1/usuarios" +
                "?id_usuario=eq.$idUsuario" +
                "&contrasena=eq.$passwordEnc" +
                "&select=id_usuario" +
                "&limit=1"

            val array = ApiClient.getArray(context, endpoint)
            Result.success(array.length() > 0)
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo verificar la contraseña actual"))
        }
    }

    suspend fun cambiarPassword(idUsuario: Int, nuevaContrasena: String): Result<Unit> {
        return try {
            val body = JSONObject().apply { put("contrasena", nuevaContrasena) }
            val code = ApiClient.patch(context, "/rest/v1/usuarios?id_usuario=eq.$idUsuario", body)
            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("Error al cambiar contraseña: $code"))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar al servidor"))
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

    private fun parseProductos(array: JSONArray): List<ProductoBD> {
        val lista = mutableListOf<ProductoBD>()
        for (i in 0 until array.length()) {
            val p = array.getJSONObject(i)
            val fotosJson = p.optJSONArray("fotos_producto")
            val fotos = extraerFotos(fotosJson)
            val vendedor = p.optJSONObject("usuarios")

            lista.add(
                ProductoBD(
                    idProducto = p.getInt("id_producto"),
                    idVendedor = p.getInt("id_vendedor"),
                    idCategoria = if (p.isNull("id_categoria")) null else p.getInt("id_categoria"),
                    nombre = p.getString("nombre"),
                    descripcion = p.optString("descripcion", ""),
                    precioBase = p.getDouble("precio_base"),
                    estadoPrenda = p.optString("estado_prenda", ""),
                    fechaPublicacion = p.optString("fecha_publicacion", ""),
                    fotoPrincipal = encontrarFotoPrincipal(fotosJson),
                    fotos = fotos,
                    nombreVendedor = vendedor?.optString("nombre").orEmpty()
                )
            )
        }
        return lista
    }
}
