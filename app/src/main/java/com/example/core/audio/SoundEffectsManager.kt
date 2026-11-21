package com.example.core.audio

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf

private const val TAG = "SoundEffectsManager"

/**
 * Plays Android's natural system button click sound (AudioManager.FX_KEY_CLICK)
 * matching standard system UI interactions across Android apps.
 */
class SoundEffectsManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    private var lastTapAtMs: Long = 0L

    fun playTap() {
        playNativeClick()
    }

    fun playCreate() {
        playNativeClick()
    }

    fun playAiMagic() {
        playNativeClick()
    }

    fun playSuccess() {
        playNativeClick()
    }

    private fun playNativeClick() {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTapAtMs < 55L) return
        lastTapAtMs = now
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Native click sound failed: ${e.message}")
        }
    }

    fun release() {
        // No allocated SoundPool/AudioTrack resources to release
    }
}

val LocalSoundEffects = staticCompositionLocalOf<SoundEffectsManager?> { null }
