package com.template.identity.application;

import com.template.identity.core.application.BaseProfileQueryUseCase;
import com.template.identity.core.domain.BaseProfile;
import com.template.identity.core.infrastructure.IdpProvider;
import com.template.identity.domain.profiles.Profile;
import com.template.identity.infrastructure.ProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Default profile-query use case. Reuses cache-aside reads from
 * {@link BaseProfileQueryUseCase} (reads populate the "profiles" cache,
 * writes evict) and adds Pattern B profile switching: setting the active
 * profile updates this service's DB-backed app_metadata view in Auth0 via the
 * {@link IdpProvider}, so the user's NEXT token carries different claims.
 */
@Component
@Slf4j
public class ProfileQueryUseCase extends BaseProfileQueryUseCase {

    private final ProfileRepository profileRepository;
    private final IdpProvider idpProvider;

    public ProfileQueryUseCase(ProfileRepository profileRepository, IdpProvider idpProvider) {
        this.profileRepository = profileRepository;
        this.idpProvider = idpProvider;
    }

    @Override
    protected List<? extends BaseProfile> loadProfiles(String userId) {
        return profileRepository.findByUserExternalId(userId);
    }

    /**
     * Pattern B profile switching. Persists the active profile and syncs it to
     * the IDP app_metadata, then evicts the cache so the next read is fresh.
     */
    @Transactional
    public Profile setActiveProfile(String externalUserId, Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        if (profile.getUser() == null
                || !externalUserId.equals(profile.getUser().getExternalId())) {
            throw new IllegalArgumentException("Profile " + profileId + " does not belong to user");
        }
        idpProvider.updateUser(externalUserId, Map.of(
                "active_profile", profile.getProfileIdentifier(),
                "active_profile_id", profile.getId()));
        evict(externalUserId);
        log.info("Active profile for {} switched to {}", externalUserId, profile.getProfileIdentifier());
        return profile;
    }
}
