/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import android.media.AudioRecord;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import java.nio.ByteBuffer;

/** Audio */
public final class Audio {
  private static final String TAG = "ConrealitySDK";

  private Audio() {}

  public static Observable<AudioFrame> record() {
    return Observable.create((final @NonNull ObservableEmitter<AudioFrame> emitter) -> {
      Log.d(TAG, "Audio.record start");

      Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

      // Determine and allocate the audio buffer size:
      final int bufferSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLE_RATE,
          AudioConfig.CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Audio.record: bufferSize=" + bufferSize);
      }
      final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

      // Construct the recorder using our preferred audio configuration:
      AudioRecord recorder;
      try {
        recorder = new AudioRecord(AudioConfig.SOURCE, AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT, bufferSize);
      }
      catch (final IllegalArgumentException error) {
        emitter.onError(error);
        return;
      }

      // Check that the recorder was successfully initialized:
      if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
        emitter.onError(new IllegalStateException("Audio.record failed to initialize"));
        return; // perhaps the RECORD_AUDIO permission is missing?
      }

      // Begin recording audio:
      try {
        recorder.startRecording();
      }
      catch (final IllegalStateException error) {
        emitter.onError(error);
        return;
      }

      try {
        // Emit audio frames to the subscriber:
        while (!emitter.isDisposed()) {
          buffer.clear();
          final int rc = recorder.read(buffer, buffer.capacity());
          if (rc < AudioRecord.SUCCESS) {
            throw new IllegalStateException(String.format("Audio.record read returned error code %d", rc));
          }
          if (rc == 0) continue; // skip any empty frames
          if (rc != buffer.capacity()) {
            Log.w(TAG, String.format("Read an unexpected audio sample length of %d bytes", rc));
          }
          emitter.onNext(new AudioFrame(buffer));
        }
        emitter.onComplete(); // the callback will never be invoked, since the emitter is already disposed
      }
      catch (final IllegalStateException error) {
        emitter.onError(error);
        return;
      }
      finally {
        Log.d(TAG, "Audio.record stop");
        try {
          recorder.stop();
        }
        catch (final IllegalStateException error) {
          Log.e(TAG, "Failed to stop audio recording.", error);
        }
        recorder.release();
      }
    });
  }
}
