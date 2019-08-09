/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import java.util.LinkedHashSet;
import java.util.Set;

/** PulseSource */
public abstract class PulseSource implements Disposable, ObservableOnSubscribe<Float> {
  protected static final String TAG = "ConrealitySDK";

  protected @Nullable Set<ObservableEmitter<Float>> emitters =
      new LinkedHashSet<ObservableEmitter<Float>>(1);

  public PulseSource(final @NonNull Context context) {
    assert(context != null);
  }

  /** Determines whether this pulse source is usable. */
  public abstract boolean isAvailable();

  /** Implements io.reactivex.disposables.Disposable#isDisposed(). */
  @Override
  public boolean isDisposed() {
    return this.emitters == null;
  }

  /** Implements io.reactivex.disposables.Disposable#dispose(). */
  @Override
  public void dispose() {
    this.emitters = null;
  }

  /** Implements io.reactivex.ObservableOnSubscribe#subscribe(). */
  @Override
  public void subscribe(@NonNull ObservableEmitter<Float> emitter) throws Exception {
    assert(emitter != null);
    Log.d(TAG, "PulseSource.subscribe");
    if (this.emitters != null) {
      this.emitters.add(emitter);
    }
  }

  /** Emits a heart-rate measurement to all observers. */
  protected void emit(final float measurement) {
    if (this.emitters != null) {
      for (final ObservableEmitter<Float> emitter : this.emitters) {
        if (!emitter.isDisposed()) {
          emitter.onNext(measurement);
        }
      }
    }
  }
}
