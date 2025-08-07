package com.template.identity.core.application;

import com.template.identity.core.domain.BaseProfile;
import com.template.identity.core.domain.BaseUser;
import com.template.identity.core.infrastructure.IdpProvider;
import com.template.identity.core.infrastructure.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseRegistrationUseCase<P extends BaseProfile> {
    
    protected final IdpProvider idpProvider;
    protected final EventPublisher eventPublisher;
    
    @Transactional
    public RegistrationResult execute(RegistrationCommand command) {
        log.info("Starting registration for user: {}", command.getEmail());
        
        try {
            // Step 1: Validate command
            validateCommand(command);
            
            // Step 2: Create external identity
            String externalId = createExternalIdentity(command);
            
            // Step 3: Create local user
            BaseUser user = createUser(command, externalId);
            
            // Step 4: Create profile
            P profile = createProfile(command, user);
            
            // Step 5: Domain-specific post-registration
            postRegistration(profile);
            
            // Step 6: Publish event
            publishRegistrationEvent(profile);
            
            log.info("Registration completed successfully for user: {}", user.getEmail());
            
            return RegistrationResult.builder()
                .userId(user.getId())
                .profileId(profile.getId())
                .externalId(externalId)
                .profileType(profile.getProfileType())
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("Registration failed: ", e);
            handleRegistrationFailure(command, e);
            throw new RegistrationException("Registration failed: " + e.getMessage(), e);
        }
    }
    
    protected void validateCommand(RegistrationCommand command) {
        if (command.getEmail() == null || command.getEmail().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (command.getPassword() == null || command.getPassword().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
        // Domain-specific validation
        validateDomainSpecific(command);
    }
    
    protected String createExternalIdentity(RegistrationCommand command) {
        Map<String, Object> metadata = buildMetadata(command);
        return idpProvider.createUser(
            command.getEmail(),
            command.getPassword(),
            metadata
        );
    }
    
    protected abstract void validateDomainSpecific(RegistrationCommand command);
    
    protected abstract BaseUser createUser(RegistrationCommand command, String externalId);
    
    protected abstract P createProfile(RegistrationCommand command, BaseUser user);
    
    protected abstract void postRegistration(P profile);
    
    protected abstract Map<String, Object> buildMetadata(RegistrationCommand command);
    
    protected void publishRegistrationEvent(P profile) {
        RegistrationEvent event = new RegistrationEvent(
            profile.getUser().getId(),
            profile.getId(),
            profile.getProfileType(),
            profile.getUser().getEmail()
        );
        eventPublisher.publish("registration-events", event);
    }
    
    protected void handleRegistrationFailure(RegistrationCommand command, Exception e) {
        // Cleanup external identity if created
        log.error("Handling registration failure for: {}", command.getEmail());
        // Domain-specific cleanup
        cleanupFailedRegistration(command, e);
    }
    
    protected abstract void cleanupFailedRegistration(RegistrationCommand command, Exception e);
    
    public static class RegistrationCommand {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Map<String, Object> additionalData;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
    }
    
    public static class RegistrationResult {
        private Long userId;
        private Long profileId;
        private String externalId;
        private String profileType;
        private boolean success;
        
        public static RegistrationResultBuilder builder() {
            return new RegistrationResultBuilder();
        }
        
        public static class RegistrationResultBuilder {
            private Long userId;
            private Long profileId;
            private String externalId;
            private String profileType;
            private boolean success;
            
            public RegistrationResultBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public RegistrationResultBuilder profileId(Long profileId) {
                this.profileId = profileId;
                return this;
            }
            
            public RegistrationResultBuilder externalId(String externalId) {
                this.externalId = externalId;
                return this;
            }
            
            public RegistrationResultBuilder profileType(String profileType) {
                this.profileType = profileType;
                return this;
            }
            
            public RegistrationResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public RegistrationResult build() {
                RegistrationResult result = new RegistrationResult();
                result.userId = this.userId;
                result.profileId = this.profileId;
                result.externalId = this.externalId;
                result.profileType = this.profileType;
                result.success = this.success;
                return result;
            }
        }
    }
    
    public static class RegistrationEvent {
        private final Long userId;
        private final Long profileId;
        private final String profileType;
        private final String email;
        
        public RegistrationEvent(Long userId, Long profileId, String profileType, String email) {
            this.userId = userId;
            this.profileId = profileId;
            this.profileType = profileType;
            this.email = email;
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public Long getProfileId() { return profileId; }
        public String getProfileType() { return profileType; }
        public String getEmail() { return email; }
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
    
    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}