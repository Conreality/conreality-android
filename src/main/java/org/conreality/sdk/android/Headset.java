/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Objects;

/** Headset */
public interface Headset {
  public static boolean hasPermissions(final @NonNull Context context) {
    Objects.requireNonNull(context);
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
  }

  public static void requestPermissions(final @NonNull Activity activity) {
    Objects.requireNonNull(activity);
    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 0); // TODO: handle the callback
  }
}
