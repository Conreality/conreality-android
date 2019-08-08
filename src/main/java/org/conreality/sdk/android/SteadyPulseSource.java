/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;

/** SteadyPulseSource */
public final class SteadyPulseSource extends PulseSource {
  private @Nullable Disposable ticker;

  public SteadyPulseSource(final @NonNull Context context) {
    super(context);
    this.ticker = Observable.interval(1, TimeUnit.SECONDS)
        .subscribe((tick) -> SteadyPulseSource.this.emit(42f));
  }

  /** Implements io.reactivex.disposables.Disposable#dispose(). */
  @Override
  public void dispose() {
    super.dispose();
    if (this.ticker != null) {
      this.ticker.dispose();
      this.ticker = null;
    }
  }
}
