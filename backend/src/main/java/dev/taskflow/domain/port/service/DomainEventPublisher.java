package dev.taskflow.domain.port.service;

import dev.taskflow.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {

    void publish(DomainEvent event);

    default void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
