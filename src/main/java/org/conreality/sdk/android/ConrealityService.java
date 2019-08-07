/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** ConrealityService */
public abstract class ConrealityService extends Service implements DefaultLifecycleObserver {
  protected static final String TAG = "ConrealitySDK";

  /** Implements android.app.Service#onBind(). */
  @Override
  public @NonNull IBinder onBind(final @NonNull Intent intent) {
    assert(intent != null);

    return null; // TODO
  }

  /** Implements android.app.Service#onCreate(). */
  @Override
  public void onCreate() {}

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {}

  /** Implements android.app.Service#onStartCommand(). */
  @Override
  public int onStartCommand(final @NonNull Intent intent, final int flags, final int startID) {
    assert(intent != null);

    final String action = (intent != null) ? intent.getAction() : null;
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, String.format("%s.onStartCommand: intent=%s flags=%d startID=%d action=%s",
          getClass().getName(), intent, flags, startID, action));
    }

    return START_REDELIVER_INTENT;
  }
}
