/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import java.util.Objects;

/** PulseService */
public final class PulseService extends ConrealityService {

  /** Bind to the service, creating it if needed. */
  public static boolean bind(final @NonNull Context context,
                             final @NonNull ServiceConnection conn) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(conn);

    final boolean ok = context.bindService(new Intent(context, PulseService.class), conn, Context.BIND_AUTO_CREATE);
    if (!ok) {
      context.unbindService(conn);
    }
    return ok;
  }

  public final class LocalBinder extends Binder {
    public @NonNull PulseService getService() {
      return PulseService.this;
    }
  }

  private final @NonNull IBinder binder = new LocalBinder();
  private @Nullable PulseSource source;

  /** Implements android.app.Service#onBind(). */
  @Override
  public @NonNull IBinder onBind(final @NonNull Intent intent) {
    assert(intent != null);

    return this.binder;
  }

  /** Implements android.app.Service#onCreate(). */
  @Override
  public void onCreate() {
    super.onCreate();

    Log.d(TAG, "Creating the pulse service...");
    this.source = new SensorPulseSource(this);
    if (!this.source.isAvailable()) {
      this.source.dispose();
      this.source = new PolarPulseSource(this); // FIXME
    }
    Log.i(TAG, "Created the pulse service.");
  }

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "Terminating the pulse service...");
    if (this.source != null) {
      this.source.dispose();
      this.source = null;
    }
    Log.i(TAG, "Terminated the pulse service.");
  }

  /** Returns observable measurements of the current heart-rate. */
  public @NonNull Observable<Float> measure() {
    assert(this.source != null);

    return Observable.create(this.source);
  }
}
