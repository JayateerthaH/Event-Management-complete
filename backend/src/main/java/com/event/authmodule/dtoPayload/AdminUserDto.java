package com.event.authmodule.dtoPayload;

import java.util.List;

/** One account in the admin user search: every role it has, and its profile name if filled in. */
public record AdminUserDto(Long id, String email, String name, List<String> roles) {
}
