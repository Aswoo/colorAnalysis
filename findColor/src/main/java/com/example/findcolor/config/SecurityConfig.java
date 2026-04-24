package com.example.findcolor.config;

import com.example.findcolor.dto.AuthDto;
import com.example.findcolor.dto.ErrorResponse;
import com.example.findcolor.entity.User;
import com.example.findcolor.security.CustomLoginFilter;
import com.example.findcolor.security.JwtAuthorizationFilter;
import com.example.findcolor.security.JwtUtil;
import com.example.findcolor.security.UserDetailsImpl;
import com.example.findcolor.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService; // 서비스 주입

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization")); // 헤더 노출
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        CustomLoginFilter loginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration));
        
        loginFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
            User user = ((UserDetailsImpl) authentication.getPrincipal()).getUser();
            String token = jwtUtil.createToken(email);
            
            response.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);
            
            AuthDto.UserResponse userResponse = new AuthDto.UserResponse(user.getId(), user.getEmail(), user.getNickname());
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(userResponse));
        });

        loginFilter.setAuthenticationFailureHandler((request, response, exception) -> {
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "아이디 또는 비밀번호가 잘못되었습니다.");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        });

        // [핵심 필터 등록]
        // 1. 인가 필터를 로그인 필터 앞에 둠
        http.addFilterBefore(jwtAuthorizationFilter(), CustomLoginFilter.class);
        // 2. 로그인 필터를 시큐리티 기본 폼 로그인 위치에 둠
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
