package com.event.sharedmodule;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards endpoints that take a user id from the request: the caller may only act on their own id,
 * unless they are an admin. The annotated method must have a parameter named {@code userId}.
 * The principal is the UserInfo set by JwtAuthFilter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
public @interface SelfOrAdmin {
}
