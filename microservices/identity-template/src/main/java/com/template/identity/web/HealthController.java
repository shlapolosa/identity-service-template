package com.template.identity.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform health contract: the webservice ComponentDefinition probes
 * {@code /health} (python-template convention) and the expose-api trait polls
 * {@code /openapi.json}. Spring's actuator lives at /actuator/health, so
 * without these aliases Knative kills healthy revisions with readiness 503
 * (caught patient2-identity day-0, 2026-06-07).
 */
@RestController
public class HealthController {

    @Value("${spring.application.name:identity-template}")
    private String serviceName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", serviceName));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of("status", "ready", "service", serviceName));
    }
}
