/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;

/** AudioRecorderThread */
public final class AudioRecorderThread extends ConrealityThread {
  @Nullable ByteBuffer buffer;
  @Nullable AudioRecord recorder;

  public AudioRecorderThread() {
    super("AudioRecorderThread");

    final int bufferSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLE_RATE,
        AudioConfig.CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "AudioRecorderThread: bufferSize=" + bufferSize);
    }
    this.buffer = ByteBuffer.allocateDirect(bufferSize);

    this.recorder = new AudioRecord(AudioConfig.SOURCE, AudioConfig.SAMPLE_RATE,
        AudioConfig.CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT, bufferSize);
  }

  @WorkerThread
  @Override
  public void run() {
    Log.d(TAG, "AudioRecorderThread.start");
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

    try {
      this.recorder.startRecording();

      while (!this.isInterrupted()) {
        this.buffer.clear();
        final int len = this.recorder.read(this.buffer, this.buffer.capacity());
        if (len >= 0 && len <= this.buffer.capacity()) {
          //Log.d(TAG, "Expected length returned: " + len); // TODO
        }
        else {
          Log.w(TAG, "Unexpected audio sample length returned: " + len);
        }
      }
    }
    //catch (final IOException error) {}
    //catch (final InterruptedException error) {}
    //catch (final ClosedByInterruptException error) {}
    finally {
      Log.d(TAG, "AudioRecorderThread.stop");
      try {
        this.recorder.stop();
      }
      catch (final IllegalStateException error) {
        Log.e(TAG, "Failed to stop audio recording.", error);
      }
      this.recorder.release();
      this.recorder = null;
      this.buffer = null;
    }
  }
}
