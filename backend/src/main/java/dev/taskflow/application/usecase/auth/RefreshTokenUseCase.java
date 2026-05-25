package dev.taskflow.application.usecase.auth;

import dev.taskflow.application.dto.auth.AuthResponse;
import dev.taskflow.application.dto.auth.RefreshRequest;
import dev.taskflow.domain.exception.InvalidRefreshTokenException;
import dev.taskflow.domain.model.RefreshToken;
import dev.taskflow.domain.model.User;
import dev.taskflow.domain.port.repository.RefreshTokenRepository;
import dev.taskflow.domain.port.repository.UserRepository;
import dev.taskflow.infrastructure.security.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtService jwtService,
                               UserDetailsService userDetailsService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse execute(RefreshRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken());

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (!existing.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        // Rotate: revoke old token, issue new one
        existing.revoke();
        refreshTokenRepository.save(existing);

        User user = userRepository.findById(existing.getUserId())
            .filter(User::isActive)
            .orElseThrow(InvalidRefreshTokenException::new);

        String newRawRefreshToken = jwtService.generateOpaqueRefreshToken();
        RefreshToken newRefreshToken = RefreshToken.create(
            user.getId(),
            jwtService.hashToken(newRawRefreshToken),
            jwtService.refreshTokenExpiresAt()
        );
        refreshTokenRepository.save(newRefreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.of(
            accessToken,
            newRawRefreshToken,
            jwtService.accessExpiresIn(),
            user.getId(),
            user.getEmail(),
            user.getFullName()
        );
    }
}
