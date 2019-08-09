/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.app.Application;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import androidx.annotation.NonNull;
import com.jakewharton.threetenabp.AndroidThreeTen;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Conreality SDK for Android. */
public final class ConrealitySDK {
  private static final AtomicBoolean initialized = new AtomicBoolean();

  private ConrealitySDK() {
    throw new AssertionError();
  }

  /** Initializes the Conreality SDK for Android. */
  public static void init(final @NonNull Application application) {
    init((Context)Objects.requireNonNull(application));
  }

  /** Initializes the Conreality SDK for Android. */
  public static void init(final @NonNull Activity activity) {
    init((Context)Objects.requireNonNull(activity));
  }

  /** Initializes the Conreality SDK for Android. */
  public static void init(final @NonNull Service service) {
    init((Context)Objects.requireNonNull(service));
  }

  /** Initializes the Conreality SDK for Android. */
  public static void init(final @NonNull Context context) {
    Objects.requireNonNull(context);

    if (initialized.getAndSet(true)) return;

    // Initialize the ThreeTenABP library:
    if (ConrealitySDK.class.getClassLoader().getResource("com/jakewharton/threetenabp/AndroidThreeTen.class") != null) {
      AndroidThreeTen.init(context);
    }
  }
}
