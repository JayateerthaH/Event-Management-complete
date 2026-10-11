package com.event.eventmodule.controller;

import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.Registration.Register;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.certificate.CertificateResponseDto;
import com.event.eventmodule.repository.FeedBackRepository;
import com.event.eventmodule.service.CertificateTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
 import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.text.Normalizer;
import java.util.List;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
@Slf4j
public class CertificateTemplateController {

    private final CertificateTemplateService service;
    private final RegisterRepository registerRepository;
    private final FeedBackRepository feedBackRepository;

    @PostMapping("/organizer/upload-certificate-template")
    @RequiresEventAccess
    public ResponseEntity<?> upload(
            @RequestParam MultipartFile file,
            @RequestParam Long eventId
    ) throws IOException {

        service.uploadTemplate(file, eventId);
        return ResponseEntity.ok("Template uploaded successfully");

    }

    @GetMapping("/eligible")
    public ResponseEntity<List<CertificateResponseDto>> getEligibleCertificates(@RequestAttribute("userId") String userId) {


        List<Register> registers =
                service.getEligibleCertificates(Long.valueOf(userId));

        List<CertificateResponseDto> response =
                registers.stream()
                        .map(this::mapToDto)
                        .toList();

        return ResponseEntity.ok(response);
    }

    private CertificateResponseDto mapToDto(Register register) {

        return new CertificateResponseDto(
                register.getEventsDto().getId(),
                register.getEventsDto().getName(),
                register.getEventsDto().getEventDate()
        );
    }



    @GetMapping(value = "/download/{eventId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long eventId,
            @RequestAttribute("userId") String userId
    ) throws Exception {

        // is attended true

        String username = registerRepository.findNameByUserIdAndEventId(Long.valueOf(userId),eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No attended registration found for this event"));

        if (!feedBackRepository.existsByUserIdAndEventId(Long.valueOf(userId), eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FeedBack is not given");
        }

        byte[] pdf = service.generateCertificatePdf(eventId,userId, username);

        String filename = "certificate-" + eventId + "-" + safeFileName(username) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);

    }

    private String safeFileName(String input) {
        if (input == null) return "user";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);
        String cleaned = normalized.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return cleaned.isBlank() ? "user" : cleaned;
    }

}
