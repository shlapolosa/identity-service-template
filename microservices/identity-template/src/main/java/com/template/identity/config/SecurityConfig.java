package com.template.identity.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Pattern B security.
 *
 * Verifiers re-validate JWTs via JWKS (issuer + audience). When
 * {@code JWT_JWK_SET_URI} (spring.security.oauth2.resourceserver.jwt.jwk-set-uri)
 * is set, the API is a JWT resource server: /actuator/health and registration
 * are public; everything else requires a valid bearer token.
 *
 * When no jwk-set-uri is configured (local/dev with H2), there is no JWT
 * decoder to wire, so a permissive chain is used so the context still starts
 * and the service is exercisable locally. The two chains are mutually exclusive
 * via {@link ConditionalOnProperty}.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/api/v1/profiles/register",
            "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**"
    };

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            matchIfMissing = true, havingValue = "__never__")
    public SecurityFilterChain permissiveSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.disable())) // allow H2 console frames in dev
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
