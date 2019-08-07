/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

/** PeerStatus */
public enum PeerStatus {
  Unknown,
  Discovered,
  Connecting,
  Connected,
  ConnectionRejected,
  ConnectionFailed,
  Disconnected,
  Lost;

  public static PeerStatus fromStatus(final Status status) {
    switch (status.getStatusCode()) {
      case ConnectionsStatusCodes.STATUS_OK:
        return Connected;
      case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
        return ConnectionRejected;
      case ConnectionsStatusCodes.STATUS_ERROR:
        return ConnectionFailed;
      default:
        return Unknown;
    }
  }
}
