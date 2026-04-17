package com.example.findcolor.dto;

/**
 * 미션 요청 접수 시 즉각적인 응답을 위한 객체
 */
public record MissionResponse(
    Long requestId,
    String status,
    String imageUrl,
    String message
) {}
