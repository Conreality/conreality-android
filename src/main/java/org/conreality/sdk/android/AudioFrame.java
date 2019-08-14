/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import androidx.annotation.NonNull;
import java.util.Objects;

/** AudioFrame */
public class AudioFrame {
  public final @NonNull byte[] data;

  public AudioFrame(final @NonNull byte[] data) {
    this.data = Objects.requireNonNull(data);
  }
}
