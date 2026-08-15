package com.payshield.service;

import com.payshield.dto.auth.AuthResponse;
import com.payshield.dto.auth.LoginRequest;
import com.payshield.dto.auth.RegisterRequest;
import com.payshield.entity.Role;
import com.payshield.entity.User;
import com.payshield.entity.enums.UserRole;
import com.payshield.entity.enums.UserStatus;
import com.payshield.repository.RoleRepository;
import com.payshield.repository.UserRepository;
import com.payshield.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            WalletService walletService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.walletService = walletService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        Role userRole =
                roleRepository.findByName(UserRole.USER.name())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "USER role not found"
                                )
                        );

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        userRepository.save(user);
        walletService.createWallet(user);

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(
                                "ROLE_" + UserRole.USER.name()
                        )                        .build();

        String token =
                jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiration() / 1000,
                user.getId(),
                user.getEmail(),
                Set.of(UserRole.USER.name())
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        String email = request.email()
                .trim()
                .toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(
                                user.getRoles()
                                        .stream()
                                        .map(role ->
                                                "ROLE_" +
                                                        role.getName()
                                        )
                                        .toArray(String[]::new)
                        )
                        .build();

        String token =
                jwtService.generateToken(userDetails);

        Set<String> roles =
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(java.util.stream.Collectors.toSet());

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiration() / 1000,
                user.getId(),
                user.getEmail(),
                roles
        );
    }
}