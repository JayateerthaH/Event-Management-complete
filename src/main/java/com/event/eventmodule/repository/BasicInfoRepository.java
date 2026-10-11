package com.event.eventmodule.repository;

import com.event.eventmodule.entity.home.BasicInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasicInfoRepository extends JpaRepository<BasicInfo, Long> {
}