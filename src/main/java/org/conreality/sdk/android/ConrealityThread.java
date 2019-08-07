/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/** AudioRecorderThread */
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
