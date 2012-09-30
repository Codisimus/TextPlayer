package com.codisimus.plugins.textplayer;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Encrypts/Decrypts sensitive files
 */
public class Encrypter {
    private Cipher ecipher;
    private Cipher dcipher;
    private byte[] salt = {
        (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
        (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
    };
    private int iterationCount = 19;

    /**
     * Constructs a new Encrypter with the given pass phrase
     *
     * @param passPhrase The given pass phrase
     */
    public Encrypter(String passPhrase) {
        try {
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            ecipher = Cipher.getInstance(key.getAlgorithm());
            dcipher = Cipher.getInstance(key.getAlgorithm());

            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (Exception e) {
            TextPlayer.logger.severe("Failed to construct Encrypter");
        }
    }

    /**
     * Returns an encrypted version of a String
     *
     * @param str The String to encrypt
     * @return The encrypted String
     */
    public String encrypt(String str) {
        try {
            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc = ecipher.doFinal(utf8);

            return new sun.misc.BASE64Encoder().encode(enc);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns an decrypted version of a String
     *
     * @param str The String to decrypt
     * @return The decrypted String
     */
    public String decrypt(String str) {
        try {
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
            byte[] utf8 = dcipher.doFinal(dec);

            return new String(utf8, "UTF8");
        } catch (Exception e) {
            return null;
        }
    }
}
