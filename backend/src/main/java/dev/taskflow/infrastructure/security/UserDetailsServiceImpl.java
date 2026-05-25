package dev.taskflow.infrastructure.security;

import dev.taskflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    public UserDetailsServiceImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userJpaRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase().trim())
            .map(entity -> new JwtUserDetails(entity.getId(), entity.getEmail(), entity.getPasswordHash()))
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
