package com.event.profilemodule.service_implementation.admin;

import com.event.profilemodule.Repository.PreferencesRepository;
import com.event.profilemodule.Services.PreferencesService;
import com.event.profilemodule.entity.admin.Preferences;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PreferencesServiceImpl implements PreferencesService {

    @Autowired
    private PreferencesRepository preferencesRepository;

    @Override
    public Preferences createPreference(Preferences preference) {
        log.info("Creating new preference: {}", preference.getPreferenceType());
        return preferencesRepository.save(preference);
    }

    @Override
    public Preferences getPreference(Long id) throws ResourceNotFoundException {
        return preferencesRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Preference not found with id {}", id);
                    return new ResourceNotFoundException("Preference not found with id " + id);
                });
    }

    @Override
    public List<Preferences> getAllPreferences() {
        return preferencesRepository.findAll();
    }

    @Override
    @Transactional
    public Preferences updatePreference(Long id, Preferences updatedPreference) throws ResourceNotFoundException {
        updatedPreference.setPreferenceId(id);
        return preferencesRepository.save(updatedPreference);
    }

    @Override
    @Transactional
    public void deletePreference(Long id) throws ResourceNotFoundException {
        // Spring Data 3+ deleteById silently ignores missing ids, so check explicitly
        if (!preferencesRepository.existsById(id)) {
            log.error("Attempted to delete non-existing preference with id {}", id);
            throw new ResourceNotFoundException("Preference not found with id " + id);
        }
        log.warn("Deleting preference with id {}", id);
        preferencesRepository.deleteById(id);
    }

}
