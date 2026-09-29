package com.barboza.bingomanager.controller

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.barboza.bingomanager.model.SpokenNumberParser

/** Controla sesiones cortas y consecutivas del reconocimiento local de voz. */
class SpeechNumberController(
    context: Context,
    private val numberRange: () -> IntRange,
    private val onCandidate: (Int) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onStopped: () -> Unit = {},
) : RecognitionListener {
    private enum class Engine { ON_DEVICE, SYSTEM }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val spanishLanguages = listOf("es-CR", "es-ES", "es-US")
    private var recognizer: SpeechRecognizer? = null
    private var engine = Engine.SYSTEM
    private var languageIndex = 0
    private var keepListening = false
    private var cycleActive = false

    fun start(): Boolean {
        if (!ensureRecognizer()) return false
        languageIndex = 0
        keepListening = true
        onStatus(
            if (engine == Engine.ON_DEVICE) {
                "Activando reconocimiento local…"
            } else {
                "Activando reconocimiento del teléfono…"
            },
        )
        startCycle()
        return true
    }

    private fun ensureRecognizer(): Boolean {
        if (recognizer != null) return true

        val localAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        if (localAvailable) {
            val localRecognizer = runCatching {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            }.getOrNull()
            if (localRecognizer != null) {
                engine = Engine.ON_DEVICE
                recognizer = localRecognizer.also { it.setRecognitionListener(this) }
                return true
            }
        }

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onStatus("No hay un servicio de reconocimiento de voz instalado. Actualiza la app de Google.")
            onStopped()
            return false
        }
        val systemRecognizer = runCatching {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }.getOrElse {
            onStatus("No se pudo iniciar el reconocimiento de voz del teléfono.")
            onStopped()
            return false
        }
        engine = Engine.SYSTEM
        recognizer = systemRecognizer.also { it.setRecognitionListener(this) }
        return true
    }

    fun stop() {
        keepListening = false
        cycleActive = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        onStatus("Micrófono apagado")
    }

    fun destroy() {
        keepListening = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun startCycle() {
        if (!keepListening || cycleActive) return
        cycleActive = true
        val language = spanishLanguages[languageIndex]
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (engine == Engine.ON_DEVICE) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
        recognizer?.startListening(intent)
    }

    private fun restart(delayMillis: Long = 350L) {
        cycleActive = false
        if (keepListening) handler.postDelayed({ startCycle() }, delayMillis)
    }

    private fun handle(bundle: Bundle?) {
        val candidate = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.asSequence()
            ?.mapNotNull(SpokenNumberParser::parse)
            ?.firstOrNull { it in numberRange() }
        if (candidate != null) {
            onCandidate(candidate)
            onStatus("Número escuchado; toca la bolita para confirmarlo")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = onStatus(
        if (engine == Engine.ON_DEVICE) "Escuchando localmente…" else "Escuchando…",
    )
    override fun onBeginningOfSpeech() = onStatus("Escuchando número…")
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = onStatus("Interpretando…")
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            keepListening = false
            cycleActive = false
            onStatus("Falta permiso para usar el micrófono")
            onStopped()
        } else if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        ) {
            if (languageIndex < spanishLanguages.lastIndex) {
                languageIndex++
                onStatus("Probando otra variante de español…")
                restart(300L)
            } else if (engine == Engine.ON_DEVICE && switchToSystemRecognizer()) {
                languageIndex = 0
                onStatus("Usando reconocimiento alternativo en español…")
                restart(350L)
            } else {
                keepListening = false
                cycleActive = false
                onStatus("El servicio de voz del teléfono no admite español")
                onStopped()
            }
        } else {
            onStatus(if (keepListening) "Escuchando…" else "Micrófono apagado")
            restart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 900L else 450L)
        }
    }

    private fun switchToSystemRecognizer(): Boolean {
        recognizer?.destroy()
        recognizer = null
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return false
        val systemRecognizer = runCatching {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }.getOrNull() ?: return false
        engine = Engine.SYSTEM
        recognizer = systemRecognizer.also { it.setRecognitionListener(this) }
        return true
    }

    override fun onResults(results: Bundle?) {
        handle(results)
        restart()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        handle(partialResults)
    }
}
