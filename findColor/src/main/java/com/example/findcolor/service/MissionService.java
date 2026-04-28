package com.example.findcolor.service;

import com.example.findcolor.dto.AnalysisStatusResponse;
import com.example.findcolor.dto.MissionResponse;
import com.example.findcolor.entity.*;
import com.example.findcolor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.findcolor.dto.HistoryResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final S3Service s3Service;
    private final ColorAnalysisService colorAnalysisService;
    private final UserRepository userRepository;
    private final MissionImageRepository missionImageRepository;
    private final AnalysisRequestRepository analysisRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final DetectedColorRepository detectedColorRepository;

    @Transactional(readOnly = true)
    public Page<HistoryResponse> getHistory(Long userId, Pageable pageable) {
        return analysisRequestRepository.findByUserId(userId, pageable)
                .map(request -> {
                    List<HistoryResponse.PaletteColor> palette = detectedColorRepository
                            .findByAnalysisRequestId(request.getId())
                            .stream()
                            .map(c -> new HistoryResponse.PaletteColor(c.getColorHex(), c.getRatio()))
                            .collect(Collectors.toList());

                    return HistoryResponse.builder()
                            .id(request.getId())
                            .imageUrl(request.getImage().getImageUrl())
                            .status(request.getStatus())
                            .similarityScore(request.getAnalysisResult() != null ? request.getAnalysisResult().getSimilarityScore() : null)
                            .matched(request.getAnalysisResult() != null ? request.getAnalysisResult().getMatched() : null)
                            .createdAt(request.getCreatedAt())
                            .isFavorite(request.isFavorite())
                            .palette(palette)
                            .build();
                });
    }

    @Transactional
    public MissionResponse performMission(Long userId, String targetSentiment, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자 없음"));

        // imageUrl은 분석 완료 후 @Async 스레드에서 채워진다
        MissionImage missionImage = MissionImage.builder().user(user).build();
        missionImageRepository.save(missionImage);

        AnalysisRequest request = AnalysisRequest.builder().user(user).image(missionImage).status("PROCESSING").isFavorite(false).build();
        analysisRequestRepository.save(request);

        // HTTP 요청 스코프가 살아있는 지금 바이트를 미리 복사한다
        // 요청이 끝나면 MultipartFile 임시 파일이 삭제되어 @Async 스레드에서 접근 불가
        final byte[] fileBytes = file.getBytes();
        final Long requestId = request.getId();
        final Long imageId = missionImage.getId();

        // 커밋 완료 후 @Async 실행: findById 타이밍 문제 방지
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                colorAnalysisService.processAnalysisAsync(requestId, imageId, fileBytes, targetSentiment);
            }
        });

        return new MissionResponse(request.getId(), request.getStatus(), "접수됨");
    }

    @Transactional(readOnly = true)
    public AnalysisStatusResponse getAnalysisStatus(Long requestId, String targetSentiment) {
        AnalysisRequest request = analysisRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청 없음"));

        if ("FAILED".equals(request.getStatus())) return AnalysisStatusResponse.failed(requestId, "분석 실패");

        if ("COMPLETED".equals(request.getStatus())) {
            // [정석대로 복구] Sleep 제거
            List<DetectedColor> detectedColors = detectedColorRepository.findByAnalysisRequestId(requestId);
            
            AnalysisResult result = analysisResultRepository.findByAnalysisRequestId(requestId)
                    .orElseThrow(() -> new RuntimeException("결과 없음"));
            
            List<AnalysisStatusResponse.DetectedColorResponse> palette = detectedColors.stream()
                    .map(c -> new AnalysisStatusResponse.DetectedColorResponse(c.getColorHex(), c.getRatio()))
                    .collect(Collectors.toList());
            
            return AnalysisStatusResponse.completed(
                requestId, 
                ColorType.fromString(targetSentiment), 
                result.getMatched(), 
                result.getSimilarityScore(),
                palette
            );
        }

        return AnalysisStatusResponse.processing(requestId, ColorType.fromString(targetSentiment));
    }
}
