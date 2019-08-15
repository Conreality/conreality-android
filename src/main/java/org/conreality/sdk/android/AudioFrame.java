/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.util.Objects;

/** AudioFrame */
public class AudioFrame {
  public final @NonNull byte[] data;

  public AudioFrame(final @NonNull ByteBuffer data) {
    this.data = new byte[data.remaining()];
    data.get(this.data);
  }

  public AudioFrame(final @NonNull byte[] data) {
    this.data = Objects.requireNonNull(data);
  }

  public @NonNull byte[] toByteArray() {
    return this.data.clone();
  }

  public @NonNull ByteBuffer toByteBuffer() {
    return ByteBuffer.wrap(this.data.clone());
  }
}
