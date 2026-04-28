package com.example.findcolor.dto;

public record MissionResponse(
    Long requestId,
    String status,
    String message
) {}
