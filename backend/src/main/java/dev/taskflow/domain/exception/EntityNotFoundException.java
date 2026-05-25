package dev.taskflow.domain.exception;

import java.util.UUID;

public class EntityNotFoundException extends DomainException {

    private final String entityType;
    private final UUID entityId;

    public EntityNotFoundException(String entityType, UUID id) {
        super(String.format("%s with id %s not found", entityType, id));
        this.entityType = entityType;
        this.entityId = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }
}
