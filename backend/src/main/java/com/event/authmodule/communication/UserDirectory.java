package com.event.authmodule.communication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Account facts other modules need for notifications: who the students are and their email (username). */
public interface UserDirectory {
    List<Long> studentIds();
    Map<Long, String> emails(Collection<Long> userIds);
}
