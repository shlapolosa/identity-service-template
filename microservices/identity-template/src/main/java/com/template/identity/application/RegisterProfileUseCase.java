package com.template.identity.application;

import com.template.identity.core.application.BaseRegistrationUseCase;
import com.template.identity.core.domain.BaseUser;
import com.template.identity.core.infrastructure.EventPublisher;
import com.template.identity.core.infrastructure.IdpProvider;
import com.template.identity.domain.User;
import com.template.identity.domain.profiles.Profile;
import com.template.identity.infrastructure.ProfileRepository;
import com.template.identity.infrastructure.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Default registration use case materialized from
 * templates/usecases/RegisterProfileUseCase.java.mustache (codegen path
 * untouched). Reuses the orchestration in {@link BaseRegistrationUseCase}
 * (validate → create external identity → create user → create profile →
 * post-registration → publish event) and only supplies the domain seams.
 */
@Component
@Slf4j
public class RegisterProfileUseCase extends BaseRegistrationUseCase<Profile> {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public RegisterProfileUseCase(IdpProvider idpProvider,
                                  EventPublisher eventPublisher,
                                  UserRepository userRepository,
                                  ProfileRepository profileRepository) {
        super(idpProvider, eventPublisher);
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    protected void validateDomainSpecific(RegistrationCommand command) {
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException("User already exists: " + command.getEmail());
        }
    }

    @Override
    protected BaseUser createUser(RegistrationCommand command, String externalId) {
        User user = User.builder()
                .username(command.getEmail())
                .email(command.getEmail())
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .phoneNumber(command.getPhoneNumber())
                .externalId(externalId)
                .status(BaseUser.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        return userRepository.save(user);
    }

    @Override
    protected Profile createProfile(RegistrationCommand command, BaseUser user) {
        String displayName = ((command.getFirstName() == null ? "" : command.getFirstName())
                + " " + (command.getLastName() == null ? "" : command.getLastName())).trim();
        Profile profile = Profile.builder()
                .user(user)
                .displayName(displayName.isEmpty() ? command.getEmail() : displayName)
                .isVerified(false)
                .build();
        profile.addPermission("profile:read");
        return profileRepository.save(profile);
    }

    @Override
    protected void postRegistration(Profile profile) {
        log.info("Default profile registered: id={} user={}",
                profile.getId(), profile.getUser().getEmail());
    }

    @Override
    protected Map<String, Object> buildMetadata(RegistrationCommand command) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("profileType", "USER");
        if (command.getAdditionalData() != null) {
            metadata.putAll(command.getAdditionalData());
        }
        return metadata;
    }

    @Override
    protected void cleanupFailedRegistration(RegistrationCommand command, Exception e) {
        log.error("Cleaning up failed registration for {}", command.getEmail(), e);
    }
}
