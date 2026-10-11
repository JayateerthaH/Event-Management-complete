package com.event.eventmodule.repository;

import com.event.eventmodule.entity.certificate.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
    Optional<CertificateTemplate> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}