package com.example.findcolor.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분석 대상 색상의 종류와 각 색상별 HSV 범위를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ColorType {
    RED(0, 10, 160, 180),   // 빨간색은 Hue 0-10 및 160-180 두 범위를 가짐
    PINK(145, 165),
    YELLOW(20, 35),
    GREEN(35, 85),
    BLUE(90, 135),
    PURPLE(135, 155);

    private final int minH;
    private final int maxH;
    private final Integer altMinH; // RED처럼 범위가 두 개인 경우를 위함
    private final Integer altMaxH;

    // 단일 범위를 가지는 색상용 생성자
    ColorType(int minH, int maxH) {
        this(minH, maxH, null, null);
    }

    // 두 범위를 가지는 색상용 생성자 (예: RED)
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
        // 무채색 필터링 (채도/명도 50 미만 제외)
        if (s < 50 || v < 50) return false;

        // 주 범위 체크
        if (h >= minH && h <= maxH) return true;

        // 보조 범위 체크 (RED 전용)
        if (altMinH != null && altMaxH != null) {
            return h >= altMinH && h <= altMaxH;
        }

        return false;
    }

    /**
     * 문자열로부터 ColorType을 안전하게 가져옵니다.
     */
    public static ColorType fromString(String sentiment) {
        try {
            return ColorType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // 지원하지 않는 색상인 경우
        }
    }
}
