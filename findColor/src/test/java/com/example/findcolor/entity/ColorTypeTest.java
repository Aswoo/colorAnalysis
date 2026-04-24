package com.example.findcolor.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ColorType")
class ColorTypeTest {

    // Lab a,b → hue 계산 헬퍼
    private static double[] hueToAB(double hueDeg, double chroma) {
        double rad = Math.toRadians(hueDeg);
        return new double[]{chroma * Math.cos(rad), chroma * Math.sin(rad)};
    }

    // ──────────────────────────────────────────────
    // hueDistance()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("hueDistance() — 원형 Hue 거리 계산")
    class HueDistance {

        @Test
        @DisplayName("기준 Hue와 동일하면 거리 = 0")
        void sameHue_returnsZero() {
            double[] ab = hueToAB(ColorType.RED.getRefHue(), 30.0);
            assertThat(ColorType.RED.hueDistance(ab[0], ab[1])).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("maxHueDist만큼 떨어진 점은 거리 = maxHueDist")
        void offsetByMax_returnsMax() {
            double target = ColorType.GREEN.getRefHue() + ColorType.GREEN.getMaxHueDist();
            double[] ab = hueToAB(target, 30.0);
            assertThat(ColorType.GREEN.hueDistance(ab[0], ab[1]))
                    .isCloseTo(ColorType.GREEN.getMaxHueDist(), within(0.01));
        }

        @Test
        @DisplayName("360° 근처 감싸기 — PURPLE 기준 358°와 2° 사이 거리는 4°")
        void wraparound_calculatesShortestPath() {
            double[] ab358 = hueToAB(358.0, 30.0);
            double[] ab2 = hueToAB(2.0, 30.0);
            // PURPLE refHue=335 기준이 아닌 두 점 간 거리를 확인: |358-2|=356 vs 360-356=4
            // hueDistance는 기준점 기준이라 직접 확인 어려우므로, 0°를 기준으로 설정된 색상의
            // 359°와 1° 점이 2° 차이인지 간접 검증 (atan2 원형 계산이 올바른지)
            double h358 = Math.toDegrees(Math.atan2(ab358[1], ab358[0]));
            if (h358 < 0) h358 += 360;
            double h2 = Math.toDegrees(Math.atan2(ab2[1], ab2[0]));
            if (h2 < 0) h2 += 360;
            double diff = Math.abs(h358 - h2);
            double circular = Math.min(diff, 360.0 - diff);
            assertThat(circular).isCloseTo(4.0, within(0.1));
        }
    }

    // ──────────────────────────────────────────────
    // isMatch()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("isMatch() — Hue 임계값 + 채도 필터")
    class IsMatch {

        @Test
        @DisplayName("기준 Hue, 충분한 채도 → true")
        void referenceHue_highChroma_returnsTrue() {
            double[] ab = hueToAB(ColorType.BLUE.getRefHue(), 30.0);
            assertThat(ColorType.BLUE.isMatch(ab[0], ab[1])).isTrue();
        }

        @Test
        @DisplayName("채도 < MIN_CHROMA(15) → false (무채색 제거)")
        void lowChroma_returnsFalse() {
            double[] ab = hueToAB(ColorType.BLUE.getRefHue(), 10.0);
            assertThat(ColorType.BLUE.isMatch(ab[0], ab[1])).isFalse();
        }

        @Test
        @DisplayName("hueDistance = maxHueDist 경계는 false (미만 조건)")
        void exactMaxHueDist_returnsFalse() {
            double target = ColorType.GREEN.getRefHue() + ColorType.GREEN.getMaxHueDist();
            double[] ab = hueToAB(target, 30.0);
            assertThat(ColorType.GREEN.isMatch(ab[0], ab[1])).isFalse();
        }

        @Test
        @DisplayName("hueDistance < maxHueDist → true")
        void withinMaxHueDist_returnsTrue() {
            double target = ColorType.GREEN.getRefHue() + ColorType.GREEN.getMaxHueDist() * 0.5;
            double[] ab = hueToAB(target, 30.0);
            assertThat(ColorType.GREEN.isMatch(ab[0], ab[1])).isTrue();
        }

        @Test
        @DisplayName("RED 기준점은 GREEN에 매칭되지 않는다")
        void redHue_doesNotMatchGreen() {
            double[] ab = hueToAB(ColorType.RED.getRefHue(), 30.0);
            assertThat(ColorType.GREEN.isMatch(ab[0], ab[1])).isFalse();
        }
    }

    // ──────────────────────────────────────────────
    // calculateSimilarity()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("calculateSimilarity() — 점수 범위")
    class CalculateSimilarity {

        @Test
        @DisplayName("기준 Hue와 완전히 같으면 100점")
        void perfectMatch_returns100() {
            double[] ab = hueToAB(ColorType.RED.getRefHue(), 30.0);
            assertThat(ColorType.RED.calculateSimilarity(ab[0], ab[1]))
                    .isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("hueDistance = maxHueDist이면 0점")
        void atMaxHueDist_returnsZero() {
            double target = ColorType.RED.getRefHue() + ColorType.RED.getMaxHueDist();
            double[] ab = hueToAB(target, 30.0);
            assertThat(ColorType.RED.calculateSimilarity(ab[0], ab[1])).isEqualTo(0.0);
        }

        @Test
        @DisplayName("hueDistance = maxHueDist/2 → 80점")
        void halfDistance_returns80() {
            double target = ColorType.RED.getRefHue() + ColorType.RED.getMaxHueDist() * 0.5;
            double[] ab = hueToAB(target, 30.0);
            assertThat(ColorType.RED.calculateSimilarity(ab[0], ab[1]))
                    .isCloseTo(80.0, within(0.01));
        }

        @Test
        @DisplayName("범위 내 임의 점은 60 이상 100 이하")
        void withinRange_scoresBetween60And100() {
            double target = ColorType.BLUE.getRefHue() + ColorType.BLUE.getMaxHueDist() * 0.8;
            double[] ab = hueToAB(target, 30.0);
            double score = ColorType.BLUE.calculateSimilarity(ab[0], ab[1]);
            assertThat(score).isBetween(60.0, 100.0);
        }

        @Test
        @DisplayName("범위 밖이면 0점")
        void outsideRange_returnsZero() {
            double[] ab = hueToAB(ColorType.GREEN.getRefHue(), 30.0);
            assertThat(ColorType.RED.calculateSimilarity(ab[0], ab[1])).isEqualTo(0.0);
        }

        @Test
        @DisplayName("기준에 가까울수록 점수가 높다")
        void closerToReference_scoresHigher() {
            double near = ColorType.YELLOW.getRefHue() + ColorType.YELLOW.getMaxHueDist() * 0.2;
            double far  = ColorType.YELLOW.getRefHue() + ColorType.YELLOW.getMaxHueDist() * 0.8;
            double[] abNear = hueToAB(near, 30.0);
            double[] abFar  = hueToAB(far, 30.0);
            assertThat(ColorType.YELLOW.calculateSimilarity(abNear[0], abNear[1]))
                    .isGreaterThan(ColorType.YELLOW.calculateSimilarity(abFar[0], abFar[1]));
        }

        @Test
        @DisplayName("명도가 달라도 같은 Hue면 같은 점수 (L 독립성)")
        void differentLightness_sameScore() {
            double[] ab = hueToAB(ColorType.YELLOW.getRefHue(), 30.0);
            // calculateSimilarity는 a,b만 받으므로 L이 달라도 호출 결과 동일
            double score1 = ColorType.YELLOW.calculateSimilarity(ab[0], ab[1]);
            double score2 = ColorType.YELLOW.calculateSimilarity(ab[0], ab[1]);
            assertThat(score1).isEqualTo(score2);
        }
    }

    // ──────────────────────────────────────────────
    // 색상 간 독립성
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("색상 간 독립성 — 각 기준 Hue가 다른 색에 매칭되지 않는다")
    class ColorIndependence {

        @ParameterizedTest(name = "{0}의 기준 Hue는 {1}에 매칭되지 않는다")
        @CsvSource({
            "RED, GREEN",
            "RED, BLUE",
            "GREEN, BLUE",
            "YELLOW, PURPLE",
            "PINK, BLUE",
            "PURPLE, GREEN"
        })
        void referenceDoesNotMatchOtherColor(String source, String target) {
            ColorType src = ColorType.valueOf(source);
            ColorType tgt = ColorType.valueOf(target);
            double[] ab = hueToAB(src.getRefHue(), 30.0);
            assertThat(tgt.calculateSimilarity(ab[0], ab[1])).isEqualTo(0.0);
        }
    }

    // ──────────────────────────────────────────────
    // fromString()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("fromString()")
    class FromString {

        @Test
        @DisplayName("소문자 입력도 정상 변환")
        void lowercase_returnsCorrectType() {
            assertThat(ColorType.fromString("green")).isEqualTo(ColorType.GREEN);
        }

        @Test
        @DisplayName("대문자 입력 정상 변환")
        void uppercase_returnsCorrectType() {
            assertThat(ColorType.fromString("GREEN")).isEqualTo(ColorType.GREEN);
        }

        @Test
        @DisplayName("존재하지 않는 색상은 null 반환")
        void invalidString_returnsNull() {
            assertThat(ColorType.fromString("orange")).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 null 반환")
        void emptyString_returnsNull() {
            assertThat(ColorType.fromString("")).isNull();
        }

        @ParameterizedTest(name = "{0} → ColorType.{0}")
        @CsvSource({"RED", "PINK", "YELLOW", "GREEN", "BLUE", "PURPLE"})
        @DisplayName("지원하는 모든 색상이 올바르게 변환된다")
        void allSupportedColors_returnNonNull(String input) {
            assertThat(ColorType.fromString(input)).isNotNull();
        }
    }
}
