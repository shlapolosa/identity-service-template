package com.template.identity.domain;

import com.template.identity.core.domain.BaseUser;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Default concrete user for the platform's out-of-the-box "user" profile
 * domain. Domains scaffolded from this template replace/extend this with
 * their own user subtypes; the base logic lives in {@link BaseUser}.
 */
@Entity
@DiscriminatorValue("DEFAULT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class User extends BaseUser {

    @Override
    public String getUserType() {
        return "DEFAULT";
    }
}
