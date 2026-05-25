package dev.taskflow.application.usecase.auth;

import dev.taskflow.domain.port.repository.RefreshTokenRepository;
import dev.taskflow.domain.port.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository,
                         UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
            refreshTokenRepository.revokeAllForUser(user.getId())
        );
    }
}
