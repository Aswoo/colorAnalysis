package com.example.findcolor.controller;

import com.example.findcolor.entity.Image;
import com.example.findcolor.entity.User;
import com.example.findcolor.repository.ImageRepository;
import com.example.findcolor.repository.UserRepository;
import com.example.findcolor.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
        if (file.isEmpty()) return "파일이 없습니다.";

        // 1. 먼저 유저가 존재하는지 확인합니다 (중요: 업로드 전 검증)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        try {
            // 2. 유저가 확인된 후에만 S3에 파일을 업로드합니다.
            String s3Url = s3Service.uploadFile(file);

            // 3. DB에 이미지 정보를 저장합니다.
            Image image = Image.builder()
                    .user(user)
                    .imageUrl(s3Url)
                    .build();
            imageRepository.save(image);

            return "S3 업로드 성공! URL: " + s3Url;
        } catch (IOException e) {
            return "업로드 실패: " + e.getMessage();
        }
    }
}
