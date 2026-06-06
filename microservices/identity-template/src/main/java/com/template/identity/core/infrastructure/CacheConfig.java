package com.template.identity.core.infrastructure;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activates Spring's cache abstraction. The backend is selected by the
 * platform binding contract: a bound redis component injects
 * SPRING_CACHE_TYPE=redis (see application-production.yml); unbound
 * services run with type=none (no-op) so @Cacheable methods behave
 * identically without a cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
