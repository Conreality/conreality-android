/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HeadsetService */
public final class HeadsetService extends ConrealityService implements Headset {
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
  private final @NonNull HeadsetStatus status = new HeadsetStatus();
  private final @NonNull EventSource<HeadsetStatus> source = new EventSource<HeadsetStatus>(this);
  private @Nullable AudioManager audioManager;
  private @Nullable BluetoothAdapter bluetoothAdapter;
  private @Nullable BluetoothHeadset bluetoothHeadset;
  private @Nullable TextToSpeech ttsEngine;
  private @Nullable Bundle ttsParams;
  private @Nullable List<String> ttsQueue = new ArrayList<String>();
  private @Nullable Disposable recorder;

  public @NonNull HeadsetStatus getStatus() {
    return this.status;
  }

  protected void sendStatus() {
    if (this.source != null) {
      this.source.emit(this.status);
    }
  }

  public @NonNull Observable<HeadsetStatus> listen() {
    return Observable.create(this.source);
  }

  /** Implements android.app.Service#onBind(). */
  @Override
  public @NonNull IBinder onBind(final @NonNull Intent intent) {
    assert(intent != null);
    return this.binder;
  }

  /** Implements android.app.Service#onCreate(). */
  @SuppressWarnings("deprecation")
  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "Creating the headset service...");

    this.audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
    if (this.audioManager != null) {
      this.status.hasWiredHeadset = this.audioManager.isWiredHeadsetOn(); // deprecated
      this.status.hasWirelessHeadset = this.audioManager.isBluetoothA2dpOn() || this.audioManager.isBluetoothScoOn(); // deprecated
    }

    this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled()) {
      final boolean ok = this.bluetoothAdapter.getProfileProxy(this, bluetoothListener, BluetoothProfile.HEADSET);
      if (!ok) {
        Log.e(TAG, "Failed to connect to the Bluetooth headset profile proxy.");
      }
    }

    this.registerReceiver(this.broadcastReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
    this.registerReceiver(this.broadcastReceiver, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
    this.registerReceiver(this.broadcastReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

    //if (this.audioManager != null) {
    //  this.audioManager.startBluetoothSco();
    //}

    this.ttsEngine = new TextToSpeech(this, this.ttsInitListener, TTS_ENGINE);
    this.ttsParams = new Bundle();

    Log.i(TAG, "Created the headset service.");
  }

  /** Implements android.app.Service#onDestroy(). */
  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Terminating the headset service...");

    if (this.recorder != null) {
      this.recorder.dispose();
      this.recorder = null;
    }

    if (this.ttsEngine != null) {
      this.ttsEngine.shutdown();
      this.ttsEngine = null;
      this.ttsParams = null;
    }
    this.ttsQueue = null;

    this.unregisterReceiver(this.broadcastReceiver);

    if (this.bluetoothAdapter != null) {
      if (this.bluetoothHeadset != null) {
        this.bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, this.bluetoothHeadset);
        this.bluetoothHeadset = null;
      }
      this.bluetoothAdapter = null;
    }

    this.audioManager = null; // should be last, used in callbacks

    Log.i(TAG, "Terminated the headset service.");
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

  /** Implements Headset#speak(). */
  @Override
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
        HeadsetService.this.sendStatus();
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
        HeadsetService.this.sendStatus();
      }
    }
  };

  private final @NonNull BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    /** Implements BroadcastReceiver#onReceive(). */
    @MainThread
    @Override
    public void onReceive(final @NonNull Context context, final @NonNull Intent intent) {
      assert(context != null);
      assert(intent != null);
      System.err.println(">>>>>>>>>> onReceive");

      switch (intent.getAction()) {
        case AudioManager.ACTION_HEADSET_PLUG: {
          final int state = intent.getIntExtra("state", -1);
          final int microphone = intent.getIntExtra("microphone", -1);
          if (Log.isLoggable(TAG, Log.DEBUG) || true) {
            final String name = intent.getStringExtra("name");
            Log.d(TAG, String.format("Received broadcast: %s state=%d microphone=%d name=%s", intent.toString(), state, microphone, name));
          }
          HeadsetService.this.status.hasWiredHeadset = (state == 1);
          HeadsetService.this.status.hasMicrophone = (microphone == 1);
          HeadsetService.this.sendStatus();
          break;
        }

        case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
          final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
          if (Log.isLoggable(TAG, Log.DEBUG) || true) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, String.format("Received broadcast: %s state=%d device=%s", intent.toString(), state, device.toString()));
          }
          HeadsetService.this.status.hasWirelessHeadset = (state == BluetoothProfile.STATE_CONNECTED);
          HeadsetService.this.sendStatus();
          if (HeadsetService.this.audioManager != null) {
            if (HeadsetService.this.status.hasWirelessHeadset) {
              HeadsetService.this.audioManager.startBluetoothSco();
            }
            else {
              HeadsetService.this.audioManager.stopBluetoothSco();
            }
          }
          break;
        }

        case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED: {
          final int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
          if (Log.isLoggable(TAG, Log.DEBUG) || true) {
            Log.d(TAG, String.format("Received broadcast: %s state=%d", intent.toString(), state));
          }
          switch (state) {
            // Audio channel is being established
            case AudioManager.SCO_AUDIO_STATE_CONNECTING: {
              // nothing to do just yet
              break;
            }
            // Audio channel is established
            case AudioManager.SCO_AUDIO_STATE_CONNECTED: {
              HeadsetService.this.recorder = AudioRecorder.record()
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.computation())
                  .subscribeWith(new DisposableObserver<AudioFrame>() {
                    @Override
                    public void onStart() {
                      System.err.println(">>>>>>>>>>>>> AudioRecorder: onStart"); // DEBUG
                    }
                    @Override
                    public void onNext(final @NonNull AudioFrame frame) {
                      assert(frame != null);
                      System.err.println(">>>>>>>>>>>>> AudioRecorder: onNext: " + frame); // DEBUG
                    }
                    @Override
                    public void onError(final @NonNull Throwable error) {
                      assert(error != null);
                      System.err.println(">>>>>>>>>>>>> AudioRecorder: onError"); // DEBUG
                      error.printStackTrace();
                    }
                    @Override
                    public void onComplete() {} // never called, since subscriber already disposed
                  });
              break;
            }
            // Audio channel is not established
            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED: {
              if (HeadsetService.this.recorder != null) {
                HeadsetService.this.recorder.dispose();
                HeadsetService.this.recorder = null;
              }
              break;
            }
            // An error trying to obtain the state
            case AudioManager.SCO_AUDIO_STATE_ERROR: {
              // should be unreachable
              break;
            }
          }
          break;
        }

        default: break; // ignore UFOs
      }
    }
  };
}
