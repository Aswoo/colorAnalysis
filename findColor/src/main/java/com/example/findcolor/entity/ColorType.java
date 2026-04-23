package com.example.findcolor.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분석 대상 색상의 종류와 각 색상별 HSV 범위를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ColorType {
    // 자연계의 색상은 생각보다 넓은 범위를 가집니다.
    RED(0, 15, 160, 180),
    PINK(140, 179),
    YELLOW(15, 40),
    GREEN(35, 95),   // 85 -> 95로 확장 (좀 더 청록빛까지 포함)
    BLUE(90, 145),   // 135 -> 145로 확장
    PURPLE(120, 155);

    private final int minH;
    private final int maxH;
    private final Integer altMinH;
    private final Integer altMaxH;

    ColorType(int minH, int maxH) {
        this(minH, maxH, null, null);
    }

    ColorType(int minH, int maxH, int altMinH, int altMaxH) {
        this.minH = minH;
        this.maxH = maxH;
        this.altMinH = altMinH;
        this.altMaxH = altMaxH;
    }

    /**
     * 입력된 HSV 값이 이 색상 범위에 해당하는지 판단합니다.
     */
    public boolean isMatch(float h, float s, float v) {
        // [필터링 완화]
        // 채도(S)나 명도(V)가 너무 낮으면 무채색(회색, 검정)이 되지만, 
        // 나무 숲은 그늘진 곳이 많으므로 기준을 30까지 낮춥니다.
        if (s < 50 || v < 50) return false;

        // 주 범위 체크
        if (h >= minH && h <= maxH) return true;

        // 보조 범위 체크 (RED 등)
        if (altMinH != null && altMaxH != null) {
            return h >= altMinH && h <= altMaxH;
        }

        return false;
    }

    /**
     * 단순히 맞다/틀리다를 넘어, 얼마나 타겟 색상에 가까운지 점수를 계산합니다.
     */
    public double calculateSimilarity(float h, float s, float v) {
        if (!isMatch(h, s, v)) return 0.0;

        // 매칭된 범위의 중심과 반경을 결정한다.
        // 보조 범위(altMinH~altMaxH)에 매칭된 경우 해당 범위 기준으로 계산해야
        // RED H=170이 주 범위 중심(7.5)과 비교되는 버그를 방지할 수 있다.
        final float center, range;
        if (altMinH != null && h >= altMinH && h <= altMaxH) {
            center = (altMinH + altMaxH) / 2.0f;
            range  = (altMaxH - altMinH) / 2.0f;
        } else {
            center = (minH + maxH) / 2.0f;
            range  = (maxH - minH) / 2.0f;
        }

        float diff = Math.abs(h - center);

        // 중심에서 멀어질수록 70~100점 사이로 분포
        double score = 100.0 - (diff / range * 30.0);

        // 채도(S)가 높을수록 선명한 색이므로 추가 가중치
        score = score * (s / 255.0 * 0.2 + 0.8);

        return Math.min(100.0, score);
    }

    public static ColorType fromString(String sentiment) {
        try {
            return ColorType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
