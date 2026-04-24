package com.example.findcolor.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryResponse {
    private Long id;
    private String imageUrl;
    private String status;
    private Double similarityScore;
    private Boolean matched;
    private LocalDateTime createdAt;
    private boolean isFavorite;
}
