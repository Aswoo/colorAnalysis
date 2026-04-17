package com.example.findcolor.service;

import com.example.findcolor.dto.AnalysisStatusResponse;
import com.example.findcolor.dto.MissionResponse;
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
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 비동기 미션 수행: 이미지 업로드 -> 요청 생성(PROCESSING) -> 비동기 분석 호출
     */
    @Transactional
    public MissionResponse performMission(Long userId, String targetSentiment, MultipartFile file) throws IOException {
        
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
        
        // DTO 반환 (엔티티 노출 방지)
        return new MissionResponse(
            request.getId(), 
            request.getStatus(), 
            missionImage.getImageUrl(), 
            "분석 요청이 성공적으로 접수되었습니다."
        );
    }

    /**
     * 특정 분석 요청의 진행 상태 및 결과를 조회합니다.
     */
    @Transactional(readOnly = true)
    public AnalysisStatusResponse getAnalysisStatus(Long requestId, String targetSentiment) {
        AnalysisRequest request = analysisRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다. ID: " + requestId));

        // 실패 상태인 경우
        if ("FAILED".equals(request.getStatus())) {
            return AnalysisStatusResponse.failed(requestId, "분석 중 오류가 발생했습니다.");
        }

        // 분석 완료된 경우 결과와 함께 반환
        if ("COMPLETED".equals(request.getStatus())) {
            AnalysisResult result = analysisResultRepository.findByAnalysisRequestId(requestId)
                    .orElseThrow(() -> new RuntimeException("분석 결과를 찾을 수 없습니다."));
            
            return AnalysisStatusResponse.completed(
                requestId, 
                ColorType.fromString(targetSentiment), 
                result.getMatched(), 
                result.getSimilarityScore()
            );
        }

        // 아직 진행 중인 경우 (PROCESSING)
        return AnalysisStatusResponse.processing(requestId, ColorType.fromString(targetSentiment));
    }
}
