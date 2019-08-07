/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** PeerRegistry */
public final class PeerRegistry {
  private Map<String, Peer> peers = new HashMap<String, Peer>();

  public PeerRegistry() {}

  public boolean has(final @NonNull String peerID) {
    return this.peers.containsKey(peerID);
  }

  public void add(final @NonNull Peer peer) {
    this.peers.put(peer.id, peer);
  }

  public void setStatus(final @NonNull String peerID, final @NonNull PeerStatus peerStatus) {
    final Peer peer = this.peers.get(peerID);
    if (peer != null) {
      peer.status = peerStatus;
      peer.updateLastSeen();
    }
  }

  public void setName(final @NonNull String peerID, final @NonNull String peerName) {
    final Peer peer = this.peers.get(peerID);
    if (peer != null) {
      peer.name = peerName;
      peer.updateLastSeen();
    }
  }

  public void updateLastSeen(final @NonNull String peerID) {
    final Peer peer = this.peers.get(peerID);
    if (peer != null) {
      peer.updateLastSeen();
    }
  }

  public List<String> getConnectedPeerIDs() {
    final List<String> result = new ArrayList<String>();
    for (final Map.Entry<String, Peer> entry : this.peers.entrySet()) {
      final Peer peer = entry.getValue();
      if (peer.isConnected()) {
        result.add(peer.id);
      }
    }
    return result;
  }

  public List<Peer> toList() {
    final List<Peer> result = new ArrayList<Peer>();
    for (final Map.Entry<String, Peer> entry : this.peers.entrySet()) {
      result.add(entry.getValue());
    }
    return result;
  }
}
