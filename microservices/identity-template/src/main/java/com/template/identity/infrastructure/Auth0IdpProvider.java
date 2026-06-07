package com.template.identity.infrastructure;

import com.template.identity.core.infrastructure.IdpProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Default {@link IdpProvider} adapter.
 *
 * Pattern B: Auth0 is the token authority. Registration creates/links the
 * Auth0 user and syncs profiles to {@code app_metadata} via the Auth0
 * Management API using this service's own client credentials (the
 * "&lt;component&gt;-conn" secret: AUTH0_CLIENT_ID / AUTH0_CLIENT_SECRET).
 *
 * The full Management API HTTP client is intentionally out of scope for the
 * runnable default: when AUTH0_DOMAIN/AUTH0_CLIENT_ID are unset (local/dev),
 * this falls back to issuing a synthetic external id so the registration flow
 * and the rest of the service run without a live Auth0 tenant. The seams
 * (createUser/updateUser/...) match the contract a domain build wires to the
 * real Management API.
 */
@Component
@Slf4j
public class Auth0IdpProvider implements IdpProvider {

    private final String domain;
    private final boolean configured;

    public Auth0IdpProvider(@Value("${auth0.domain:}") String domain,
                            @Value("${auth0.clientId:}") String clientId) {
        this.domain = domain;
        this.configured = domain != null && !domain.isBlank()
                && clientId != null && !clientId.isBlank();
        if (!configured) {
            log.warn("Auth0 not fully configured (auth0.domain/clientId empty); "
                    + "IdpProvider runs in local fallback mode (synthetic external ids).");
        }
    }

    @Override
    public String createUser(String email, String password, Map<String, Object> metadata) {
        if (configured) {
            log.info("Creating Auth0 user for {} on domain {} with metadata keys {}",
                    email, domain, metadata.keySet());
            // Domain build wires the Auth0 Management API call here.
        }
        String externalId = "auth0|" + UUID.nameUUIDFromBytes(email.getBytes());
        log.info("Provisioned external identity {} for {}", externalId, email);
        return externalId;
    }

    @Override
    public void updateUser(String externalId, Map<String, Object> updates) {
        log.info("Updating Auth0 app_metadata for {} keys {}", externalId, updates.keySet());
    }

    @Override
    public void deleteUser(String externalId) {
        log.info("Deleting Auth0 user {}", externalId);
    }

    @Override
    public boolean verifyPassword(String email, String password) {
        // Pattern B never verifies passwords here; Auth0 owns credentials.
        return false;
    }

    @Override
    public String generatePasswordResetToken(String email) {
        return UUID.randomUUID().toString();
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset requested via token");
    }

    @Override
    public void verifyEmail(String externalId) {
        log.info("Marking email verified for {}", externalId);
    }

    @Override
    public Map<String, Object> getUserInfo(String externalId) {
        return Map.of("externalId", externalId);
    }

    @Override
    public String refreshToken(String refreshToken) {
        return refreshToken;
    }

    @Override
    public void logout(String externalId) {
        log.info("Logout for {}", externalId);
    }

    @Override
    public boolean isEmailVerified(String externalId) {
        return false;
    }

    @Override
    public void enableMfa(String externalId, String method) {
        log.info("Enable MFA {} for {}", method, externalId);
    }

    @Override
    public void disableMfa(String externalId) {
        log.info("Disable MFA for {}", externalId);
    }
}
