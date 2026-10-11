package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesRepository extends JpaRepository<Preferences, Long> {

    Optional<Preferences> findByPreferenceType(String preferenceType);

}