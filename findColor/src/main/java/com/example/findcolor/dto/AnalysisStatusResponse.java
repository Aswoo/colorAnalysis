package com.example.findcolor.dto;

import com.example.findcolor.entity.ColorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AnalysisStatusResponse {
    
    @JsonProperty("requestId")
    private Long requestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("targetColor")
    private ColorType targetColor;

    @JsonProperty("matched")
    private Boolean matched;

    @JsonProperty("similarityScore")
    private Double similarityScore;

    // [이름 변경] 강제 갱신을 위해 colorPalettes로 변경
    @JsonProperty("colorPalettes")
    private List<DetectedColorResponse> colorPalettes;

    @JsonProperty("message")
    private String message;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedColorResponse {
        @JsonProperty("hex")
        private String hex;
        
        @JsonProperty("ratio")
        private Double ratio;
    }

    public static AnalysisStatusResponse processing(Long requestId, ColorType targetColor) {
        return AnalysisStatusResponse.builder()
                .requestId(requestId)
                .status("PROCESSING")
                .targetColor(targetColor)
                .message("분석 중입니다.")
                .build();
    }

    public static AnalysisStatusResponse completed(Long requestId, ColorType targetColor, boolean matched,
            double score, List<DetectedColorResponse> colorPalettes) {
        return AnalysisStatusResponse.builder()
                .requestId(requestId)
                .status("COMPLETED")
                .targetColor(targetColor)
                .matched(matched)
                .similarityScore(score)
                .colorPalettes(colorPalettes)
                .message("분석 완료 (V2-COLOR-PALETTE)") // 버전 식별 메시지
                .build();
    }

    public static AnalysisStatusResponse failed(Long requestId, String message) {
        return AnalysisStatusResponse.builder()
                .requestId(requestId)
                .status("FAILED")
                .message(message)
                .build();
    }
}
