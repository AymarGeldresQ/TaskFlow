package dev.taskflow.application.usecase.auth;

import dev.taskflow.application.dto.auth.AuthResponse;
import dev.taskflow.application.dto.auth.RegisterRequest;
import dev.taskflow.domain.exception.EmailAlreadyTakenException;
import dev.taskflow.domain.model.RefreshToken;
import dev.taskflow.domain.model.User;
import dev.taskflow.domain.port.repository.RefreshTokenRepository;
import dev.taskflow.domain.port.repository.UserRepository;
import dev.taskflow.infrastructure.security.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public RegisterUserUseCase(UserRepository userRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService,
                               UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyTakenException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), passwordHash, request.fullName());
        User saved = userRepository.save(user);

        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();
        RefreshToken refreshToken = RefreshToken.create(
            saved.getId(),
            jwtService.hashToken(rawRefreshToken),
            jwtService.refreshTokenExpiresAt()
        );
        refreshTokenRepository.save(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.of(
            accessToken,
            rawRefreshToken,
            jwtService.accessExpiresIn(),
            saved.getId(),
            saved.getEmail(),
            saved.getFullName()
        );
    }
}
