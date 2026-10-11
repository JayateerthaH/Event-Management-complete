package com.event.eventmodule.repository;

import com.event.eventmodule.entity.event.WhatToBring;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatToBringRepository extends JpaRepository<WhatToBring, Long> {
    @Nullable List<WhatToBring> findByEvents_Id(Long eventId);
}