package com.example.findcolor.controller;

import com.example.findcolor.dto.AnalysisStatusResponse;
import com.example.findcolor.dto.MissionRequest;
import com.example.findcolor.dto.MissionResponse;
import com.example.findcolor.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Frontend 도메인 허용
public class MissionImageController {

    private final MissionService missionService;

    /**
     * [미션 수행] 이미지 업로드 및 비동기 분석 요청을 접수합니다.
     * 
     * @param request 유저 ID, 타겟 색상 감성 정보 (JSON 파트)
     * @param file 분석할 이미지 파일 (Binary 파트)
     * @return requestId를 포함한 접수 확인 DTO
     */
    @PostMapping(value = "/perform", consumes = {"multipart/form-data"})
    public ResponseEntity<MissionResponse> performMission(
            @RequestPart("request") MissionRequest request,
            @RequestPart("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new RuntimeException("파일이 없습니다.");
        }

        // 서비스 호출 (S3 업로드, 분석 요청 생성, 비동기 호출 통합 처리)
        MissionResponse response = missionService.performMission(
                request.userId(), 
                request.targetSentiment(), 
                file
        );

        return ResponseEntity.ok(response);
    }

    /**
     * [상태 조회] 특정 분석 요청의 진행 상태 및 결과를 확인합니다.
     * 
     * @param requestId 분석 요청 시 발급받은 ID
     * @param targetSentiment 확인하려는 색상 감성 (DTO 생성에 필요)
     * @return 현재 상태(PROCESSING, COMPLETED, FAILED) 및 분석 결과
     */
    @GetMapping("/analysis/{requestId}")
    public ResponseEntity<AnalysisStatusResponse> getAnalysisStatus(
            @PathVariable Long requestId,
            @RequestParam String targetSentiment) {

        AnalysisStatusResponse response = missionService.getAnalysisStatus(requestId, targetSentiment);
        return ResponseEntity.ok(response);
    }
}
