/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrData;

/** PolarPulseSource */
public final class PolarPulseSource extends PulseSource {
  private @Nullable PolarBleApi api;
  private @Nullable String deviceID;

  public PolarPulseSource(final @NonNull Context context) {
    super(context);

    this.api = PolarBleApiDefaultImpl.defaultImplementation(context, PolarBleApi.FEATURE_HR);
    this.api.setAutomaticReconnection(true);
    this.api.setApiCallback(callbacks);

    Log.d(TAG, "Discovering Polar devices...");
    final Disposable result = this.api.autoConnectToDevice(-60/*dBm*/, "180D", null).subscribe(
      new Action() {
        @Override
        public void run() throws Exception {
          Log.i(TAG, "Discovered a Polar device, attempting to connect...");
        }
      },
      new Consumer<Throwable>() {
        @Override
        public void accept(final Throwable throwable) throws Exception {
          Log.e(TAG, "Failed to discover Polar devices.", throwable);
        }
      }
    );
  }

  /** Implements io.reactivex.disposables.Disposable#dispose(). */
  @Override
  public void dispose() {
    super.dispose();
    if (this.api != null) {
      if (this.deviceID != null) {
        try {
          this.api.disconnectFromDevice(this.deviceID);
        }
        catch (final PolarInvalidArgument error) {
          Log.e(TAG, String.format("Failed to disconnect from Polar device %s.", this.deviceID), error);
        }
        this.deviceID = null;
      }
      this.api.shutDown();
      this.api = null;
    }
  }

  private final @NonNull PolarBleApiCallback callbacks = new PolarBleApiCallback() {
    /** Implements polar.com.sdk.api.PolarBleApiCallback#deviceConnecting(). */
    @Override
    public void deviceConnecting(final @NonNull PolarDeviceInfo device) {
      assert(device != null);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, String.format("Connecting to Polar device %s with %d dBm...", device.deviceId, device.rssi));
      }
      // TODO: PolarPulseSource.this.emit(...);
    }

    /** Implements polar.com.sdk.api.PolarBleApiCallback#deviceConnected(). */
    @Override
    public void deviceConnected(final @NonNull PolarDeviceInfo device) {
      assert(device != null);
      if (Log.isLoggable(TAG, Log.INFO)) {
        Log.i(TAG, String.format("Connected to Polar device %s with %d dBm.", device.deviceId, device.rssi));
      }
      PolarPulseSource.this.deviceID = device.deviceId;
      // TODO: PolarPulseSource.this.emit(...);
    }

    /** Implements polar.com.sdk.api.PolarBleApiCallback#deviceDisconnected(). */
    @Override
    public void deviceDisconnected(final @NonNull PolarDeviceInfo device) {
      assert(device != null);
      if (Log.isLoggable(TAG, Log.INFO)) {
        Log.i(TAG, String.format("Disconnected from Polar device %s.", device.deviceId));
      }
      PolarPulseSource.this.deviceID = null;
      // TODO: PolarPulseSource.this.emit(...);
    }

    /** Implements polar.com.sdk.api.PolarBleApiCallback#hrFeatureReady(). */
    @Override
    public void hrFeatureReady(final @NonNull String deviceID) { // HR notifications are about to start
      assert(deviceID != null);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, String.format("Preparing to receive from Polar device %s...", deviceID));
      }
    }

    /** Implements polar.com.sdk.api.PolarBleApiCallback#hrNotificationReceived(). */
    @Override
    public void hrNotificationReceived(final @NonNull String deviceID, final @NonNull PolarHrData data) {
      assert(deviceID != null);
      assert(data != null);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, String.format("Received a reading of %d bpm from Polar device %s.", data.hr, deviceID));
      }
      PolarPulseSource.this.emit(data.hr);
    }
  };
}
