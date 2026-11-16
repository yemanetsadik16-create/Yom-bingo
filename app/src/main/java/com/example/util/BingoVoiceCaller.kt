package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class BingoVoiceCaller(context: Context) {
  private var tts: TextToSpeech? = null
  private var ready = false

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.US
        tts?.setSpeechRate(0.92f)
        ready = true
      }
    }
  }

  fun speak(text: String) {
    if (!ready) return
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bingo_call_${System.currentTimeMillis()}")
  }

  fun stop() {
    tts?.stop()
  }

  fun shutdown() {
    tts?.stop()
    tts?.shutdown()
    tts = null
    ready = false
  }
}
