package com.event.authmodule.communication;

import java.util.Collection;
import java.util.Map;

/** Organizer accounts as other modules may see them: whether an account is an organizer, and its name. */
public interface OrganizerDirectory {
    boolean isOrganizer(Long userId);
    Map<Long, String> names(Collection<Long> organizerIds);
}
