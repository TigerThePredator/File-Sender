package com.github.tigerthepredator.file_sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class Encryptor {
	// Generates a public/private key pair using RSA encryption
	public static KeyPair generateKeyPair() {
		try {
			// Generate the keypair
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			// Save the two keys in the folder that the user types in
			String loc = Terminal.ask("Enter the folder where you would like to save your keys: ");
			saveKeyPair(loc, keyPair);
			Terminal.confirm(
					"The keys have been saved successfully. Make sure that you never tell anyone your private key.");

			// Return the keypair
			return keyPair;
		} catch (NoSuchAlgorithmException | IOException e) {
			Terminal.error(e.getMessage());
		}

		return null;
	}

	// Convert the key to base64 string format
	// Code copied from
	// https://self-learning-java-tutorial.blogspot.com/2015/07/convert-string-to-secret-key-in-java.html
	public static String keyToString(Key key) {
		byte[] encoded = key.getEncoded();
		String encodedKey = Base64.getEncoder().encodeToString(encoded);
		return encodedKey;
	}

	// Convert base64 string to a key
	// Code copied from
	// https://self-learning-java-tutorial.blogspot.com/2015/07/convert-string-to-secret-key-in-java.html
	public static Key stringToKey(String s) {
		byte[] decodedKey = Base64.getDecoder().decode(s);
		javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "RSA");
		return secretKey;
	}

	// Save the key pair in two files
	// Code copied from https://snipplr.com/view/18368/
	private static void saveKeyPair(String path, KeyPair keyPair) throws IOException {
		// Get the public key and private key
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		// Save the public key
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		File publicKeyFile = new File(path + "/public.key");
		FileOutputStream fos = new FileOutputStream(publicKeyFile);
		fos.write(x509EncodedKeySpec.getEncoded());
		fos.close();

		// Save the private key
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		File privateKeyFile = new File(path + "/private.key");
		fos = new FileOutputStream(privateKeyFile);
		fos.write(pkcs8EncodedKeySpec.getEncoded());
		fos.close();
	}

	// Load previously saved keys
	// TODO: Actually implement this in the program
	// Code copied from https://snipplr.com/view/18368/
	public KeyPair loadKeyPair(String path)
			throws IOException, NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
		// Get the public key in byte[] format
		File filePublicKey = new File(path + "/public.key");
		FileInputStream fis = new FileInputStream(path + "/public.key");
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();

		// Get the private key in byte[] format
		File filePrivateKey = new File(path + "/private.key");
		fis = new FileInputStream(path + "/private.key");
		byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
		fis.read(encodedPrivateKey);
		fis.close();

		// Create a key factory
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		
		// Get the public key
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

		// Get the private key
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

		// Return the two keys
		return new KeyPair(publicKey, privateKey);
	}

	// Encrypts a string using the given public key
	// Code copied from http://niels.nu/blog/2016/java-rsa.html
	public static String encrypt(String message, Key publicKey) {
		try {
			Cipher encryptCipher = Cipher.getInstance("RSA");
			encryptCipher.init(1, publicKey);
			byte[] cipherText = encryptCipher.doFinal(message.getBytes("UTF_8"));
			return Base64.getEncoder().encodeToString(cipherText);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| javax.crypto.IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
			Terminal.error("Error while encrypting message");
			Terminal.error(e.getMessage());
		}
		return null;
	}

	// Decrypts a string using the given private key
	// Code copied from http://niels.nu/blog/2016/java-rsa.html
	public static String decrypt(String message, Key privateKey) {
		try {
			byte[] bytes = Base64.getDecoder().decode(message);

			Cipher decriptCipher = Cipher.getInstance("RSA");
			decriptCipher.init(2, privateKey);

			return new String(decriptCipher.doFinal(bytes), "UTF_8");
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException
				| javax.crypto.IllegalBlockSizeException | BadPaddingException e) {
			Terminal.error("Error while decrypting received message.");
			Terminal.error(e.getMessage());
		}
		return null;
	}

	// Generates a random string
	// Code copied from https://dzone.com/articles/generate-random-alpha-numeric
	public static String randString(int count) {
		String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder builder = new StringBuilder();

		while (count-- != 0) {
			int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
			builder.append(ALPHA_NUMERIC_STRING.charAt(character));
		}

		return builder.toString();
	}

	// Convert a byte[] to a string in hex format
	// Code copied from http://www.baeldung.com/sha-256-hashing-java
	public static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xFF & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
