package com.example.findcolor.dto;

import com.example.findcolor.entity.ColorType;

/**
 * 분석 진행 상태 및 최종 결과를 조회할 때 사용하는 객체
 */
public record AnalysisStatusResponse(
        Long requestId,
        String status,
        ColorType targetColor,
        Boolean matched,
        Double similarityScore,
        String message) {
    // 분석 중일 때를 위한 편의 정적 메서드
    public static AnalysisStatusResponse processing(Long requestId, ColorType targetColor) {
        return new AnalysisStatusResponse(requestId, "PROCESSING", targetColor, null, null, "분석 중입니다.");
    }

    // 분석 완료되었을 때를 위한 편의 정적 메서드
    public static AnalysisStatusResponse completed(Long requestId, ColorType targetColor, boolean matched,
            double score) {
        return new AnalysisStatusResponse(requestId, "COMPLETED", targetColor, matched, score, "분석이 완료되었습니다.");
    }

    // 실패했을 때
    public static AnalysisStatusResponse failed(Long requestId, String message) {
        return new AnalysisStatusResponse(requestId, "FAILED", null, null, null, message);
    }
}
