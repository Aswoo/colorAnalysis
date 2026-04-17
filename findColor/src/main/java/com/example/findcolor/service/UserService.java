package com.example.findcolor.service;

import com.example.findcolor.dto.AuthDto;
import com.example.findcolor.entity.User;
import com.example.findcolor.repository.UserRepository;
import com.example.findcolor.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDto.UserResponse signup(AuthDto.SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        return new AuthDto.UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    public AuthDto.UserResponse login(AuthDto.LoginRequest request) {
        // Spring Security의 표준 인증 방식 사용
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 인증 성공 시 UserDetailsImpl에서 유저 정보 추출
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        return new AuthDto.UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
