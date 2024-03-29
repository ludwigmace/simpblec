package com.blemsgfw;

import java.util.UUID;

public interface BleStatusCallback {

	public void messageSent (UUID uuid);
	
	public void remoteServerAdded(String serverName);
	
	public void foundPeer(BlePeer blePeer);
	
	public void handleReceivedMessage(String recipientFingerprint, String senderFingerprint, byte[] payload, String msgType);
	
}
