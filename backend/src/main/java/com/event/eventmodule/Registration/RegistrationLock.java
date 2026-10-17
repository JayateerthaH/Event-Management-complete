package com.event.eventmodule.Registration;

/** One lock for everything that changes an event's seat count (register, release, waitlist promotion). */
final class RegistrationLock {
    static final Object LOCK = new Object();

    private RegistrationLock() {}
}
