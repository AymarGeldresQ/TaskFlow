package dev.taskflow.application.usecase.auth;

import dev.taskflow.application.dto.auth.AuthResponse;
import dev.taskflow.application.dto.auth.LoginRequest;
import dev.taskflow.domain.model.RefreshToken;
import dev.taskflow.domain.model.User;
import dev.taskflow.domain.port.repository.RefreshTokenRepository;
import dev.taskflow.domain.port.repository.UserRepository;
import dev.taskflow.infrastructure.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public LoginUseCase(UserRepository userRepository,
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
    public AuthResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .filter(User::isActive)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Same exception as "not found" to avoid user enumeration
            throw new BadCredentialsException("Invalid credentials");
        }

        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();
        RefreshToken refreshToken = RefreshToken.create(
            user.getId(),
            jwtService.hashToken(rawRefreshToken),
            jwtService.refreshTokenExpiresAt()
        );
        refreshTokenRepository.save(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.of(
            accessToken,
            rawRefreshToken,
            jwtService.accessExpiresIn(),
            user.getId(),
            user.getEmail(),
            user.getFullName()
        );
    }
}
