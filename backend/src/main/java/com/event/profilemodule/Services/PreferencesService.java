package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.Preferences;
import com.event.sharedmodule.exception.ResourceNotFoundException;

import java.util.List;

public interface PreferencesService {

    Preferences createPreference(Preferences preference);
    Preferences getPreference(Long id) throws ResourceNotFoundException;
    List<Preferences> getAllPreferences();
    Preferences updatePreference(Long id, Preferences preference) throws ResourceNotFoundException;
    void deletePreference(Long id) throws ResourceNotFoundException;

}
