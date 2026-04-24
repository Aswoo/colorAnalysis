package com.example.findcolor.controller;

import com.example.findcolor.dto.AuthDto;
import com.example.findcolor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthDto.UserResponse> signup(@RequestBody AuthDto.SignupRequest request) {
        return ResponseEntity.ok(userService.signup(request));
    }
    
    // login 메서드는 이제 CustomLoginFilter에서 처리하므로 삭제합니다.
}
