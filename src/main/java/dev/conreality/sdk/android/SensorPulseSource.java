/* This is free and unencumbered software released into the public domain. */

package dev.conreality.sdk.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;

/** SensorPulseSource */
public final class SensorPulseSource extends PulseSource {
  private final @Nullable SensorManager sensorManager;
  private final @Nullable Sensor sensor;

  public SensorPulseSource(final @NonNull Context context) {
    super(context);

    this.sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    assert(this.sensor != null);
    if (this.sensor == null) {
      Log.w(TAG, "Unable to find a TYPE_HEART_RATE sensor.");
      return;
    }

    final boolean ok = this.sensorManager.registerListener(this.sensorEventListener,
        this.sensor, SensorManager.SENSOR_DELAY_NORMAL, 0);
    if (!ok) {
      Log.e(TAG, "Failed to register TYPE_HEART_RATE sensor listener.");
    }
  }

  /** Implements dev.conreality.sdk.android.PulseSource#isAvailable(). */
  @Override
  public boolean isAvailable() {
    return this.sensor != null;
  }

  /** Implements io.reactivex.disposables.Disposable#dispose(). */
  @Override
  public void dispose() {
    super.dispose();
    if (this.sensorEventListener != null) {
      this.sensorManager.unregisterListener(this.sensorEventListener);
      this.sensorEventListener = null;
    }
  }

  private @Nullable SensorEventListener sensorEventListener = new SensorEventListener() {
    /** Implements android.hardware.SensorEventListener#onAccuracyChanged(). */
    @Override
    public void onAccuracyChanged(final @NonNull Sensor sensor, final int accuracy) {}

    /** Implements android.hardware.SensorEventListener#onSensorChanged(). */
    @Override
    public void onSensorChanged(final @NonNull SensorEvent event) {
      assert(event != null);
      assert(event.sensor.getType() == Sensor.TYPE_HEART_RATE);
      SensorPulseSource.this.emit(event.values.length > 0 ? (float)event.values[0] : null);
    }
  };
}
