package com.event.socialmodule.push;

import com.event.socialmodule.entity.BuddyNotification.Type;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PushTest {

    @Test
    void quietHoursAreTenPmToEightAm() {
        assertThat(PushService.isQuietHours(LocalTime.of(22, 0))).isTrue();
        assertThat(PushService.isQuietHours(LocalTime.of(3, 0))).isTrue();
        assertThat(PushService.isQuietHours(LocalTime.of(7, 59))).isTrue();
        assertThat(PushService.isQuietHours(LocalTime.of(8, 0))).isFalse();
        assertThat(PushService.isQuietHours(LocalTime.of(21, 59))).isFalse();
    }

    @Test
    void timeSensitiveTypesAreUrgentAndLinkToTheRightPage() {
        assertThat(PushListeners.urgent(Type.SEAT_OPENED)).isTrue();
        assertThat(PushListeners.urgent(Type.DIGEST)).isFalse();
        var seat = PushListeners.message(new BellNotificationCreated(1L, Type.SEAT_OPENED, 9L, "A seat opened"));
        assertThat(seat.url()).isEqualTo("/events/9");
        assertThat(seat.title()).isEqualTo("You got a seat");
        assertThat(PushListeners.message(new BellNotificationCreated(1L, Type.BUDDY_REQUEST, null, "Hi")).url()).isEqualTo("/friends?tab=requests");
    }

    /** Decrypts like a browser would (RFC 8291), checking the record layout and keys round-trip. */
    @Test
    void encryptedBodyDecryptsWithTheSubscriptionKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair ua = gen.generateKeyPair();
        byte[] uaPublic = WebPushCrypto.uncompressed((ECPublicKey) ua.getPublic());
        byte[] auth = new byte[16];
        Arrays.fill(auth, (byte) 7);
        byte[] body = WebPushCrypto.encrypt("hello".getBytes(StandardCharsets.UTF_8), WebPushCrypto.encode(uaPublic), WebPushCrypto.encode(auth));

        byte[] salt = Arrays.copyOfRange(body, 0, 16);
        int idLen = body[20];
        byte[] asPublic = Arrays.copyOfRange(body, 21, 21 + idLen);
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ua.getPrivate());
        ka.doPhase(WebPushCrypto.publicKey(asPublic), true);
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = WebPushCrypto.hkdf(auth, ka.generateSecret(), keyInfo, 32);
        byte[] cek = WebPushCrypto.hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = WebPushCrypto.hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plain = c.doFinal(Arrays.copyOfRange(body, 21 + idLen, body.length));
        assertThat(new String(plain, 0, plain.length - 1, StandardCharsets.UTF_8)).isEqualTo("hello");
        assertThat(plain[plain.length - 1]).isEqualTo((byte) 2);
    }

    private static byte[] concat(byte[]... parts) {
        var out = new java.io.ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }
}
