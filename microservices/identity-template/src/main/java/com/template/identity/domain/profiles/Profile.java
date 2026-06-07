package com.template.identity.domain.profiles;

import com.template.identity.core.domain.BaseProfile;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Default "user" profile materialized from templates/profiles/Profile.java.mustache
 * (the codegen path stays untouched). This is the concrete out-of-the-box
 * profile so the template compiles and runs as-is. Domain scaffolding replaces
 * this with domain-specific profiles (Patient, Provider, ...). Logic lives in
 * {@link BaseProfile}.
 */
@Entity
@Table(name = "user_profiles")
@DiscriminatorValue("USER")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class Profile extends BaseProfile {

    @Column(name = "display_name")
    private String displayName;

    @Override
    public String getProfileIdentifier() {
        return "USER-" + getId();
    }

    @Override
    public boolean requiresVerification() {
        return false;
    }
}
