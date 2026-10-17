package com.event.authmodule.dtoPayload;

import com.event.authmodule.entitys.type.RoleType;

import java.util.Set;

/**
 * The ID token ("credential") from Google's sign-in button. {@code roles} matters only when the account is
 * new: ORGANIZER files an organizer request, exactly like the signup form.
 */
public record GoogleLoginRequestDto(String credential, Set<RoleType> roles) {
}
