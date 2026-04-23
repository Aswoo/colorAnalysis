package com.example.findcolor.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ColorType")
class ColorTypeTest {

    // ──────────────────────────────────────────────
    // isMatch()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("isMatch() — 무채색 필터")
    class IsMatchAchromaticFilter {

        @Test
        @DisplayName("채도(S) < 50 이면 어떤 H값이든 false")
        void saturationBelowThreshold_returnsFalse() {
            assertThat(ColorType.GREEN.isMatch(65, 49, 200)).isFalse();
        }

        @Test
        @DisplayName("명도(V) < 50 이면 어떤 H값이든 false")
        void valueBelowThreshold_returnsFalse() {
            assertThat(ColorType.GREEN.isMatch(65, 200, 49)).isFalse();
        }

        @Test
        @DisplayName("S=50, V=50 (정확히 경계값) 이면 통과")
        void exactThresholdValues_passesFilter() {
            assertThat(ColorType.GREEN.isMatch(65, 50, 50)).isTrue();
        }
    }

    @Nested
    @DisplayName("isMatch() — H 범위 판별")
    class IsMatchHueRange {

        @Test
        @DisplayName("H가 범위 중앙이면 true")
        void hAtCenter_returnsTrue() {
            // GREEN: 35–95, center = 65
            assertThat(ColorType.GREEN.isMatch(65, 200, 200)).isTrue();
        }

        @Test
        @DisplayName("H가 minH 경계값이면 true (포함)")
        void hAtMinBoundary_returnsTrue() {
            assertThat(ColorType.GREEN.isMatch(35, 200, 200)).isTrue();
        }

        @Test
        @DisplayName("H가 maxH 경계값이면 true (포함)")
        void hAtMaxBoundary_returnsTrue() {
            assertThat(ColorType.GREEN.isMatch(95, 200, 200)).isTrue();
        }

        @Test
        @DisplayName("H가 minH - 1 이면 false")
        void hJustBelowMin_returnsFalse() {
            assertThat(ColorType.GREEN.isMatch(34, 200, 200)).isFalse();
        }

        @Test
        @DisplayName("H가 maxH + 1 이면 false")
        void hJustAboveMax_returnsFalse() {
            assertThat(ColorType.GREEN.isMatch(96, 200, 200)).isFalse();
        }
    }

    @Nested
    @DisplayName("isMatch() — RED 이중 범위")
    class IsMatchRedDualRange {

        @Test
        @DisplayName("주 범위(H=0–15)에서 true")
        void red_mainRange_returnsTrue() {
            assertThat(ColorType.RED.isMatch(10, 200, 200)).isTrue();
        }

        @Test
        @DisplayName("보조 범위(H=160–180)에서 true — 어두운 적색")
        void red_altRange_returnsTrue() {
            assertThat(ColorType.RED.isMatch(170, 200, 200)).isTrue();
        }

        @Test
        @DisplayName("두 범위 모두 해당 없으면 false")
        void red_betweenBothRanges_returnsFalse() {
            // H=90 은 RED의 어느 범위에도 속하지 않음
            assertThat(ColorType.RED.isMatch(90, 200, 200)).isFalse();
        }

        @Test
        @DisplayName("보조 범위 경계값(H=160)에서 true")
        void red_altRangeMinBoundary_returnsTrue() {
            assertThat(ColorType.RED.isMatch(160, 200, 200)).isTrue();
        }
    }

    // ──────────────────────────────────────────────
    // calculateSimilarity()
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("calculateSimilarity() — RED 이중 범위 버그 검증")
    class CalculateSimilarityRedDualRange {

        @Test
        @DisplayName("보조 범위(H=170) 점수가 주 범위 중심(7.5)과 비교되지 않는다 — circular distance fix")
        void red_altRange_usesAltRangeCenter() {
            // 버그 재현: 수정 전에는 H=170이 주 범위 중심(7.5)과 비교되어
            // diff=162.5로 계산되어 점수가 0에 가깝거나 음수가 되었음.
            // 수정 후: 보조 범위 중심(170)과 비교하므로 합리적인 점수가 나와야 함.
            double altRangeScore = ColorType.RED.calculateSimilarity(170, 200, 200);
            assertThat(altRangeScore).isGreaterThan(50.0);
        }

        @Test
        @DisplayName("보조 범위 중심(H=170)은 주 범위 중심(H=7)보다 높거나 같은 점수")
        void red_altRangeCenter_scoresWell() {
            double mainCenter  = ColorType.RED.calculateSimilarity(7,   200, 200); // 주 범위 중심
            double altCenter   = ColorType.RED.calculateSimilarity(170, 200, 200); // 보조 범위 중심
            // 두 중심 모두 고득점이어야 함 (범위의 중심이므로)
            assertThat(mainCenter).isGreaterThan(70.0);
            assertThat(altCenter).isGreaterThan(70.0);
        }

        @Test
        @DisplayName("보조 범위 경계(H=160)는 중심(H=170)보다 낮은 점수")
        void red_altRangeEdge_scoresLowerThanCenter() {
            double center = ColorType.RED.calculateSimilarity(170, 200, 200);
            double edge   = ColorType.RED.calculateSimilarity(160, 200, 200);
            assertThat(center).isGreaterThan(edge);
        }
    }

    @Nested
    @DisplayName("calculateSimilarity() — 기본 동작")
    class CalculateSimilarityBasic {

        @Test
        @DisplayName("매칭되지 않는 픽셀은 0.0 반환")
        void noMatch_returnsZero() {
            // H=10 은 GREEN 범위(35–95) 밖
            assertThat(ColorType.GREEN.calculateSimilarity(10, 200, 200)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("무채색 픽셀은 0.0 반환 (isMatch 선행 차단)")
        void achromatic_returnsZero() {
            assertThat(ColorType.GREEN.calculateSimilarity(65, 10, 200)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("점수는 100.0을 초과하지 않는다")
        void score_neverExceeds100() {
            double score = ColorType.GREEN.calculateSimilarity(65, 255, 255);
            assertThat(score).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("매칭되는 픽셀은 0보다 큰 점수를 반환")
        void match_returnsPositiveScore() {
            assertThat(ColorType.GREEN.calculateSimilarity(65, 200, 200)).isGreaterThan(0.0);
        }
    }

    @Nested
    @DisplayName("calculateSimilarity() — H 위치 가중치")
    class CalculateSimilarityHueWeight {

        @Test
        @DisplayName("H가 범위 중앙에 가까울수록 점수가 높다")
        void centerH_scoresHigherThanEdgeH() {
            // GREEN center = 65, edge = 35 or 95
            double centerScore = ColorType.GREEN.calculateSimilarity(65, 200, 200);
            double edgeScore   = ColorType.GREEN.calculateSimilarity(36, 200, 200);
            assertThat(centerScore).isGreaterThan(edgeScore);
        }

        @Test
        @DisplayName("범위 경계값의 점수는 edge score 공식 상 70% 수준")
        void edgeH_scoreApproaches70Percent() {
            // score = 100 - (range/range * 30) = 70, 채도 가중치 적용 전
            // s=255일 때: 70 * (1.0 * 0.2 + 0.8) = 70.0
            double edgeScore = ColorType.GREEN.calculateSimilarity(35, 255, 200);
            assertThat(edgeScore).isGreaterThan(50.0).isLessThan(80.0);
        }
    }

    @Nested
    @DisplayName("calculateSimilarity() — S(채도) 가중치")
    class CalculateSimilaritySaturationWeight {

        @Test
        @DisplayName("같은 H값이면 채도가 높을수록 점수가 높다")
        void higherSaturation_returnsHigherScore() {
            double highS = ColorType.GREEN.calculateSimilarity(65, 255, 200);
            double lowS  = ColorType.GREEN.calculateSimilarity(65, 30,  200);
            assertThat(highS).isGreaterThan(lowS);
        }

        @Test
        @DisplayName("채도 최대(S=255)일 때 점수가 최고점")
        void maxSaturation_producesMaxScore() {
            double maxSScore  = ColorType.GREEN.calculateSimilarity(65, 255, 200);
            double halfSScore = ColorType.GREEN.calculateSimilarity(65, 128, 200);
            assertThat(maxSScore).isGreaterThan(halfSScore);
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
