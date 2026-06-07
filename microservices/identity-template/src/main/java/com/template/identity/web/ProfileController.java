package com.template.identity.web;

import com.template.identity.application.ProfileQueryUseCase;
import com.template.identity.application.RegisterProfileUseCase;
import com.template.identity.core.application.BaseRegistrationUseCase.RegistrationResult;
import com.template.identity.core.domain.BaseProfile;
import com.template.identity.domain.profiles.Profile;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pattern B profile endpoints (this service is the profile authority; it is
 * never called for login). Reuses {@link RegisterProfileUseCase} and
 * {@link ProfileQueryUseCase}.
 *
 *  - POST /api/v1/profiles/register   registration (no auth — Pattern B)
 *  - GET  /api/v1/profiles/me         current user's profiles (JWT required)
 *  - POST /api/v1/profiles/me/active/{profileId}  switch active profile (JWT required)
 */
@RestController
@RequestMapping("/api/v1/profiles")
@Slf4j
public class ProfileController {

    private final RegisterProfileUseCase registerProfileUseCase;
    private final ProfileQueryUseCase profileQueryUseCase;

    public ProfileController(RegisterProfileUseCase registerProfileUseCase,
                             ProfileQueryUseCase profileQueryUseCase) {
        this.registerProfileUseCase = registerProfileUseCase;
        this.profileQueryUseCase = profileQueryUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResult> register(@Valid @RequestBody RegistrationRequest request) {
        log.info("Registering new profile: {}", request.getEmail());
        RegistrationResult result = registerProfileUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/me")
    public ResponseEntity<List<? extends BaseProfile>> myProfiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(profileQueryUseCase.profilesFor(userId));
    }

    @PostMapping("/me/active/{profileId}")
    public ResponseEntity<Profile> switchActiveProfile(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable Long profileId) {
        String userId = jwt.getSubject();
        Profile profile = profileQueryUseCase.setActiveProfile(userId, profileId);
        return ResponseEntity.ok(profile);
    }
}
