package com.example.findcolor.service;

import com.example.findcolor.entity.AnalysisRequest;
import com.example.findcolor.entity.AnalysisResult;
import com.example.findcolor.entity.ColorType;
import com.example.findcolor.entity.DetectedColor;
import com.example.findcolor.repository.AnalysisRequestRepository;
import com.example.findcolor.repository.AnalysisResultRepository;
import com.example.findcolor.repository.DetectedColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColorAnalysisService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final DetectedColorRepository detectedColorRepository;

    @Value("${color.analysis.match-score-threshold}")
    private double matchScoreThreshold;

    @Async
    @Transactional
    public void processAnalysisAsync(Long requestId, byte[] fileBytes, String targetSentiment) {
        log.info("[Analysis Start] Request ID: {}, Target Sentiment: {}", requestId, targetSentiment);
        try {
            AnalysisRequest request = analysisRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("분석 요청을 찾을 수 없습니다."));

            AnalysisData analysisData = analyzeWithRatios(fileBytes, targetSentiment);

            double otherChromaticRatio = analysisData.totalChromaticRatio - analysisData.targetColorTotalRatio;
            boolean isMatched = analysisData.targetWeightedScoreSum >= matchScoreThreshold
                    && analysisData.targetColorTotalRatio * 2 > otherChromaticRatio;
            double finalScore = analysisData.maxSimilarityScore;

            AnalysisResult result = AnalysisResult.builder()
                    .analysisRequest(request)
                    .similarityScore(finalScore)
                    .matched(isMatched)
                    .build();
            analysisResultRepository.save(result);

            List<DetectedColor> detectedList = new ArrayList<>();
            for (DetectedInfo info : analysisData.detectedColors) {
                detectedList.add(DetectedColor.builder()
                        .analysisRequest(request)
                        .colorHex(info.hex)
                        .ratio(info.ratio)
                        .build());
            }
            detectedColorRepository.saveAll(detectedList);

            detectedColorRepository.flush();
            analysisResultRepository.flush();

            request.setStatus("COMPLETED");
            analysisRequestRepository.saveAndFlush(request);

            log.info("[Analysis End] Request {} → matched={}, score={:.2f}", requestId, isMatched, finalScore);

        } catch (Exception e) {
            log.error("[Analysis Error] Request ID: {}, Message: {}", requestId, e.getMessage());
            analysisRequestRepository.findById(requestId).ifPresent(request -> {
                request.setStatus("FAILED");
                analysisRequestRepository.saveAndFlush(request);
            });
        }
    }

    private AnalysisData analyzeWithRatios(byte[] fileBytes, String targetSentiment) {
        ColorType colorType = ColorType.fromString(targetSentiment);
        Mat image = opencv_imgcodecs.imdecode(new Mat(fileBytes), opencv_imgcodecs.IMREAD_COLOR);

        Mat resized = new Mat();
        opencv_imgproc.resize(image, resized, new Size(150, 150));
        int totalPixels = resized.rows() * resized.cols();

        // K-means를 Lab 공간에서 수행: 클러스터링 기준 = 인간 시각 거리
        Mat labImage = new Mat();
        opencv_imgproc.cvtColor(resized, labImage, opencv_imgproc.COLOR_BGR2Lab);

        Mat data = labImage.reshape(1, totalPixels);
        Mat data32f = new Mat();
        data.convertTo(data32f, opencv_core.CV_32F);

        int k = 8;
        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0);
        opencv_core.kmeans(data32f, k, labels, criteria, 3, opencv_core.KMEANS_PP_CENTERS, centers);

        int[] clusterCounts = new int[k];
        IntIndexer labelIndexer = labels.createIndexer();
        for (int i = 0; i < totalPixels; i++) {
            clusterCounts[labelIndexer.get(i)]++;
        }

        AnalysisData analysisData = new AnalysisData();
        FloatIndexer centerIndexer = centers.createIndexer();

        for (int i = 0; i < k; i++) {
            // 클러스터 중심 = Lab byte scale (L: 0-255, a/b: 0-255)
            float rawL = centerIndexer.get(i, 0);
            float rawA = centerIndexer.get(i, 1);
            float rawB = centerIndexer.get(i, 2);

            double labL = rawL * 100.0 / 255.0;
            double labA = rawA - 128.0;
            double labB = rawB - 128.0;

            // hex 저장: Lab → BGR 역변환
            Mat labPixel = new Mat(1, 1, opencv_core.CV_8UC3, new Scalar(rawL, rawA, rawB, 0));
            Mat bgrPixel = new Mat();
            opencv_imgproc.cvtColor(labPixel, bgrPixel, opencv_imgproc.COLOR_Lab2BGR);
            int r = bgrPixel.data().get(2) & 0xFF;
            int g = bgrPixel.data().get(1) & 0xFF;
            int b = bgrPixel.data().get(0) & 0xFF;

            double ratio = (double) clusterCounts[i] / totalPixels;
            analysisData.detectedColors.add(new DetectedInfo(String.format("#%02X%02X%02X", r, g, b), ratio));

            // 밝기 가중치: Lab L 기준 (L<12 ≈ V<30, L<20 ≈ V<50)
            double brightnessWeight;
            if (labL < 12.0) {
                brightnessWeight = 0.0;
            } else if (labL < 20.0) {
                brightnessWeight = (labL - 12.0) / 8.0 * 0.5 + 0.5;
            } else {
                brightnessWeight = 1.0;
            }

            // 유채색 합계: Chroma≥15 AND L≥12
            double chroma = Math.sqrt(labA * labA + labB * labB);
            if (chroma >= 15.0 && labL >= 12.0) {
                analysisData.totalChromaticRatio += ratio;
            }

            double similarity = colorType.calculateSimilarity(labA, labB);
            double effectiveRatio = ratio * brightnessWeight;

            if (similarity > analysisData.maxSimilarityScore) {
                analysisData.maxSimilarityScore = similarity;
            }
            if (similarity > 0) {
                analysisData.targetColorTotalRatio += effectiveRatio;
                analysisData.targetWeightedScoreSum += similarity * effectiveRatio;
            }
        }
        return analysisData;
    }

    private static class AnalysisData {
        double maxSimilarityScore = 0.0;
        double targetColorTotalRatio = 0.0;
        double targetWeightedScoreSum = 0.0;
        double totalChromaticRatio = 0.0;
        List<DetectedInfo> detectedColors = new ArrayList<>();
    }

    private static record DetectedInfo(String hex, double ratio) {}
}
