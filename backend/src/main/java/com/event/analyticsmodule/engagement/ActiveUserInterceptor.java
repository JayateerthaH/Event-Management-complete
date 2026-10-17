package com.event.analyticsmodule.engagement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Counts a logged-in user as active for the day on their first authenticated request. */
@Configuration
@RequiredArgsConstructor
public class ActiveUserInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final ActivityRecorder recorder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object userId = request.getAttribute("userId");
        if (userId == null || response.getStatus() >= 400) return;
        try {
            recorder.markActive(Long.valueOf(userId.toString()));
        } catch (RuntimeException ignored) {
            // never affect the response
        }
    }
}
