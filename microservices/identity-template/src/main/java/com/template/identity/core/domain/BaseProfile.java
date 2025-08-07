package com.template.identity.core.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "profile_type", discriminatorType = DiscriminatorType.STRING)
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BaseUser user;
    
    @Column(name = "profile_type", insertable = false, updatable = false)
    private String profileType;
    
    @ElementCollection
    @CollectionTable(name = "profile_attributes", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> additionalAttributes = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "profile_permissions", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();
    
    @Column(name = "is_verified")
    private boolean isVerified;
    
    @Column(name = "verification_date")
    private LocalDateTime verificationDate;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public abstract String getProfileIdentifier();
    
    public abstract boolean requiresVerification();
    
    public void addAttribute(String key, String value) {
        this.additionalAttributes.put(key, value);
    }
    
    public String getAttribute(String key) {
        return this.additionalAttributes.get(key);
    }
    
    public void addPermission(String permission) {
        this.permissions.add(permission);
    }
    
    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }
}