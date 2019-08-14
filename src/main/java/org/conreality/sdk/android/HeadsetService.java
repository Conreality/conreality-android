/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HeadsetService */
public final class HeadsetService extends ConrealityService {
  private static final String TTS_ENGINE = "com.google.android.tts";

  /** Bind to the service, creating it if needed. */
  public static boolean bind(final @NonNull Context context,
                             final @NonNull ServiceConnection conn) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(conn);

    final boolean ok = context.bindService(new Intent(context, HeadsetService.class), conn, Context.BIND_AUTO_CREATE);
    if (!ok) {
      context.unbindService(conn);
    }
    return ok;
  }

  public final class LocalBinder extends Binder {
    public @NonNull HeadsetService getService() {
      return HeadsetService.this;
    }
  }

  private final @NonNull IBinder binder = new LocalBinder();
  private @NonNull HeadsetStatus status = new HeadsetStatus();
  private @Nullable BluetoothHeadset bluetoothHeadset;
  private @Nullable TextToSpeech ttsEngine;
  private @Nullable Bundle ttsParams;
  private @Nullable List<String> ttsQueue = new ArrayList<String>();

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
    Log.i(TAG, "Created the bound service.");
    this.ttsEngine = new TextToSpeech(this, this.ttsInitListener, TTS_ENGINE);
    this.ttsParams = new Bundle();
  }

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Terminating the bound service...");
    if (this.ttsEngine != null) {
      this.ttsEngine.shutdown();
      this.ttsEngine = null;
      this.ttsParams = null;
    }
    if (this.ttsQueue != null) {
      this.ttsQueue = null;
    }
    Log.i(TAG, "Terminated the bound service.");
  }

  /** Implements android.app.Service#onStartCommand(). */
  @Override
  public int onStartCommand(final @NonNull Intent intent, final int flags, final int startID) {
    assert(intent != null);

    final String action = (intent != null) ? intent.getAction() : null;
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, String.format("HeadsetService.onStartCommand: intent=%s flags=%d startID=%d action=%s", intent, flags, startID, action));
    }
    switch (action) {
      case "speak": {
        this.speak(intent.getStringExtra("message"));
        break;
      }
    }
    return START_REDELIVER_INTENT;
  }

  /** Plays an audio file. */
  public boolean playFile(final @NonNull String file) {
    Objects.requireNonNull(file);

    try {
      (new AudioPlayerThread(new File(file))).start();
      return true;
    }
    catch (final FileNotFoundException error) {
      Log.e(TAG, "Failed to play audio file.", error);
      return false;
    }
  }

  /** Determines whether text-to-speech is supported. */
  public boolean canSpeak() {
    return (this.ttsEngine != null) || (this.ttsQueue != null);
  }

  /** Synthesizes speech from the given text message. */
  public boolean speak(final @NonNull String message) {
    Objects.requireNonNull(message);

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, String.format("HeadsetService.speak: message=\"%s\"", message));
    }

    if (this.ttsEngine == null) {
      if (this.ttsQueue == null) return false; // nothing to be done
      return ttsQueue.add(message);
    }

    return this._speak(message, TextToSpeech.QUEUE_FLUSH);
  }

  /** Stops any ongoing speech synthesis. */
  public boolean stopSpeaking() {
    Log.d(TAG, "HeadsetService.stopSpeaking");

    if (this.ttsQueue != null) this.ttsQueue.clear();
    if (this.ttsEngine == null) return false;

    return this.ttsEngine.stop() == TextToSpeech.SUCCESS;
  }

  private boolean _speak(final @NonNull String message, final int queueMode) {
    assert(message != null);

    final @NonNull String utteranceID = UUID.randomUUID().toString();
    return this.ttsEngine.speak(message, queueMode, this.ttsParams, utteranceID) == TextToSpeech.SUCCESS;
  }

  private final @NonNull TextToSpeech.OnInitListener ttsInitListener = new TextToSpeech.OnInitListener() {
    /** Implements TextToSpeech.OnInitListener#onInit(). */
    @Override
    public void onInit(final int status) {
      if (status == TextToSpeech.SUCCESS) {
        Log.d(TAG, "Initialized the speech synthesis engine.");
        //HeadsetService.this.ttsEngine.setOnUtteranceProgressListener(this); // TODO
        for (final String message : HeadsetService.this.ttsQueue) {
          HeadsetService.this._speak(message, TextToSpeech.QUEUE_ADD);
        }
        HeadsetService.this.ttsQueue.clear();
      }
      else {
        Log.e(TAG, "Failed to initialize the speech synthesis engine.");
        HeadsetService.this.ttsEngine = null;
        HeadsetService.this.ttsParams = null;
        HeadsetService.this.ttsQueue = null;
      }
    }
  };

  private final @NonNull BluetoothProfile.ServiceListener bluetoothListener = new BluetoothProfile.ServiceListener() {
    /** Implements BluetoothProfile.ServiceListener#onServiceConnected(). */
    @Override
    public void onServiceConnected(final int profile, final @NonNull BluetoothProfile proxy) {
      assert(proxy != null);

      if (profile == BluetoothProfile.HEADSET) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Connected to the Bluetooth headset service.");
        }
        HeadsetService.this.bluetoothHeadset = (BluetoothHeadset)proxy;
        HeadsetService.this.status.hasWirelessHeadset = (proxy.getConnectedDevices().size() > 0); // requires the BLUETOOTH permission
        //HeadsetService.this.sendStatus(); // TODO
      }
    }

    /** Implements BluetoothProfile.ServiceListener#onServiceDisconnected(). */
    @Override
    public void onServiceDisconnected(final int profile) {
      if (profile == BluetoothProfile.HEADSET) {
        if (Log.isLoggable(TAG, Log.INFO)) {
          Log.i(TAG, "Disconnected from the Bluetooth headset service.");
        }
        HeadsetService.this.bluetoothHeadset = null;
        HeadsetService.this.status.hasWirelessHeadset = false;
        //HeadsetService.this.sendStatus(); // TODO
      }
    }
  };
}
