package com.example.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TtsStatus(
    val isReady: Boolean = false,
    val isPersianSupported: Boolean = false,
    val usingOnlinePersianVoice: Boolean = false,
    val availableVoices: List<VoiceInfo> = emptyList(),
    val statusMessageFa: String = "در حال آماده‌سازی صدای راهنمای فارسی..."
)

data class VoiceInfo(
    val name: String,
    val displayLabelFa: String,
    val localeTag: String
)

class VoiceGuidanceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentSpeechJob: Job? = null

    private val ttsCacheDir = File(context.cacheDir, "persian_tts").apply { mkdirs() }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow(TtsStatus())
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var enabled: Boolean = true
    private var volume: Float = 0.9f
    private var selectedVoiceName: String = ""
    private var lastSpokenMessage: String = ""
    private var lastSpokenTime: Long = 0L

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(initStatus: Int) {
        val engine = tts
        if (initStatus != TextToSpeech.SUCCESS || engine == null) {
            _status.value = TtsStatus(
                isReady = true,
                isPersianSupported = false,
                usingOnlinePersianVoice = true,
                statusMessageFa = "صدای فارسی آنلاین فعال است (موتور گفتار آفلاین یافت نشد)"
            )
            return
        }

        val persianLocale = Locale.forLanguageTag("fa-IR")
        val langResult = engine.setLanguage(persianLocale)
        val nativePersianAvailable = langResult != TextToSpeech.LANG_MISSING_DATA &&
            langResult != TextToSpeech.LANG_NOT_SUPPORTED

        val voicesList = runCatching {
            engine.voices
                ?.filter { v -> v.locale.language.equals("fa", ignoreCase = true) }
                ?.take(8)
                ?.map { v ->
                    VoiceInfo(
                        name = v.name,
                        displayLabelFa = "صدای فارسی دستگاه (${v.name.takeLast(12)})",
                        localeTag = v.locale.toLanguageTag()
                    )
                } ?: emptyList()
        }.getOrDefault(emptyList())

        if (nativePersianAvailable && selectedVoiceName.isNotBlank()) {
            applyVoice(selectedVoiceName)
        }

        _status.value = TtsStatus(
            isReady = true,
            isPersianSupported = nativePersianAvailable,
            usingOnlinePersianVoice = !nativePersianAvailable,
            availableVoices = voicesList,
            statusMessageFa = if (nativePersianAvailable) {
                "راهنمای صوتی فارسی آفلاین و آنلاین آماده است"
            } else {
                "صدای فارسی آنلاین (پخش مستقیم فارسی) فعال است"
            }
        )
    }

    fun updateSettings(enabled: Boolean, volume: Float, voiceName: String) {
        this.enabled = enabled
        this.volume = volume.coerceIn(0f, 1f)
        if (voiceName != this.selectedVoiceName) {
            this.selectedVoiceName = voiceName
            applyVoice(voiceName)
        }
        runCatching { mediaPlayer?.setVolume(this.volume, this.volume) }
        if (!enabled) {
            stop()
        }
    }

    private fun applyVoice(voiceName: String) {
        val engine = tts ?: return
        if (voiceName.isBlank()) return
        runCatching {
            val match: Voice? = engine.voices?.firstOrNull { it.name == voiceName }
            if (match != null && match.locale.language.equals("fa", ignoreCase = true)) {
                engine.voice = match
            }
        }
    }

    fun speak(message: String, force: Boolean = false) {
        if (!enabled) return
        val cleanText = message.trim()
        if (cleanText.isEmpty()) return

        val now = System.currentTimeMillis()
        if (!force && cleanText == lastSpokenMessage && (now - lastSpokenTime) < 7000L) {
            return
        }
        lastSpokenMessage = cleanText
        lastSpokenTime = now

        val st = _status.value
        if (st.isPersianSupported && tts != null) {
            // Device natively supports Persian TTS offline
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "rahnama_$now")
        } else {
            // Synthesize real Persian voice (tl=fa) and play via MediaPlayer (cached locally)
            speakOnlinePersian(cleanText)
        }
    }

    private fun speakOnlinePersian(text: String) {
        currentSpeechJob?.cancel()
        currentSpeechJob = scope.launch {
            val audioFile = getOrFetchPersianAudio(text) ?: return@launch
            playAudioFile(audioFile)
        }
    }

    private fun getOrFetchPersianAudio(text: String): File? {
        val hash = sha256(text)
        val cached = File(ttsCacheDir, "$hash.mp3")
        if (cached.exists() && cached.length() > 512L) {
            return cached
        }

        val url = "https://translate.google.com/translate_tts".toHttpUrl().newBuilder()
            .addQueryParameter("ie", "UTF-8")
            .addQueryParameter("tl", "fa")
            .addQueryParameter("client", "tw-ob")
            .addQueryParameter("q", text)
            .build()

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .header("Referer", "https://translate.google.com/")
            .get()
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                if (bytes.size < 512) return@use null
                cached.writeBytes(bytes)
                cached
            }
        }.getOrNull()
    }

    private fun playAudioFile(file: File) {
        scope.launch(Dispatchers.Main) {
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    setVolume(volume, volume)
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (mediaPlayer === mp) mediaPlayer = null
                    }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        if (mediaPlayer === mp) mediaPlayer = null
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener { mp -> mp.start() }
                }
            }
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    fun stop() {
        currentSpeechJob?.cancel()
        runCatching { tts?.stop() }
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stop()
        scope.cancel()
        runCatching { tts?.shutdown() }
        tts = null
    }
}
