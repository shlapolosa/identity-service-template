package com.template.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Identity Service (Pattern B profile authority).
 *
 * Auth0 is the token authority; this service is the profile authority:
 * it owns the users/profiles database, performs registration, profile
 * switching (via Auth0 app_metadata) and exposes rich profile data behind
 * JWT validation. See README-PATTERN-B.md.
 */
@SpringBootApplication
@EnableJpaAuditing
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
