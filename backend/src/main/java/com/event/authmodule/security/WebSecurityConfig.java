package com.event.authmodule.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;


@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final HandlerExceptionResolver  handlerExceptionResolver;

    // Frontend origin(s), comma-separated; configurable per environment
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(eventAppCorsConfigurationSource()))
                .csrf(csrfConfig -> csrfConfig.disable())
                .sessionManagement(sessionConfig ->
                        sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // ⭐ VERY IMPORTANT
                                .requestMatchers("/public/**", "/auth/**").permitAll()
                                // Read-only public content: the frontend's Home, Events and Event Details pages
                                // are browsable without logging in. Writes and user-specific data stay protected.
                                .requestMatchers(HttpMethod.GET,
                                        "/home/basic-info", "/home/category", "/home/featured", "/events/filter-options",
                                        "/events/{eventId:\\d+}",
                                        "/events/{eventId:\\d+}/about", "/events/{eventId:\\d+}/details",
                                        "/events/{eventId:\\d+}/schedule", "/events/{eventId:\\d+}/faqs",
                                        "/events/{eventId:\\d+}/highlights", "/events/{eventId:\\d+}/speakers",
                                        "/events/{eventId:\\d+}/whatToBring",
                                        "/event/images/{eventId:\\d+}", "/images/**",
                                        "/ai/events/{eventId:\\d+}/similar").permitAll()   // no LLM, approved events only
                                .requestMatchers(HttpMethod.POST, "/events/filter").permitAll()   // approved events only
//                        .requestMatchers("/admin/**").hasRole(ADMIN.name())
//                        .requestMatchers("/organizer/**").hasAnyRole(ORGANIZER.name(), ADMIN.name())
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oAuth2 -> oAuth2
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 error: {}", exception.getMessage());
                            handlerExceptionResolver.resolveException(request, response, null, exception);
                        })
                        .successHandler(oAuth2SuccessHandler)
                )
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                        // Unauthenticated API calls get a 401 ApiError instead of a 302 redirect to the OAuth login page
                        .authenticationEntryPoint((request, response, authException) ->
                                handlerExceptionResolver.resolveException(request, response, null, authException))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
                        }));

//                .formLogin();
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource eventAppCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
