/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.MediaRecorder;

/** AudioConfig */
public abstract class AudioConfig {
  public static final int SOURCE = MediaRecorder.AudioSource.DEFAULT;
  public static final int USAGE = AudioAttributes.USAGE_VOICE_COMMUNICATION;
  public static final int CONTENT_TYPE = AudioAttributes.CONTENT_TYPE_SPEECH;
  public static final int SAMPLE_RATE = 44100; // Hz
  public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
  public static final int CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO;
  public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
}
