package com.example.findcolor.dto;

/**
 * 미션 수행 요청 시 전달받는 데이터 객체
 */
public record MissionRequest(
    Long userId,
    String targetSentiment
) {}
