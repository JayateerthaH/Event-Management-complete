package com.event.eventmodule.service;

import com.sun.net.httpserver.HttpServer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Certificate templates are organizer-supplied HTML. Rendering must not let a template pull in
 * files outside its own folder or reach the network.
 */
class CertificateTemplateRenderingTest {

    @TempDir
    Path root;

    private Path templateDir;
    private Path secretDir;

    @BeforeEach
    void setUp() throws IOException {
        templateDir = Files.createDirectories(root.resolve("certificates/event-1"));
        secretDir = Files.createDirectories(root.resolve("secret"));
        writePng(templateDir.resolve("logo.png"));
        writePng(secretDir.resolve("secret.png"));
        Files.writeString(secretDir.resolve("secret.txt"), "LEAKED_SECRET_VALUE");
    }

    @Test
    void imageInsideTemplateFolderStillRenders() throws Exception {
        // Control case: proves the image check below can detect an embedded image at all
        byte[] pdf = render("<img src=\"logo.png\" width=\"50\" height=\"50\"/>");
        assertThat(imageCount(pdf)).isEqualTo(1);
    }

    @Test
    void imageFromOutsideTemplateFolderIsNotEmbedded() throws Exception {
        String absolute = secretDir.resolve("secret.png").toUri().toString();
        assertThat(imageCount(render("<img src=\"" + absolute + "\" width=\"50\" height=\"50\"/>"))).isZero();
    }

    @Test
    void pathTraversalOutOfTemplateFolderIsNotEmbedded() throws Exception {
        assertThat(imageCount(render("<img src=\"../../secret/secret.png\" width=\"50\" height=\"50\"/>"))).isZero();
    }

    @Test
    void symlinkPointingOutOfTemplateFolderIsNotEmbedded() throws Exception {
        Files.createSymbolicLink(templateDir.resolve("link.png"), secretDir.resolve("secret.png"));
        assertThat(imageCount(render("<img src=\"link.png\" width=\"50\" height=\"50\"/>"))).isZero();
    }

    @Test
    void externalEntityCannotReadFiles() throws Exception {
        String secretUri = secretDir.resolve("secret.txt").toUri().toString();
        String html = "<?xml version=\"1.0\"?>\n<!DOCTYPE html [<!ENTITY xxe SYSTEM \"" + secretUri + "\">]>\n"
                + "<html><body><p>Name: &xxe;</p></body></html>";
        byte[] pdf;
        try {
            pdf = CertificateTemplateService.renderPdf(html, templateDir);
        } catch (Exception rejected) {
            return; // parser refusing the DOCTYPE outright is also safe
        }
        assertThat(text(pdf)).doesNotContain("LEAKED_SECRET_VALUE");
    }

    @Test
    void remoteUrlsAreNeverFetched() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            render("<link rel=\"stylesheet\" href=\"" + base + "/style.css\"/>"
                    + "<img src=\"" + base + "/pixel.png\" width=\"5\" height=\"5\"/>");
        } finally {
            server.stop(0);
        }
        assertThat(hits.get()).as("requests made to the internal server").isZero();
    }

    @Test
    void policyRejectsNonFileSchemesAndAcceptsDataUris() throws Exception {
        Path dir = templateDir.toRealPath();
        assertThat(CertificateTemplateService.isAllowedResource("data:image/png;base64,AAAA", dir)).isTrue();
        assertThat(CertificateTemplateService.isAllowedResource(dir.resolve("logo.png").toUri().toString(), dir)).isTrue();
        assertThat(CertificateTemplateService.isAllowedResource("http://169.254.169.254/latest/meta-data/", dir)).isFalse();
        assertThat(CertificateTemplateService.isAllowedResource("jar:file:/x.jar!/a.png", dir)).isFalse();
        assertThat(CertificateTemplateService.isAllowedResource("file:///etc/passwd", dir)).isFalse();
        assertThat(CertificateTemplateService.isAllowedResource("not a uri", dir)).isFalse();
        assertThat(CertificateTemplateService.isAllowedResource(null, dir)).isFalse();
    }

    private byte[] render(String body) throws IOException {
        return CertificateTemplateService.renderPdf("<html><body>" + body + "</body></html>", templateDir);
    }

    private static void writePng(Path path) throws IOException {
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", path.toFile());
    }

    private static int imageCount(byte[] pdf) throws IOException {
        int count = 0;
        try (PDDocument doc = PDDocument.load(pdf)) {
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) count++;
                }
            }
        }
        return count;
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
