package com.template.identity.core.application;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.template.identity.core.domain.BaseProfile;

/**
 * GET-side profile reads are cache-aside BY DEFAULT (platform convention,
 * mirroring the python template's ListItems/GetItem): reads populate the
 * "profiles" cache, writes evict. Extend and implement loadProfiles with
 * your repository; call evict(userId) from mutating use cases.
 */
public abstract class BaseProfileQueryUseCase {

    @Cacheable(cacheNames = "profiles", key = "#userId")
    public List<? extends BaseProfile> profilesFor(String userId) {
        return loadProfiles(userId);
    }

    @CacheEvict(cacheNames = "profiles", key = "#userId")
    public void evict(String userId) {
        // eviction by annotation
    }

    protected abstract List<? extends BaseProfile> loadProfiles(String userId);
}
