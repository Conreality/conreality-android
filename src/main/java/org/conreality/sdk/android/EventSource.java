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

/** EventSource */
public class EventSource<T> implements Disposable, ObservableOnSubscribe<T> {
  protected static final String TAG = "ConrealitySDK";

  protected @Nullable Set<ObservableEmitter<T>> emitters =
      new LinkedHashSet<ObservableEmitter<T>>(1);

  public EventSource(final @NonNull Context context) {
    assert(context != null);
  }

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
  public void subscribe(final @NonNull ObservableEmitter<T> emitter) throws Exception {
    assert(emitter != null);
    Log.d(TAG, String.format("%s#subscribe", this.getClass().getName()));
    if (this.emitters != null) {
      this.emitters.add(emitter);
    }
  }

  /** Emits an event to all observers. */
  public void emit(final @NonNull T event) {
    if (this.emitters != null) {
      for (final ObservableEmitter<T> emitter : this.emitters) {
        if (!emitter.isDisposed()) {
          emitter.onNext(event);
        }
      }
    }
  }
}
