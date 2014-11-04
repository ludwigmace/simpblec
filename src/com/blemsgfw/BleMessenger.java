package com.blemsgfw;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class BleMessenger {
	private static String TAG = "blemessenger";
	
	// handles to the device's bluetooth
	private BluetoothManager btMgr;
	private BluetoothAdapter btAdptr;
	private Context ctx;
	
	private boolean messageInbound;
	private boolean messageOutbound;
	
	//  this is defined by the framework, but certainly the developer or user can change it
    private static String uuidServiceBase = "73A20000-2C47-11E4-8C21-0800200C9A66";
    private static MyCentral myGattClient = null; 
    
    private BleStatusCallback bleStatusCallback;
        
    private BleMessage blmsgOut;
    private BleMessage blmsgIn;
    
    private String myIdentifier;
    private String myFriendlyName;

    // keep a map of our messages for a connection session - this may not work out; or we may need to keep a map per peer
    private Map<Integer, BleMessage> bleMessageMap;
    
    private Map<String, BlePeer> peerMap;
    
    List<BleCharacteristic> serviceDef;
	
	public BleMessenger(BleMessengerOptions options, BluetoothManager m, BluetoothAdapter a, Context c) {
		myIdentifier = options.Identifier;
		myFriendlyName = options.FriendlyName;
		
		btMgr = m;
		btAdptr = a;
		ctx = c;
		
		serviceDef = new ArrayList<BleCharacteristic>();
		
		// i need a place to put my found peers
		peerMap = new HashMap<String, BlePeer>();
		
		// create your server for listening and your client for looking; Android can be both at the same time
		myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
	
		serviceDef.add(new BleCharacteristic("identifier_read", uuidFromBase("100"), "read"));		
		serviceDef.add(new BleCharacteristic("identifier_writes", uuidFromBase("101"), "readwrite"));
		//serviceDef.add(new BleCharacteristic("data_notify", uuidFromBase("102"), MyAdvertiser.GATT_NOTIFY));
		serviceDef.add(new BleCharacteristic("data_indicate", uuidFromBase("102"), "notify"));
		//serviceDef.add(new BleCharacteristic("data_write", uuidFromBase("104"), MyAdvertiser.GATT_WRITE));

		
		myGattClient.setRequiredServiceDef(serviceDef);
		
		bleMessageMap = new HashMap<Integer, BleMessage>();

		
	}
	
	private UUID uuidFromBase(String smallUUID) {
		String strUUID =  uuidServiceBase.substring(0, 4) + new String(new char[4-smallUUID.length()]).replace("\0", "0") + smallUUID + uuidServiceBase.substring(8, uuidServiceBase.length());
		UUID idUUID = UUID.fromString(strUUID);
		
		return idUUID;
	}
	
		
	public void showFound(BleStatusCallback blestatuscallback) {
		// actually return a list of some sort
		myGattClient.scanLeDevice(true);
		
		bleStatusCallback = blestatuscallback;
	}
	
	public void attendMessage(BleMessage message, BleStatusCallback blestatuscallback) {
		// TODO add a way to switch b/w Peripheral and Central modes; for right now we'll start with Central
		
		// need to set some variables here . . .
		myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		blmsgIn = message;
		messageInbound = true;
		myGattClient.scanLeDevice(true);
		
	}
	
	public void sendMessage(BleMessage message, BleStatusCallback blestatuscallback) {
	
		// TODO add a way to switch b/w Peripheral and Central modes; for right now we'll start with Peripheral
		//myGattServer = new MyAdvertiser(uuidServiceBase, ctx, btAdptr, btMgr, defaultHandler);

		//myGattClient = new MyCentral(btAdptr, ctx, clientHandler);
		
		// give us a hook to notify the calling Activity
		bleStatusCallback = blestatuscallback;
		
	
		myGattClient.scanLeDevice(true);
		
		// TODO: convert this to use a list of messages, not just a single message
		blmsgOut = message;
		
		// prep the read characteristic with the first packet in the message, but don't increment the counter
		byte[] firstPacket = blmsgOut.GetPacket(0).MessageBytes;

		
	}
	
    private void sendIndicateNotify(UUID uuid) {
    	byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
    	
    	//boolean msgSent = myGattServer.updateCharValue(uuid, nextPacket);
		
    	if (blmsgOut.PendingPacketStatus()) {
    		sendIndicateNotify(uuid);
    	}
    	
    }
    
    // make this work for a collection of recipients, and for each recipient, each one of their pending messages
    private void sendWrite(BluetoothGattCharacteristic writeChar) {
    	
    	// if we've got packets pending send, then send them
    	if (blmsgOut.PendingPacketStatus()) {
    		byte[] nextPacket = blmsgOut.GetPacket().MessageBytes;
    		
    		Log.v(TAG, "send pending packet to " + writeChar.getUuid().toString());
    		//myGattClient.submitCharacteristicWriteRequest(writeChar, nextPacket);
    	} else {
    		Log.v(TAG, "all pending packets sent");
    	}
    }


    public void parseIncomingMsg(BlePeer blePeer, BluetoothGattCharacteristic incomingChar, byte[] incomingBytes, boolean initNewRead) {
    	
    	// read in the bytes to build the message - if it needs another read, read in
    	// probably need to build in a failsafe as well
    	
    	boolean readMore = false;
    	
    	Log.v(TAG, "incoming byte (string): " + new String(incomingBytes));
    	
    	readMore = blmsgIn.BuildMessage(incomingBytes);
    	
    	if (readMore) {
    		if (initNewRead) {
    			//getRead(incomingChar);
    		}
    	} else {
            String pendingmsg = "";
            
            ArrayList<BlePacket> blms = blmsgIn.GetAllPackets();
            
            for (BlePacket b : blms) {
            	pendingmsg = pendingmsg + new String(b.MessageBytes);
            }
            
            pendingmsg = pendingmsg.trim();
            
            Log.v(TAG, "msg fin: " + pendingmsg);
    	}
    	
    }
    


    
    MyGattClientHandler clientHandler = new MyGattClientHandler() {
    	
    	@Override
    	public void incomingMissive(String remoteAddress, UUID remoteCharUUID, byte[] incomingBytes) {
    		// based on remoteAddress, UUID of remote characteristic, put the incomingBytes into a Message
    		// probably need to have a switchboard function
    		
    		// remoteAddress will allow me to look up the connection, so "sender" won't need to be in the packet
    		// 
    		
    		int parentMessagePacketTotal = 0;
    		
    		Log.v(TAG, "incoming hex bytes:" + bytesToHex(incomingBytes));
    		
    		// if our msg is under a few bytes it can't be valid; return
        	if (incomingBytes.length < 5) {
        		Log.v(TAG, "message bytes less than 5");
        		return;
        	}
        	
        	//get the connection
        	BlePeer thisConnection = peerMap.get(remoteAddress);
    	    	
    		// stick our incoming bytes into a bytebuffer to do some operations
        	ByteBuffer bb  = ByteBuffer.wrap(incomingBytes);
        
        	// get the Message to which these packets belong as well as the current counter
        	int parentMessage = incomingBytes[0] & 0xFF;
        	int packetCounter = (incomingBytes[1] << 8) | incomingBytes[2] & 0xFF;

        	// find the message for this connection that we're building
        	BleMessage b = thisConnection.getBleMessage(parentMessage);
        	
        	// your packet payload will be the size of the incoming bytes less our 3 needed for the header (ref'd above)
        	byte[] packetPayload = new byte[incomingBytes.length - 3];
        	
        	// throw these bytes into our payload array
        	bb.get(packetPayload, 2, incomingBytes.length - 3);
        	
        	// if our current packet counter is ZERO, then we can expect our payload to be:
        	// the number of packets we're expecting
        	if (packetCounter == 0) {
        		// right now this is only going to be a couple of bytes
        		parentMessagePacketTotal = (incomingBytes[3] << 8) | incomingBytes[4] & 0xFF;
        		b.BuildMessageFromPackets(packetCounter, packetPayload, parentMessagePacketTotal);
        	} else {
        		// otherwise throw this packet payload into the message
        		b.BuildMessageFromPackets(packetCounter, packetPayload);	
        	}
        	
        	// check if this particular message is done; ie, is it still pending packets?
        	if (b.PendingPacketStatus() == false) {
        		
        		// this message receipt is now complete
        		// so now we need to handle that completed state
        		
        		// if this particular message was an identifying message, then:
        		// - send our identity over
        		// - return friendly name to calling program

        		byte[] payload = b.MessagePayload;
        		String recipientFingerprint = bytesToHex(b.RecipientFingerprint);
        		String senderFingerprint = bytesToHex(b.SenderFingerprint);
        		String msgType = b.MessageType;
        		
        		bleStatusCallback.handleReceivedMessage(recipientFingerprint, senderFingerprint, payload, msgType);
        		
        		// check message integrity here?
        		// what about encryption?
        		
        		// how do i parse the payload if the message contains handshake/identity?
        	}
        	
    		
    		
    	}
    	
		@Override
		public void intakeFoundDevices(ArrayList<BluetoothDevice> devices) {
			
			// loop over all the found devices
			// add them 
			for (BluetoothDevice b: devices) {
				Log.v(TAG, "(BleMessenger)MyGattClientHandler found device:" + b.getAddress());
				
				
				// if the peer isn't already in our list, put them in!
				String peerAddress = b.getAddress();
				
				if (!peerMap.containsKey(peerAddress)) {
					BlePeer blePeer = new BlePeer(peerAddress);
					peerMap.put(peerAddress, blePeer);
				}
				
				// this could possibly be moved into an exterior loop
				myGattClient.connectAddress(peerAddress);
			}
			
		}

		public void readCharacteristicReturned(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status) {
			// a characteristic came in outta the blue - what do i do with it?
			
			// who the hell is talking back to us?
			String remoteAddress = gatt.getDevice().getAddress().toString();
			
			// find the peer based on that address
			BlePeer bleP = peerMap.get(remoteAddress);
			
			parseIncomingMsg(bleP, readChar, charValue, true);

			UUID readIdChar = uuidFromBase("100"); 

			Log.v(TAG, "read request returned from:" + remoteAddress);
			
			// if the incoming characteristic is an ID
			if (readChar.getUuid().equals(readIdChar)) {
				String remoteName = new String(charValue); 
				bleP.SetName(remoteName);
				peerMap.put(remoteAddress, bleP);
				
				Log.v(TAG, "added to peermap:" + remoteName);
				
				bleStatusCallback.remoteServerAdded(bleP.GetName());
				
			}
			
			// now need to add if incoming characteristic is handling data
			
		}
		
		
		
		@Override
		public void getFoundCharacteristics(BluetoothGatt gatt, List<BluetoothGattCharacteristic> foundChars) {
			
			String gattServiceAddress = gatt.getDevice().getAddress();
			String currentStateForService = "id";
			
			String myIdentifying = myIdentifier + "|" + myFriendlyName;

			// the characteristics from a remote gatt device have been pulled into the foundChars List<>
			// we should expect a certain number and type of characteristics
	

			// the correct service definition has already been verified
			// you don't really need to loop over all kinds of shit, you've already verified!

			
		}
		
		@Override
		public void parlayWithRemote(String remoteAddress) {
			
			// so at this point we should still be connected with our remote device
			// and we wouldn't have gotten here if the remote device didn't meet our service spec
			// so now let's trade identification information and transfer any data

			BleMessage b = new BleMessage();
			
			// this message will have a numeric identifier of Zero since it's an ID message, and will use the other info for general ID as well:
			// for the peer id'd by address (might want to change to the actual peer object instead), the UUID we've selected for IDs
			b.SetRemoteInfo(remoteAddress, uuidFromBase("100"), 0);
			
			// add this new message to our message map
			bleMessageMap.put(0, b);

			// pass our remote address and desired uuid to our gattclient, who will look up the gatt object and uuid and issue the read request
			//myGattClient.submitCharacteristicReadRequest(remoteAddress, uuidFromBase("100"));
			myGattClient.submitSubscription(remoteAddress, uuidFromBase("102"));
			
			// still need to write identity info

			
		}
		
		@Override
		public void handleWriteResult(BluetoothGatt gatt, BluetoothGattCharacteristic writtenCharacteristic, int result) {
			// if we're handling the data characteristics, then do this:
			// sendWrite(writtenCharacteristic);
			
			// if we're handling identification characteristics, and we were successful, we've identified ourselves to the remote server
			
			
		}
		
		@Override
		public void reportDisconnect() {

			
		}
    	
    };

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
}
