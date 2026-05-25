package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.AuditLog;
import dev.taskflow.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogEntity entity) {
        return AuditLog.reconstitute(
            entity.getId(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getAction(),
            entity.getActorId(),
            entity.getPayload(),
            entity.getCreatedAt()
        );
    }

    public AuditLogEntity toEntity(AuditLog domain) {
        return new AuditLogEntity(
            domain.getId(),
            domain.getEntityType(),
            domain.getEntityId(),
            domain.getAction(),
            domain.getActorId(),
            domain.getPayload(),
            domain.getCreatedAt()
        );
    }
}
