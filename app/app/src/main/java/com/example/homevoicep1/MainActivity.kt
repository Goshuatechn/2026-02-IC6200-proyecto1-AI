package com.example.homevoicep1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =====================================================
// MAIN ACTIVITY: arranque de la app, permiso de micrófono
// y conexión entre el modelo y state.handle()
// =====================================================
class MainActivity : ComponentActivity() {

    private val state = SmartHomeState()

    // null si el modelo no se pudo cargar (los botones siguen funcionando igual)
    private var classifier: VoiceClassifier? = null

    // Pide el permiso de micrófono; si lo aceptan, empezamos a escuchar
    private val askMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                listen()
            } else {
                state.message = "Sin permiso de micrófono. Usa los botones."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            classifier = VoiceClassifier(this)
        } catch (e: Exception) {
            state.message = "No se pudo cargar el modelo: ${e.message}"
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SmartHomeApp(state, onSpeak = { onSpeakPressed() })
            }
        }
    }

    // Botón "Hablar": revisamos el permiso antes de grabar
    private fun onSpeakPressed() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            listen()
        } else {
            askMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Graba 1 s, corre el modelo y le pasa el resultado al estado
    private fun listen() {
        val c = classifier
        if (c == null) {
            state.message = "El modelo no está cargado. Usa los botones."
            return
        }
        if (state.isListening) return  // ya estamos grabando

        state.isListening = true
        state.message = "Escuchando... di un comando"

        lifecycleScope.launch {
            try {
                // Grabar y clasificar en segundo plano para no congelar la pantalla
                val prediction = withContext(Dispatchers.Default) { c.listenOnce() }
                state.onVoiceResult(prediction.word, prediction.confidence)
            } catch (e: Exception) {
                state.message = "Error con el micrófono: ${e.message}"
            } finally {
                state.isListening = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier?.close()
    }
}