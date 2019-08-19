/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import java.util.Objects;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

/** BeaconService */
public final class BeaconService extends ConrealityService implements BeaconConsumer {
  private static final String IBEACON_LAYOUT = "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24";
  private static final String ALTBEACON_LAYOUT = BeaconParser.ALTBEACON_LAYOUT;

  /** Bind to the service, creating it if needed. */
  public static boolean bind(final @NonNull Context context,
                             final @NonNull ServiceConnection conn) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(conn);

    final boolean ok = context.bindService(new Intent(context, BeaconService.class), conn, Context.BIND_AUTO_CREATE);
    if (!ok) {
      context.unbindService(conn);
    }
    return ok;
  }

  public final class LocalBinder extends Binder {
    public @NonNull BeaconService getService() {
      return BeaconService.this;
    }
  }

  private final @NonNull IBinder binder = new LocalBinder();
  private @Nullable BeaconManager beaconManager;

  public void setRangeNotifier(final @Nullable RangeNotifier rangeNotifier) {
    assert(this.beaconManager != null);

    this.beaconManager.setRangeNotifier(rangeNotifier);
  }

  /** Implements android.app.Service#onBind(). */
  @Override
  public @NonNull IBinder onBind(final @NonNull Intent intent) {
    return this.binder;
  }

  /** Implements android.app.Service#onCreate(). */
  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "Created the bound service.");
  }

  public void onConnection(final @NonNull Activity activity, final @NonNull Context context) {
    assert(activity != null);
    assert(context != null);

    if (activity instanceof LifecycleOwner) {
      ((LifecycleOwner)activity).getLifecycle().addObserver(this);
    }

    this.beaconManager = BeaconManager.getInstanceForApplication(context);
    this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));
    this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(ALTBEACON_LAYOUT));
    this.beaconManager.bind(this);
    //this.beaconManager.setDebug(true); // DEBUG
  }

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Terminating the bound service...");
    if (this.beaconManager != null) {
      this.beaconManager.unbind(this);
      this.beaconManager = null;
    }
    Log.i(TAG, "Terminated the bound service.");
  }

  /** Implements BeaconConsumer#onBeaconServiceConnect(). */
  @Override
  public void onBeaconServiceConnect() {
    try {
      this.beaconManager.startRangingBeaconsInRegion(new Region("Anywhere", null, null, null)); // TODO
    }
    catch (final RemoteException error) {
      throw new RuntimeException(error);
    }
  }
}
