package com.example.findcolor.service;

import com.example.findcolor.entity.*;
import com.example.findcolor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final S3Service s3Service;
    private final ColorAnalysisService colorAnalysisService;
    private final UserRepository userRepository;
    private final MissionImageRepository missionImageRepository;
    private final AnalysisRequestRepository analysisRequestRepository;

    /**
     * 비동기 미션 수행: 이미지 업로드 -> 요청 생성(PROCESSING) -> 비동기 분석 호출
     */
    @Transactional
    public AnalysisRequest performMission(Long userId, String targetSentiment, MultipartFile file) throws IOException {
        
        // 1. 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 2. S3 이미지 업로드
        String s3Url = s3Service.uploadFile(file);

        // 3. 미션 이미지 정보 DB 저장
        MissionImage missionImage = MissionImage.builder()
                .user(user)
                .imageUrl(s3Url)
                .build();
        missionImageRepository.save(missionImage);

        // 4. 분석 요청 기록 선행 생성 (상태: PROCESSING)
        AnalysisRequest request = AnalysisRequest.builder()
                .user(user)
                .image(missionImage)
                .status("PROCESSING")
                .isFavorite(false)
                .build();
        analysisRequestRepository.save(request);

        // 5. 비동기 분석 실행 (OpenCV + K-means)
        colorAnalysisService.processAnalysisAsync(request.getId(), file, targetSentiment);
        
        // 즉시 분석 요청 객체 리턴
        return request;
    }
}
