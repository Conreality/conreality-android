/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import java.util.concurrent.TimeUnit;

/** Pulse */
public interface Pulse {

  /** Streams heart-rate measurements. */
  public static @NonNull Observable<Float> measure() {
    return Observable.interval(1, TimeUnit.SECONDS).map((tick) -> {
      return (float)Math.random() * 200f; // TODO
    });
  }
}
