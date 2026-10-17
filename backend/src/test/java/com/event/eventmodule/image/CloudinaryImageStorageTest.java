package com.event.eventmodule.image;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cloudinary's API is replaced by a local server that records what it receives. */
class CloudinaryImageStorageTest {

    private HttpServer server;
    private final List<String> paths = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private String reply;
    private int status;
    private CloudinaryImageStorage storage;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            paths.add(ex.getRequestURI().getPath());
            bodies.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
        var credentials = CloudinaryImageStorage.Credentials.parse("cloudinary://111222333:s3cr3t@demo-cloud");
        storage = new CloudinaryImageStorage(credentials, "http://127.0.0.1:" + server.getAddress().getPort(), "campusevents");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static String sha1(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static String field(String body, String name) {
        int i = body.indexOf("name=\"" + name + "\"\r\n\r\n");
        if (i < 0) return null;
        int start = i + ("name=\"" + name + "\"\r\n\r\n").length();
        return body.substring(start, body.indexOf("\r\n", start));
    }

    @Test
    void uploadsSignedToTheEventsFolderAndReturnsTheUrl() throws Exception {
        status = 200;
        reply = "{\"secure_url\":\"https://res.cloudinary.com/demo-cloud/image/upload/v1/campusevents/events/42/abc.jpg\","
                + "\"public_id\":\"campusevents/events/42/abc\"}";

        var stored = storage.store(new MockMultipartFile("file", "stage.jpg", "image/jpeg", new byte[]{1, 2, 3}), 42L);

        assertThat(stored.url()).isEqualTo("https://res.cloudinary.com/demo-cloud/image/upload/v1/campusevents/events/42/abc.jpg");
        assertThat(stored.publicId()).isEqualTo("campusevents/events/42/abc");
        assertThat(paths).containsExactly("/v1_1/demo-cloud/image/upload");
        String body = bodies.get(0);
        assertThat(field(body, "api_key")).isEqualTo("111222333");
        assertThat(field(body, "folder")).isEqualTo("campusevents/events/42");
        String timestamp = field(body, "timestamp");
        assertThat(field(body, "signature")).isEqualTo(sha1("folder=campusevents/events/42&timestamp=" + timestamp + "s3cr3t"));
        assertThat(body).contains("filename=\"stage.jpg\"").doesNotContain("s3cr3t");   // the secret is never sent
    }

    @Test
    void reportsCloudinarysErrorMessage() {
        status = 401;
        reply = "{\"error\":{\"message\":\"Invalid Signature\"}}";
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}), 1L))
                .isInstanceOf(IOException.class).hasMessageContaining("Invalid Signature");
    }

    @Test
    void deletesWithASignedRequestAndSkipsDiskImages() throws Exception {
        status = 200;
        reply = "{\"result\":\"ok\"}";
        storage.delete("abc", "campusevents/events/42/abc");
        storage.delete("old-disk-file.jpg", null);   // stored on disk before Cloudinary: nothing to delete there

        assertThat(paths).containsExactly("/v1_1/demo-cloud/image/destroy");
        String form = bodies.get(0);
        String timestamp = form.replaceAll(".*timestamp=(\\d+).*", "$1");
        assertThat(form).contains("public_id=campusevents%2Fevents%2F42%2Fabc")
                .contains("signature=" + sha1("public_id=campusevents/events/42/abc&timestamp=" + timestamp + "s3cr3t"));
    }

    @Test
    void parsesCloudinaryUrl() {
        assertThat(CloudinaryImageStorage.Credentials.parse("cloudinary://key:secret@my-cloud"))
                .isEqualTo(new CloudinaryImageStorage.Credentials("my-cloud", "key", "secret"));
        assertThat(CloudinaryImageStorage.Credentials.parse("")).isNull();
        assertThat(CloudinaryImageStorage.Credentials.parse("https://key:secret@my-cloud")).isNull();
        assertThat(CloudinaryImageStorage.Credentials.parse("cloudinary://only-key@my-cloud")).isNull();
    }
}
