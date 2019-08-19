/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/** ConrealityThread */
public abstract class ConrealityThread extends Thread {
  protected static final String TAG = "ConrealitySDK";

  public ConrealityThread() {
    this("ConrealityThread");
  }

  public ConrealityThread(final @Nullable String name) {
    super(name);
  }

  @WorkerThread
  @Override
  public void run() {}
}
