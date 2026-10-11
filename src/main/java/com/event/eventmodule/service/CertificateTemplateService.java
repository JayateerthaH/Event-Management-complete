package com.event.eventmodule.service;


import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.Registration.Register;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.certificate.CertificateTemplate;
import com.event.eventmodule.repository.CertificateTemplateRepository;
import com.event.eventmodule.repository.EventsRepository;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateTemplateService {

    private final CertificateTemplateRepository repository;
    private final RegisterRepository registerRepository;
    private final EventsRepository eventsRepository;
    private static final String BASE_DIR = "certificates";

    public void uploadTemplate(MultipartFile file, Long eventId) throws IOException, IOException {

        String originalName = file.getOriginalFilename();
        if (file.isEmpty() || originalName == null || !originalName.toLowerCase().endsWith(".html")) {
            throw new IllegalArgumentException("Only HTML files allowed");
        }

        Path dir = Paths.get(BASE_DIR, "event-" + eventId);
        Files.createDirectories(dir);

        Path filePath = dir.resolve("certificate.html");
        Files.write(filePath, file.getBytes());

        CertificateTemplate template =
                repository.findByEventId(eventId)
                        .orElse(new CertificateTemplate());

        template.setEventId(eventId);
        template.setFilePath(filePath.toAbsolutePath().toString());

        repository.save(template);
    }

    public List<Register> getEligibleCertificates(Long userId) {
        return registerRepository.findEligibleCertificates(userId);
    }

    public byte[] generateCertificatePdf(Long eventId, String userId, String userName) throws IOException {
//        if (feedBackTableRepository.existsByEventIdAndUserId(eventId, Long.valueOf(userId))) {
//            throw new RuntimeException("Submit Feedback and Download Certificate");
//        }
        Path dir = Paths.get(BASE_DIR, "event-" + eventId);
        Path htmlPath = dir.resolve("certificate.html");

        if (!Files.exists(htmlPath)) {
            throw new ResourceNotFoundException("Certificate template not found for event " + eventId);
        }

        String html = Files.readString(htmlPath);

        // inject data
        html = html.replace("{{name}}", escapeHtml(userName));

        // Fetch event details for remaining placeholders
        Events event = eventsRepository.findById(eventId).orElse(null);
        String courseName = (event != null && event.getName() != null) ? event.getName() : "Event";
        String eventDate = "";
        if (event != null && event.getEventDate() != null) {
            eventDate = new SimpleDateFormat("MMMM d, yyyy").format(event.getEventDate());
        }
        String certId = "CERT-" + eventId + "-" + userId;
        String director = "Event Director";

        html = html.replace("{{course}}", escapeHtml(courseName));
        html = html.replace("{{date}}", escapeHtml(eventDate));
        html = html.replace("{{certId}}", escapeHtml(certId));
        html = html.replace("{{director}}", escapeHtml(director));

        return renderPdf(html, dir);
    }

    // Every protocol java.net.URL can open; each gets our guarded stream so nothing falls back to URL.openStream()
    private static final String[] URL_PROTOCOLS = {"file", "http", "https", "ftp", "jar", "jrt"};

    /**
     * Renders organizer-supplied HTML. The template is untrusted, so the renderer may only load
     * {@code data:} URIs and files inside the template's own folder. Without this, a template could
     * embed any server file (e.g. {@code <img src="file:///etc/passwd">}) into the PDF or make the
     * server fetch internal URLs (SSRF).
     *
     * Enforced at two layers, because the library's own useExternalResourceAccessControl is not
     * applied to images in openhtmltopdf 1.0.10 (PdfBoxUserAgent.getImageResource skips it):
     *  1. the URI resolver refuses disallowed URIs before anything is loaded;
     *  2. the stream factory re-checks at the point of I/O as a backstop.
     */
    static byte[] renderPdf(String html, Path templateDir) throws IOException {
        Path allowedDir = templateDir.toRealPath();
        String baseUri = allowedDir.toUri().toString();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            // baseUri to resolve relative images/css in same folder
            builder.withHtmlContent(html, baseUri);
            builder.useUriResolver((base, uri) -> resolveAllowed(base != null ? base : baseUri, uri, allowedDir));
            builder.useProtocolsStreamImplementation(uri -> new GuardedFileStream(uri, allowedDir), URL_PROTOCOLS);
            builder.toStream(os);

            // helpful for certificates with background images/colors
            builder.useFastMode();
            builder.run();

            return os.toByteArray();
        }
    }

    // Resolves uri against base; returns null (= refuse to load) unless the result is allowed.
    static String resolveAllowed(String base, String uri, Path allowedDir) {
        if (uri == null) {
            return null;
        }
        try {
            String resolved = URI.create(base).resolve(uri.trim()).toString();
            return isAllowedResource(resolved, allowedDir) ? resolved : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Allow inline data: URIs, or a file: URI whose real path (after "..", symlinks) is inside allowedDir.
    static boolean isAllowedResource(String uri, Path allowedDir) {
        if (uri == null) {
            return false;
        }
        if (uri.regionMatches(true, 0, "data:", 0, 5)) {
            return true;
        }
        return allowedFile(uri, allowedDir) != null;
    }

    // The real path of an allowed file: URI, or null for any other scheme, outside path or missing file.
    private static Path allowedFile(String uri, Path allowedDir) {
        try {
            URI parsed = URI.create(uri);
            if (!"file".equalsIgnoreCase(parsed.getScheme())) {
                return null;    // http(s), ftp, jar, ... — no network or archive access
            }
            Path real = Paths.get(parsed).toRealPath();
            return real.startsWith(allowedDir) && Files.isRegularFile(real) ? real : null;
        } catch (Exception e) {
            return null;        // malformed URI or missing file
        }
    }

    // Opens only allowed files; every other URI yields no content.
    private record GuardedFileStream(String uri, Path allowedDir) implements FSStream {
        @Override
        public InputStream getStream() {
            Path file = allowedFile(uri, allowedDir);
            try {
                return file != null ? Files.newInputStream(file) : null;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public Reader getReader() {
            InputStream in = getStream();
            return in != null ? new InputStreamReader(in, StandardCharsets.UTF_8) : null;
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");

    }

//    public void downloadCertificate(Long eventId) {
//        //first check the feedback table if not present userid and event id then only
//
//
//        // download certificate logic i have to write//
//    }
}
