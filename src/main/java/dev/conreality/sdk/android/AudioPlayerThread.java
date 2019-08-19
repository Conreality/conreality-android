/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

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
  final @NonNull InputStream input;
  @Nullable byte[] buffer;
  @Nullable AudioTrack player;

  public AudioPlayerThread(final @NonNull File path) throws FileNotFoundException {
    this(new FileInputStream(Objects.requireNonNull(path)));
  }

  public AudioPlayerThread(final @NonNull InputStream input) {
    super("AudioPlayerThread"); // up to 15 characters shown

    this.input = Objects.requireNonNull(input);

    final int bufferSize = AudioTrack.getMinBufferSize(
        AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_MASK, AudioConfig.AUDIO_FORMAT);

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "AudioPlayerThread: bufferSize=" + bufferSize);
    }
    this.buffer = new byte[bufferSize];

    final AudioAttributes audioAttributes =
        new AudioAttributes.Builder()
            .setUsage(AudioConfig.USAGE)
            .setContentType(AudioConfig.CONTENT_TYPE)
            .build();
    final AudioFormat audioFormat =
        new AudioFormat.Builder()
            .setSampleRate(AudioConfig.SAMPLE_RATE)
            .setChannelMask(AudioConfig.CHANNEL_MASK)
            .setEncoding(AudioConfig.AUDIO_FORMAT)
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
