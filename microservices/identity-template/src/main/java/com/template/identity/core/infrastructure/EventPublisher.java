package com.template.identity.core.infrastructure;

public interface EventPublisher {
    
    void publish(String topic, Object event);
    
    void publishAsync(String topic, Object event);
    
    void publishWithKey(String topic, String key, Object event);
}