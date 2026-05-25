package dev.taskflow.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditLog {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID actorId;
    private Map<String, Object> payload;
    private Instant createdAt;

    private AuditLog() {}

    public static AuditLog create(String entityType, UUID entityId, String action,
                                  UUID actorId, Map<String, Object> payload) {
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID();
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.actorId = actorId;
        log.payload = payload;
        log.createdAt = Instant.now();
        return log;
    }

    public static AuditLog reconstitute(UUID id, String entityType, UUID entityId, String action,
                                        UUID actorId, Map<String, Object> payload, Instant createdAt) {
        AuditLog log = new AuditLog();
        log.id = id;
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.actorId = actorId;
        log.payload = payload;
        log.createdAt = createdAt;
        return log;
    }

    public UUID getId() { return id; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getAction() { return action; }
    public UUID getActorId() { return actorId; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
