package com.event.socialmodule.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push with the JDK only: payload encryption (RFC 8291, aes128gcm) and VAPID authorization (RFC 8292).
 * Keys are base64url: the VAPID public key is the 65-byte uncompressed point, the private key the 32-byte scalar.
 */
public final class WebPushCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    static final int RECORD_SIZE = 4096;
    private static final ECParameterSpec P256 = p256();

    private WebPushCrypto() {}

    /** The request body for a push message to a subscription with these keys. */
    public static byte[] encrypt(byte[] payload, String p256dh, String auth) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(P256, RANDOM);
        return encrypt(payload, p256dh, auth, gen.generateKeyPair(), randomBytes(16));
    }

    static byte[] encrypt(byte[] payload, String p256dh, String auth, KeyPair ephemeral, byte[] salt) throws GeneralSecurityException {
        byte[] uaPublic = decode(p256dh);
        byte[] authSecret = decode(auth);
        byte[] asPublic = uncompressed((ECPublicKey) ephemeral.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(publicKey(uaPublic), true);
        byte[] ecdhSecret = ka.generateSecret();

        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
        byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        if (payload.length > RECORD_SIZE - 17) throw new IllegalArgumentException("Push payload too large");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(concat(payload, new byte[]{2}));   // 0x02: last (only) record, no padding

        ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
        header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
        return concat(header.array(), ciphertext);
    }

    /** "vapid t=<jwt>, k=<public key>" for a push service endpoint. */
    public static String vapidAuthorization(String endpoint, String subject, String publicKey, String privateKey, Instant now)
            throws GeneralSecurityException {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        String header = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = B64.encodeToString(("{\"aud\":\"" + audience + "\",\"exp\":" + now.plusSeconds(12 * 3600).getEpochSecond()
                + ",\"sub\":\"" + subject.replace("\"", "") + "\"}").getBytes(StandardCharsets.UTF_8));
        Signature es256 = Signature.getInstance("SHA256withECDSAinP1363Format");   // raw R||S, as JWS needs
        es256.initSign(privateKey(decode(privateKey)));
        es256.update((header + "." + claims).getBytes(StandardCharsets.US_ASCII));
        String jwt = header + "." + claims + "." + B64.encodeToString(es256.sign());
        return "vapid t=" + jwt + ", k=" + publicKey;
    }

    /** Checks a key pair loads and matches, so a misconfigured server fails at startup, not per message. */
    static void validateKeys(String publicKey, String privateKey) throws GeneralSecurityException {
        byte[] pub = decode(publicKey);
        if (pub.length != 65 || pub[0] != 4) throw new InvalidKeyException("VAPID public key must be a 65-byte uncompressed P-256 point");
        PrivateKey priv = privateKey(decode(privateKey));
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(priv);
        s.update(new byte[]{1});
        byte[] sig = s.sign();
        s.initVerify(publicKey(pub));
        s.update(new byte[]{1});
        if (!s.verify(sig)) throw new InvalidKeyException("VAPID private key doesn't match the public key");
    }

    static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        return Arrays.copyOf(mac.doFinal(concat(info, new byte[]{1})), length);
    }

    static ECPublicKey publicKey(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 4) throw new InvalidKeyException("Expected an uncompressed P-256 point");
        ECPoint w = new ECPoint(new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33)), new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, P256));
    }

    static PrivateKey privateKey(byte[] d) throws GeneralSecurityException {
        if (d.length != 32) throw new InvalidKeyException("VAPID private key must be 32 bytes");
        return KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
    }

    static byte[] uncompressed(ECPublicKey key) {
        return concat(new byte[]{4}, fixed32(key.getW().getAffineX()), fixed32(key.getW().getAffineY()));
    }

    static String encode(byte[] bytes) {
        return B64.encodeToString(bytes);
    }

    static byte[] decode(String b64url) {
        return B64D.decode(b64url.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    private static byte[] fixed32(BigInteger n) {
        byte[] raw = n.toByteArray();
        byte[] out = new byte[32];
        int copy = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available in this JVM", e);
        }
    }
}
