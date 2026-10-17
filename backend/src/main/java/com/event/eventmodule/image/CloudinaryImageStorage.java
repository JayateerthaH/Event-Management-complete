package com.event.eventmodule.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Images on Cloudinary through its REST upload API (signed with the API secret), one folder per event.
 * Configured with Cloudinary's standard CLOUDINARY_URL: cloudinary://API_KEY:API_SECRET@CLOUD_NAME
 */
@Slf4j
public class CloudinaryImageStorage implements ImageStorage {

    public record Credentials(String cloudName, String apiKey, String apiSecret) {
        /** Parses cloudinary://key:secret@cloud; null if the value is missing or malformed. */
        public static Credentials parse(String url) {
            if (url == null || url.isBlank()) return null;
            try {
                URI uri = URI.create(url.trim());
                String[] keySecret = uri.getRawUserInfo() == null ? new String[0] : uri.getRawUserInfo().split(":", 2);
                if (!"cloudinary".equals(uri.getScheme()) || keySecret.length != 2 || uri.getHost() == null
                        || keySecret[0].isBlank() || keySecret[1].isBlank()) return null;
                return new Credentials(uri.getHost(), keySecret[0], keySecret[1]);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Credentials credentials;
    private final String apiBase;
    private final String folderPrefix;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final FileService fileService;   // images uploaded to disk before Cloudinary was set up

    public CloudinaryImageStorage(Credentials credentials, String apiBase, String folderPrefix) {
        this(credentials, apiBase, folderPrefix, null);
    }

    public CloudinaryImageStorage(Credentials credentials, String apiBase, String folderPrefix, FileService fileService) {
        this.fileService = fileService;
        this.credentials = credentials;
        this.apiBase = (apiBase == null || apiBase.isBlank() ? "https://api.cloudinary.com" : apiBase).replaceAll("/+$", "");
        this.folderPrefix = folderPrefix;
    }

    @Override
    public StoredImage store(MultipartFile file, Long eventId) throws IOException {
        String folder = folderPrefix + "/events/" + eventId;
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = sign("folder=" + folder + "&timestamp=" + timestamp);

        String boundary = "----campusevents" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, boundary, "api_key", credentials.apiKey());
        field(body, boundary, "timestamp", timestamp);
        field(body, boundary, "folder", folder);
        field(body, boundary, "signature", signature);
        String filename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename().replaceAll("[\"\\r\\n]", "_");
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: " + file.getContentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(file.getBytes());
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(endpoint("upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        JsonNode reply = send(request);
        String url = reply.path("secure_url").asString("");
        String publicId = reply.path("public_id").asString("");
        if (url.isBlank() || publicId.isBlank()) {
            throw new IOException("Image upload failed: " + errorOf(reply));
        }
        log.info("[Cloudinary] Uploaded {} for event {} ({} bytes)", publicId, eventId, file.getSize());
        return new StoredImage(publicId.substring(publicId.lastIndexOf('/') + 1), url, publicId);
    }

    /** Only asked about images stored on disk before Cloudinary was set up (Cloudinary images have a publicId). */
    @Override
    public boolean exists(String fileName) {
        return fileService == null || fileService.exists(fileName);
    }

    @Override
    public void delete(String fileName, String publicId) {
        if (publicId == null || publicId.isBlank()) {   // stored on disk before Cloudinary was set up
            if (fileService != null) fileService.deleteFile(fileName);
            return;
        }
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String form = "public_id=" + enc(publicId) + "&timestamp=" + timestamp + "&api_key=" + enc(credentials.apiKey())
                    + "&signature=" + sign("public_id=" + publicId + "&timestamp=" + timestamp);
            HttpRequest request = HttpRequest.newBuilder(endpoint("destroy"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            JsonNode reply = send(request);
            log.info("[Cloudinary] Deleted {}: {}", publicId, reply.path("result").asString(errorOf(reply)));
        } catch (IOException e) {
            log.warn("[Cloudinary] Could not delete {}: {}", publicId, e.getMessage());
        }
    }

    @Override
    public String name() {
        return "Cloudinary (" + credentials.cloudName() + ")";
    }

    private URI endpoint(String action) {
        return URI.create(apiBase + "/v1_1/" + credentials.cloudName() + "/image/" + action);
    }

    private JsonNode send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode reply;
            try {
                reply = JSON.readTree(response.body());
            } catch (RuntimeException e) {
                throw new IOException("Unexpected reply from Cloudinary (HTTP " + response.statusCode() + ")");
            }
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Cloudinary refused the request (HTTP " + response.statusCode() + "): " + errorOf(reply));
            }
            return reply;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting Cloudinary", e);
        }
    }

    private static String errorOf(JsonNode reply) {
        return reply.path("error").path("message").asString("unknown error");
    }

    /** Cloudinary's signature: SHA-1 of the sorted parameters followed by the API secret. */
    String sign(String sortedParams) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((sortedParams + credentials.apiSecret()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void field(ByteArrayOutputStream body, String boundary, String name, String value) throws IOException {
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
