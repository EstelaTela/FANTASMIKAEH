package com.example.fkaeh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfigScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    var urlActual by remember { mutableStateOf(ApiClient.getBaseUrl(context)) }
    var urlInput by remember { mutableStateOf(urlActual) }
    var guardado by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚙️ Configuración del servidor",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Introduce la URL del backend.\nPuede ser tu IP local o una URL de ngrok.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it; guardado = false },
            label = { Text("URL del servidor") },
            placeholder = { Text("https://xxxx.ngrok-free.app") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ejemplos de URL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Ejemplos de URL:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Ngrok:  https://abcd1234.ngrok-free.app", fontSize = 12.sp)
                Text("• WiFi local:  http://192.168.1.X:3000", fontSize = 12.sp)
                Text("• Emulador:  http://10.0.2.2:3000", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                ApiClient.setBaseUrl(context, urlInput.trim())
                urlActual = ApiClient.getBaseUrl(context)
                guardado = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Guardar URL", fontSize = 16.sp)
        }

        if (guardado) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✅ URL guardada: $urlActual",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("← Volver")
        }
    }
}
