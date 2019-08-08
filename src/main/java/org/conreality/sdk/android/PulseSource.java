/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import java.util.LinkedHashSet;
import java.util.Set;

/** PulseSource */
public abstract class PulseSource implements ObservableOnSubscribe<Float> {
  protected static final String TAG = "ConrealitySDK";

  protected Set<ObservableEmitter<Float>> emitters = new LinkedHashSet<ObservableEmitter<Float>>(1);

  public PulseSource(final @NonNull Context context) {
    assert(context != null);
  }

  /** Implements io.reactivex.ObservableOnSubscribe#subscribe(). */
  @Override
  public void subscribe(@NonNull ObservableEmitter<Float> emitter) throws Exception {
    assert(emitter != null);
    Log.d(TAG, "PulseSource.subscribe");
    this.emitters.add(emitter);
  }

  /** Emits a heart-rate measurement to all observers. */
  protected void emit(final float measurement) {
    for (final ObservableEmitter<Float> emitter : this.emitters) {
      if (!emitter.isDisposed()) {
        emitter.onNext(measurement);
      }
    }
  }
}
