package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.User;
import dev.taskflow.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.reconstitute(
            entity.getId(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getFullName(),
            entity.getAvatarUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt()
        );
    }

    public UserEntity toEntity(User domain) {
        return new UserEntity(
            domain.getId(),
            domain.getEmail(),
            domain.getPasswordHash(),
            domain.getFullName(),
            domain.getAvatarUrl(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()
        );
    }
}
