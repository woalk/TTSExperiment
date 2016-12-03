package com.woalk.apps.ttsexperiment;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.HashMap;

/**
 * A compatibility helper class to deal with API specific implementations in the
 * {@link TextToSpeech} class.
 *
 * @see TextToSpeech
 */
public class TTSCompat {
    private TTSCompat() {
    }

    private static int speakId = 0;

    /**
     * Speaks the text using the specified queuing strategy and speech parameters, the text may be
     * spanned with TtsSpans. This method is asynchronous, i.e. the method just adds the request to
     * the queue of TTS requests and then returns. The synthesis might not have finished (or even
     * started!) at the time when this method returns.
     *
     * @param ttsInstance  The {@link TextToSpeech} instance to use.
     * @param text         The {@link String} of text to be spoken. No longer than
     *                     {@link TextToSpeech#getMaxSpeechInputLength()} characters.
     * @param ttsQueueMode The queuing strategy to use, {@link TextToSpeech#QUEUE_ADD} or
     *                     {@link TextToSpeech#QUEUE_FLUSH}.
     * @return {@link TextToSpeech#ERROR} or {@link TextToSpeech#SUCCESS} of queuing the speak
     * operation.
     * @see TextToSpeech#speak(CharSequence, int, Bundle, String)
     * @see TextToSpeech#speak(String, int, HashMap)
     */
    public static int speak(TextToSpeech ttsInstance, CharSequence text, int ttsQueueMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String id = String.valueOf(speakId++);
            return ttsInstance.speak(text, ttsQueueMode, null, id);
        } else {
            //noinspection deprecation
            return ttsInstance.speak(text.toString(), ttsQueueMode, null);
        }
    }

    /**
     * Call {@link TextToSpeech#setVoice(Voice)} if this is run on a device with Lollipop or higher.
     *
     * @param ttsInstance The {@link TextToSpeech} instance to use.
     * @param voice       The {@link Voice} to set the TTS instance to.
     * @see TextToSpeech#setVoice(Voice)
     */
    public static void setVoiceIfAvailable(TextToSpeech ttsInstance, Voice voice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && voice != null) {
            ttsInstance.setVoice(voice);
        }
    }
}
