package com.efa.wizzmoni.access.security.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(AuthEncryptionUtil.class);

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    // ── Key Padding ───────────────────────────────────────────────
    // Ensures key is exactly 16 bytes for AES-128
    private String padKey(String key) {
        log.debug(">>>> padKey: rawKeyLength={}", key.length());

        String paddedKey;
        if (key.length() < 16) {
            paddedKey = String.format("%-16s", key); // right-pad with spaces
            log.debug("==== padKey: key was short, padded to 16 bytes");
        } else if (key.length() > 16) {
            paddedKey = key.substring(0, 16);        // truncate to 16 bytes
            log.debug("==== padKey: key was long, truncated to 16 bytes");
        } else {
            paddedKey = key;
            log.debug("==== padKey: key is exactly 16 bytes, no change");
        }

        log.debug("<<<< padKey: paddedKeyLength={}", paddedKey.length());
        return paddedKey;
    }
    //Instead Of Padding - Use this
    private SecretKeySpec deriveKey(String secret) throws Exception {
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        keyBytes = java.util.Arrays.copyOf(keyBytes, 16); // AES-128
        return new SecretKeySpec(keyBytes, "AES");
    }
    // ── Encrypt ───────────────────────────────────────────────────
    // Called by External App — encrypts payload into a Base64 token
    public String encrypt(String data, String secretKey) throws Exception {
        log.debug(">>>> encrypt");
        log.debug("==== encrypt: dataLength={}", data.length());
        log.debug("==== encrypt: secretKeyLength={}", secretKey.length());

        //String paddedKey = padKey(secretKey);
        // Key aesKey = new SecretKeySpec(paddedKey.getBytes(StandardCharsets.UTF_8), "AES");
        SecretKeySpec aesKey = deriveKey(secretKey);

        log.debug("==== encrypt: AES key created");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        log.debug("==== encrypt: cipher initialised for ENCRYPT_MODE. algorithm={}", ALGORITHM);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String base64 = Base64.getUrlEncoder().encodeToString(encryptedBytes);

        log.debug("==== encrypt: encryption complete. encryptedByteLength={}", encryptedBytes.length);
        log.debug("<<<< encrypt: returning base64 token");
        return base64;
    }

    // ── Decrypt ───────────────────────────────────────────────────
    // Called by BRS AuthLaunchFilter — decrypts token back to payload string
    public String decrypt(String encryptedBase64, String secretKey) throws Exception {
        log.debug(">>>> decrypt");
        log.debug("==== decrypt: encryptedBase64Length={}", encryptedBase64.length());
        log.debug("==== decrypt: secretKeyLength={}", secretKey.length());

        //String paddedKey = padKey(secretKey);
        //Key aesKey = new SecretKeySpec(paddedKey.getBytes(StandardCharsets.UTF_8), "AES");
        SecretKeySpec aesKey = deriveKey(secretKey);

        log.debug("==== decrypt: AES key created");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        log.debug("==== decrypt: cipher initialised for DECRYPT_MODE. algorithm={}", ALGORITHM);

        byte[] decodedBytes   = Base64.getUrlDecoder().decode(encryptedBase64);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        String decrypted      = new String(decryptedBytes, StandardCharsets.UTF_8);

        log.debug("==== decrypt: decryption complete. decryptedLength={}", decrypted.length());
        log.debug("<<<< decrypt: returning decrypted payload");
        return decrypted;
    }
}