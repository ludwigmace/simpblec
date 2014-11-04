package com.blemsgfw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

public interface MyGattClientHandler {
	
	//public void getReadCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status);	
	public void intakeFoundDevices(ArrayList<BluetoothDevice> devices);
	public void parlayWithRemote(String remoteAddress);
	public void incomingMissive(String remoteAddress, UUID remoteCharUUID, byte[] incomingBytes);
	public void getFoundCharacteristics(BluetoothGatt gatt, List<BluetoothGattCharacteristic> foundChars);		
	public void readCharacteristicReturned(BluetoothGatt gatt, BluetoothGattCharacteristic readChar, byte[] charValue, int status);
	
	public void handleWriteResult(BluetoothGatt gatt, BluetoothGattCharacteristic writtenCharacteristic, int result);
	public void reportDisconnect();
	
}
