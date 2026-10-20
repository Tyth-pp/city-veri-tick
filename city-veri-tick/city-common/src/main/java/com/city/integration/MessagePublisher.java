package com.city.integration;

/**
 * Reserved abstraction for RabbitMQ-backed domain event publishing.
 */
public interface MessagePublisher {
    void publish(String topic, Object payload);
}
