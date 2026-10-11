package com.event.eventmodule.config;

import com.event.eventmodule.entity.event.Collaborators;
import com.event.eventmodule.entity.feedback.EventLink;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class EventAccessAspect {

    private final EventAccessService eventAccessService;

    @Before("@annotation(RequiresEventAccess)")
    public void checkAccess(JoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        // ✅ Changed from getHeader() → getAttribute()
        String userId = (String) request.getAttribute("userId");
        String rolesHeader = (String) request.getAttribute("userRoles");
        String userEmail = (String) request.getAttribute("username");

        Long eventId = extractEventId(joinPoint.getArgs())
                .orElseThrow(() -> new IllegalArgumentException("eventId not found"));

        if (!eventAccessService.isAllowed(eventId, userId, rolesHeader, userEmail)) {
            throw new AccessDeniedException("Only creator or collaborator is allowed");
        }
    }

    private Optional<Long> extractEventId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) return Optional.of((Long) arg);
            if (arg instanceof Collaborators) return Optional.ofNullable(((Collaborators) arg).getEventId());
            if (arg instanceof EventLink) return Optional.ofNullable(((EventLink) arg).getEventId());
        }
        return Optional.empty();
    }
}