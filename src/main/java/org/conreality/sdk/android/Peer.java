/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import java.time.Instant;

/** Peer */
public class Peer {
  public final String id;
  public String name;
  public PeerStatus status;
  public Instant lastSeen;

  public Peer(final String id, final String name, final PeerStatus status) {
    this.id = id;
    this.name = name;
    this.status = status;
    this.updateLastSeen();
  }

  public void updateLastSeen() {
    this.lastSeen = Instant.now();
  }

  public boolean isConnected() {
    return this.status == PeerStatus.Connected;
  }
}
