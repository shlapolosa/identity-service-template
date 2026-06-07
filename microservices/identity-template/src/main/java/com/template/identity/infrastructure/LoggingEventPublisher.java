package com.template.identity.infrastructure;

import com.template.identity.core.infrastructure.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default {@link EventPublisher}. Bridges domain events onto Spring's
 * application event bus and logs them. A bound Kafka component (Spring
 * Modulith externalization / spring-kafka) can replace this without touching
 * the use cases, which only depend on the {@link EventPublisher} seam.
 */
@Component
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public LoggingEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publish(String topic, Object event) {
        log.info("Publishing event to topic '{}': {}", topic, event);
        springPublisher.publishEvent(event);
    }

    @Override
    public void publishAsync(String topic, Object event) {
        publish(topic, event);
    }

    @Override
    public void publishWithKey(String topic, String key, Object event) {
        log.info("Publishing event to topic '{}' with key '{}'", topic, key);
        springPublisher.publishEvent(event);
    }
}
