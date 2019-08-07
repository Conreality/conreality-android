/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;

/** AudioPlayerThread */
public final class AudioPlayerThread extends ConrealityThread {
  private static final int SAMPLE_RATE = 44100; // Hz
  private static final int CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO;
  private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

  final @NonNull InputStream input;
  @Nullable byte[] buffer;
  @Nullable AudioTrack player;

  public AudioPlayerThread(final @NonNull File path) throws FileNotFoundException {
    this(new FileInputStream(Objects.requireNonNull(path)));
  }

  public AudioPlayerThread(final @NonNull InputStream input) {
    super("AudioPlayerThread");

    this.input = Objects.requireNonNull(input);

    final int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, AUDIO_FORMAT);
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "AudioPlayerThread: bufferSize=" + bufferSize);
    }
    this.buffer = new byte[bufferSize];

    final AudioAttributes audioAttributes =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build();
    final AudioFormat audioFormat =
        new AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_MASK)
            .setEncoding(AUDIO_FORMAT)
            .build();
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Android 6.0+
      this.player = new AudioTrack.Builder()
          .setAudioAttributes(audioAttributes)
          .setAudioFormat(audioFormat)
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          //.setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
          .build();
    }
    else { // Android 5.0+
      this.player = new AudioTrack(audioAttributes, audioFormat, bufferSize,
          AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }
  }

  @WorkerThread
  @Override
  public void run() {
    Log.d(TAG, "AudioPlayerThread.start");
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

    try {
      this.player.play();

      int len;
      while (!this.isInterrupted() && (len = this.input.read(this.buffer)) > 0) {
        this.player.write(this.buffer, 0, len); // FIXME: check return value
      }
    }
    catch (final IOException error) {
      Log.e(TAG, "Failed to play audio.", error);
    }
    //catch (final InterruptedException error) {}
    //catch (final ClosedByInterruptException error) {}
    finally {
      Log.d(TAG, "AudioPlayerThread.stop");
      this.player.stop();
      try {
        this.input.close();
      }
      catch (final IOException error) {}
      this.player.release();
      this.player = null;
      this.buffer = null;
    }
  }
}
