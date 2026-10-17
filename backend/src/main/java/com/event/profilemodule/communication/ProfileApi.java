package com.event.profilemodule.communication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only view of a student's profile choices for other modules. */
public interface ProfileApi {

    /** Names of the interests the student selected (empty if none or no profile yet). */
    List<String> interestNames(Long userId);

    /** Volunteer preference types the student selected. */
    List<String> preferenceTypes(Long userId);

    /** Department from the student's profile, if filled in. */
    Optional<String> department(Long userId);

    /** Name from the profile of every user who filled one in, keyed by user id. */
    Map<Long, String> displayNames();
}
