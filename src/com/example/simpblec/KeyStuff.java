package com.example.simpblec;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;

// taken from: https://android.googlesource.com/platform/development/+/master/samples/Vault/src/com/example/android/vault/SecretKeyWrapper.java
public class KeyStuff {

	private final Cipher mCipher;
	private final KeyPair mPair;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static final String TAG = null;
    
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
	
	public KeyStuff(Context context, String alias) throws GeneralSecurityException, IOException {

		mCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		
		final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
	    keyStore.load(null);
	
	    // if there isn't already a keypair, generate one
	    if (!keyStore.containsAlias(alias)) {
	    	generateKeyPair(context, alias);
	    }
		
	    final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
	      
	    mPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
	    
	}
	
	
	// generate the keypair and add it 
    private static void generateKeyPair(Context context, String alias)
            throws GeneralSecurityException {
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 100);
        final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        gen.initialize(spec);
        gen.generateKeyPair();
    }
    
    public byte[] PublicKey () {
    	return mPair.getPublic().getEncoded();
    }
    
    public String PuFingerprint() {
    	return bytesToHex(this.PublicKey());
    }
    
    // Wrap a SecretKey using the public key assigned to this wrapper.
    public byte[] wrap(SecretKey key) throws GeneralSecurityException {
        mCipher.init(Cipher.WRAP_MODE, mPair.getPublic());
        return mCipher.wrap(key);
    }
    
    public byte[] encrypt(byte[] destKpu, byte[] payload)  {
    	
		Key pubkey;
		byte[] emptyBytes = new byte[0];
		
		// return an empty byte array is anything fails
		
		try {
			pubkey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destKpu));
			mCipher.init(Cipher.ENCRYPT_MODE, pubkey);
		} catch (InvalidKeySpecException e) {
			Log.v(TAG, "rsa: invalid key specification");
			return emptyBytes;
		} catch (NoSuchAlgorithmException e) {
			Log.v(TAG, "rsa: no such algorithm");
			return emptyBytes;
		} catch (InvalidKeyException e) {
			Log.v(TAG, "rsa: invalid key");
			return emptyBytes;
		}
		
		try {
			return(mCipher.doFinal(payload));
		} catch (IllegalBlockSizeException e) {
			Log.v(TAG, "rsa: illegal block size;");
			e.printStackTrace();
			return emptyBytes;
		} catch (BadPaddingException e) {
			Log.v(TAG, "rsa: bad padding");
			return emptyBytes;
		}
		
		
		
    }
    
    // Unwrap a SecretKey using the private key assigned to this
    public SecretKey unwrap(byte[] blob) throws GeneralSecurityException {
        mCipher.init(Cipher.UNWRAP_MODE, mPair.getPrivate());
        return (SecretKey) mCipher.unwrap(blob, "AES", Cipher.SECRET_KEY);
    }
	
}
