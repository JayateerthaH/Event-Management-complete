package com.event.socialmodule.portfolio;

import com.event.socialmodule.portfolio.PortfolioService.Mine;
import com.event.socialmodule.portfolio.PortfolioService.View;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;

/** The student's own portfolio settings, the public page data, and its link preview for LinkedIn/WhatsApp. */
@RestController
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final String frontendUrl;

    public PortfolioController(PortfolioService portfolioService, @Value("${app.frontend-url}") String frontendUrl) {
        this.portfolioService = portfolioService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public record PublishBody(Boolean published) {}

    @GetMapping("/social/portfolio")
    public Mine mine(@RequestAttribute("userId") String userId) {
        return portfolioService.mine(Long.valueOf(userId));
    }

    @PutMapping("/social/portfolio")
    public Mine publish(@RequestAttribute("userId") String userId, @RequestBody PublishBody body) {
        if (body == null || body.published() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "published is required");
        return portfolioService.setPublished(Long.valueOf(userId), body.published());
    }

    @PostMapping("/social/portfolio/reset")
    public Mine reset(@RequestAttribute("userId") String userId) {
        return portfolioService.resetLink(Long.valueOf(userId));
    }

    @GetMapping("/public/portfolio/{slug}")
    public ResponseEntity<View> publicPortfolio(@PathVariable String slug) {
        View view = portfolioService.published(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This portfolio is private or the link was reset"));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(1))).body(view);
    }

    /** vercel.json sends link-preview crawlers' requests for /p/{slug} here (they don't run JavaScript). */
    @GetMapping(value = "/public/share/portfolio/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String slug) {
        View v = portfolioService.published(slug).orElse(null);
        String url = frontendUrl + "/p/" + HtmlUtils.htmlEscape(slug);
        String title = v == null ? "CampusEvents" : v.name() + " · Campus portfolio";
        String description = v == null ? "Discover and register for campus events." : describe(v);
        String html = """
                <!doctype html>
                <html lang="en"><head>
                <meta charset="utf-8">
                <title>%1$s</title>
                <meta name="description" content="%2$s">
                <meta property="og:type" content="profile">
                <meta property="og:site_name" content="CampusEvents">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:url" content="%3$s">
                <meta property="og:image" content="%4$s">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="robots" content="noindex">
                <link rel="canonical" href="%3$s">
                <meta http-equiv="refresh" content="0; url=%3$s">
                </head><body><p><a href="%3$s">%1$s</a></p><p>%2$s</p></body></html>
                """.formatted(esc(title), esc(description), esc(url), esc(frontendUrl + "/og-default.jpg"));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic()).body(html);
    }

    static String describe(View v) {
        StringBuilder s = new StringBuilder();
        if (v.department() != null) s.append(v.department()).append(". ");
        s.append(count(v.stats().events(), "event")).append(" attended (QR-verified), ")
                .append(count(v.stats().certificates(), "certificate")).append(", ")
                .append(count(v.stats().badges(), "badge")).append('.');
        return s.toString();
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }
}
