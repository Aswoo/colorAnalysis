package com.example.findcolor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 파일 이름 중복 방지를 위해 랜덤 ID 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 2. S3에 업로드 요청 설정
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        // 3. 실제 파일 데이터 전송
        s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 4. 업로드된 파일의 공개 URL 생성 후 반환
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
    }
}
