package com.example.homevoicep1

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.nio.FloatBuffer
import kotlin.math.exp

// =====================================================
// CONTRATO DEL MODELO (debe coincidir con el entrenamiento)
// =====================================================

// Las 10 palabras, en el mismo orden que los logits del modelo
val LABELS = listOf("yes", "no", "up", "down", "left", "right", "on", "off", "stop", "go")

// Solo actuamos si el modelo está al menos 70 % seguro
const val CONFIDENCE_THRESHOLD = 0.70f

const val SAMPLE_RATE = 16000          // 16 kHz
const val NUM_SAMPLES = 16000          // 1 segundo de audio
const val MODEL_FILE = "smart_home_voice.onnx"   // en app/src/main/assets/
const val INPUT_NAME = "audio_in"      // entrada: forma [16000]
const val OUTPUT_NAME = "logits_out"   // salida: forma [1, 10]

// Resultado de una predicción
data class Prediction(val word: String, val confidence: Float)

// =====================================================
// GRABACIÓN + MODELO
// =====================================================
class VoiceClassifier(context: Context) {

    private val env = OrtEnvironment.getEnvironment()

    // Cargamos el .onnx desde assets una sola vez, al crear la clase
    private val session: OrtSession = run {
        val modelBytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    // Graba 1 segundo y devuelve la palabra detectada.
    // Tarda ~1 s, por eso se llama fuera del hilo principal.
    fun listenOnce(): Prediction {
        val audio = recordOneSecond()
        return classify(audio)
    }

    // ---------- 1. Grabar ----------
    // El permiso RECORD_AUDIO se revisa en MainActivity antes de llamar aquí.
    @SuppressLint("MissingPermission")
    private fun recordOneSecond(): FloatArray {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,  // audio sin filtros extra, más parecido al dataset
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, NUM_SAMPLES * 2)             // 2 bytes por muestra
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("No se pudo abrir el micrófono")
        }

        val pcm = ShortArray(NUM_SAMPLES)
        try {
            recorder.startRecording()
            // read() puede devolver menos muestras de las pedidas, así que repetimos hasta llenar
            var total = 0
            while (total < NUM_SAMPLES) {
                val n = recorder.read(pcm, total, NUM_SAMPLES - total)
                if (n <= 0) break
                total += n
            }
            recorder.stop()
        } finally {
            recorder.release()
        }

        // PCM16 -> float en [-1, 1], igual que torchaudio.load
        val audio = FloatArray(NUM_SAMPLES)
        for (i in 0 until NUM_SAMPLES) {
            audio[i] = pcm[i] / 32768f
        }
        return audio
    }

    // ---------- 2. Clasificar ----------
    private fun classify(audio: FloatArray): Prediction {
        // Tensor de forma [16000] (una dimensión, como pide el modelo)
        val shape = longArrayOf(NUM_SAMPLES.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(audio), shape).use { input ->
            session.run(mapOf(INPUT_NAME to input)).use { result ->
                // logits_out tiene forma [1, 10]: tomamos la primera fila
                @Suppress("UNCHECKED_CAST")
                val output = result.get(OUTPUT_NAME).get().value as Array<FloatArray>
                val probs = softmax(output[0])

                // Buscamos la clase con mayor probabilidad
                var best = 0
                for (i in probs.indices) {
                    if (probs[i] > probs[best]) best = i
                }
                return Prediction(LABELS[best], probs[best])
            }
        }
    }

    // Convierte logits en probabilidades que suman 1.
    // Restamos el máximo antes de exp() para evitar números enormes.
    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = FloatArray(logits.size)
        var sum = 0f
        for (i in logits.indices) {
            exps[i] = exp(logits[i] - max)
            sum += exps[i]
        }
        for (i in exps.indices) {
            exps[i] = exps[i] / sum
        }
        return exps
    }

    fun close() {
        session.close()
    }
}