package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.AuditLog;
import dev.taskflow.domain.port.repository.AuditLogRepository;
import dev.taskflow.infrastructure.persistence.mapper.AuditLogMapper;
import dev.taskflow.infrastructure.persistence.repository.AuditLogJpaRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogMapper mapper;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpaRepository, AuditLogMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(auditLog)));
    }

    @Override
    public Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
            .map(mapper::toDomain);
    }
}
