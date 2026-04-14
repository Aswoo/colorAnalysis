package com.example.findcolor.dto;

import com.example.findcolor.entity.ColorType;

/**
 * 색상 분석 결과를 담는 불변 데이터 객체 (Record)
 */
public record ColorAnalysisResult(
    Long requestId,
    ColorType targetColor,
    boolean isMatched,
    double similarityScore,
    String message
) {}
