package com.healthcare.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;
/**
 * Encryption, hashing and keyword-token helpers.
 * NOTE: simplified "ABSE-style" scheme = AES-256-GCM + attribute policy check + HMAC keyword index.
 * A full pairing-based ABSE library (e.g. JPBC / CP-ABE) can replace encrypt()/decrypt() later.
 */
@Service
public class CryptoService {
    private final SecretKey masterKey;
    private final SecretKey indexKey;
    private final SecureRandom rnd = new SecureRandom();

    public CryptoService(@Value("${app.master-key}") String master, @Value("${app.index-key}") String index) {
        this.masterKey = new SecretKeySpec(Base64.getDecoder().decode(master), "AES");
        this.indexKey = new SecretKeySpec(Base64.getDecoder().decode(index), "HmacSHA256");
    }
    public record Encrypted(byte[] cipher, String wrappedKey) {}

    /** Encrypt a file with a fresh data key; the data key is wrapped with the master key. */
    public Encrypted encrypt(byte[] plain) throws GeneralSecurityException {
        KeyGenerator kg = KeyGenerator.getInstance("AES"); kg.init(256);
        SecretKey dataKey = kg.generateKey();
        byte[] cipher = encryptGcm(dataKey, plain);
        String wrapped = Base64.getEncoder().encodeToString(encryptGcm(masterKey, dataKey.getEncoded()));
        return new Encrypted(cipher, wrapped);
    }
    public byte[] decrypt(byte[] cipher, String wrappedKey) throws GeneralSecurityException {
        byte[] raw = decryptGcm(masterKey, Base64.getDecoder().decode(wrappedKey));
        return decryptGcm(new SecretKeySpec(raw, "AES"), cipher);
    }
    /** output = iv(12 bytes) + ciphertext */
    private byte[] encryptGcm(SecretKey key, byte[] in) throws GeneralSecurityException {
        byte[] iv = new byte[12]; rnd.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(in);
        byte[] out = new byte[12 + ct.length];
        System.arraycopy(iv, 0, out, 0, 12); System.arraycopy(ct, 0, out, 12, ct.length);
        return out;
    }
    private byte[] decryptGcm(SecretKey key, byte[] in) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(in, 0, 12)));
        return c.doFinal(in, 12, in.length - 12);
    }
    public String sha256Hex(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** Searchable token: the server stores only HMAC(keyword), never the keyword itself. */
    public String keywordToken(String keyword) {
        try { Mac m = Mac.getInstance("HmacSHA256"); m.init(indexKey);
              return HexFormat.of().formatHex(m.doFinal(keyword.trim().toLowerCase().getBytes())); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    /** Attribute-based access check: the user must hold ALL attributes listed in the policy. */
    public boolean policySatisfied(String policy, String userAttrs) {
        Set<String> have = split(userAttrs);
        return split(policy).stream().allMatch(have::contains);
    }
    private Set<String> split(String s) {
        Set<String> out = new HashSet<>();
        if (s != null) for (String p : s.split(",")) if (!p.isBlank()) out.add(p.trim().toLowerCase());
        return out;
    }
}
