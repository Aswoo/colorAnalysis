package com.example.findcolor.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lab 색공간의 Hue 각도(atan2(b, a))로 색상을 판별한다.
 * L(명도)을 제외하므로 어두운 그림자나 밝은 하이라이트도 같은 색으로 인식한다.
 *
 * refHue:     기준 Hue 각도 (degrees, 0–360)
 * maxHueDist: 이 각도 이상 벗어나면 해당 색으로 판정하지 않음
 */
@Getter
@RequiredArgsConstructor
public enum ColorType {
    RED(40.0, 30.0),
    PINK(5.0, 25.0),
    YELLOW(85.0, 30.0),
    GREEN(130.0, 40.0),
    BLUE(285.0, 35.0),
    PURPLE(335.0, 40.0);

    private final double refHue;
    private final double maxHueDist;

    private static final double MIN_CHROMA = 15.0;

    private static double labHue(double a, double b) {
        double angle = Math.toDegrees(Math.atan2(b, a));
        return angle < 0 ? angle + 360.0 : angle;
    }

    public double hueDistance(double a, double b) {
        double hue = labHue(a, b);
        double diff = Math.abs(hue - refHue);
        return Math.min(diff, 360.0 - diff);
    }

    public boolean isMatch(double a, double b) {
        if (Math.sqrt(a * a + b * b) < MIN_CHROMA) return false;
        return hueDistance(a, b) < maxHueDist;
    }

    /**
     * Hue 중심에서 가까울수록 100점, maxHueDist 경계에서 60점, 범위 밖 0점
     */
    public double calculateSimilarity(double a, double b) {
        if (!isMatch(a, b)) return 0.0;
        double dist = hueDistance(a, b);
        return 100.0 - (dist / maxHueDist) * 40.0;
    }

    public static ColorType fromString(String sentiment) {
        try {
            return ColorType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
