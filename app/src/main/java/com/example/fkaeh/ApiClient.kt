package com.example.fkaeh

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val PREFS_NAME = "fkaeh_config"
    private const val PREF_KEY_BASE_URL = "base_url"

    const val SUPABASE_ANON_KEY = "sb_publishable_tM2wcVU4iyHuDQRZYPCv2A_YcL5kZj_"
    const val DEFAULT_URL = "https://gomfmabmfytmrhvwmugh.supabase.co"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_BASE_URL, url.trimEnd('/'))
            .apply()
    }

    private fun HttpURLConnection.addCommonHeaders() {
        setRequestProperty("apikey", SUPABASE_ANON_KEY)
        setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
    }

    // GET → JSONArray (Supabase siempre devuelve array en consultas a tablas)
    suspend fun getArray(context: Context, endpoint: String): JSONArray =
        withContext(Dispatchers.IO) {
            val conn = (URL("${getBaseUrl(context)}$endpoint").openConnection()
                    as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                addCommonHeaders()
            }
            val code = conn.responseCode
            val text = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).readText()
            conn.disconnect()
            JSONArray(text)
        }

    // POST → JSONArray con Prefer: return=representation para obtener el registro creado
    suspend fun postArray(
        context: Context,
        endpoint: String,
        body: JSONObject
    ): JSONArray = withContext(Dispatchers.IO) {
        val conn = (URL("${getBaseUrl(context)}$endpoint").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            addCommonHeaders()
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=representation")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        val text = BufferedReader(InputStreamReader(
            if (code in 200..299) conn.inputStream else conn.errorStream
        )).readText()
        conn.disconnect()
        if (text.trimStart().startsWith("[")) JSONArray(text)
        else JSONArray().put(JSONObject(text))
    }

    // PATCH para actualizar filas
    suspend fun patch(
        context: Context,
        endpoint: String,
        body: JSONObject
    ): Int = withContext(Dispatchers.IO) {
        val conn = (URL("${getBaseUrl(context)}$endpoint").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            addCommonHeaders()
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        conn.disconnect()
        code
    }

    // DELETE
    suspend fun delete(context: Context, endpoint: String): Int =
        withContext(Dispatchers.IO) {
            val conn = (URL("${getBaseUrl(context)}$endpoint").openConnection()
                    as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 10_000
                readTimeout = 10_000
                addCommonHeaders()
            }
            val code = conn.responseCode
            conn.disconnect()
            code
        }
}
