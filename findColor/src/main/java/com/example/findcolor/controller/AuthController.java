package com.example.findcolor.controller;

import com.example.findcolor.dto.AuthDto;
import com.example.findcolor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Frontend 연동을 위해 임시 허용
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthDto.UserResponse> signup(@RequestBody AuthDto.SignupRequest request) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.UserResponse> login(@RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
