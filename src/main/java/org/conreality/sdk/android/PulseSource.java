/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import androidx.annotation.NonNull;

/** PulseSource */
public abstract class PulseSource extends EventSource<Float> {
  public PulseSource(final @NonNull Context context) {
    super(context);
  }

  /** Determines whether this pulse source is usable. */
  public abstract boolean isAvailable();
}
