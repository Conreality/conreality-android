/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** PeerService */
public final class PeerService extends ConrealityService {

  /** Bind to the service, creating it if needed. */
  public static boolean bind(final @NonNull Context context,
                             final @NonNull ServiceConnection conn) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(conn);

    final boolean ok = context.bindService(new Intent(context, PeerService.class), conn, Context.BIND_AUTO_CREATE);
    if (!ok) {
      context.unbindService(conn);
    }
    return ok;
  }

  public final class LocalBinder extends Binder {
    public @NonNull PeerService getService() {
      return PeerService.this;
    }
  }

  private final @NonNull IBinder binder = new LocalBinder();
  private @Nullable PeerMesh peerMesh;

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
    this.peerMesh = new PeerMesh(this);
  }

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Terminating the bound service...");
    if (this.peerMesh != null) {
      this.peerMesh.stop();
      this.peerMesh = null;
    }
    Log.i(TAG, "Terminated the bound service.");
  }

  public void start() {
    if (this.peerMesh == null) return;

    this.peerMesh.startDiscovery();

    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      peerMesh.stopDiscovery(); // FIXME
      peerMesh.startAdvertising();
      schedulePing();
    }, 5000);
  }

  public void schedulePing() {
    pingAll();
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      schedulePing();
    }, 1000);
  }

  public void pingAll() {
    Log.d(TAG, "Pinging all peers...");
    if (this.peerMesh != null) {
      this.peerMesh.pingAll();
    }
  }

  public void stop() {
    if (this.peerMesh == null) return;

    this.peerMesh.stop();
  }

  public List<Peer> getPeers() {
    if (this.peerMesh == null) return new ArrayList<Peer>();

    return this.peerMesh.registry.toList();
  }
}
