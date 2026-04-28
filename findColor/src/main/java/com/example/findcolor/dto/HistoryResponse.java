package com.example.findcolor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @JsonProperty("isFavorite")
    private boolean isFavorite;
    private List<PaletteColor> palette;

    public record PaletteColor(String hex, Double ratio) {}
}
