package com.event.eventmodule.repository;

import com.event.eventmodule.entity.event.FAQs;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FAQsRepository extends JpaRepository<FAQs, Long> {

    @Nullable List<FAQs> findByEvents_Id(Long eventId);
}