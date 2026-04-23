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

    @Value("${color.analysis.similarity-threshold}")
    private double similarityThreshold;

    @Value("${color.analysis.match-ratio-threshold}")
    private double matchRatioThreshold;

    @Async
    @Transactional
    public void processAnalysisAsync(Long requestId, byte[] fileBytes, String targetSentiment) {
        log.info("[Analysis Start] Request ID: {}, Target Sentiment: {}", requestId, targetSentiment);
        try {
            AnalysisRequest request = analysisRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("분석 요청을 찾을 수 없습니다."));

            AnalysisData analysisData = analyzeWithRatios(fileBytes, targetSentiment);
            
            double otherChromaticRatio = analysisData.totalChromaticRatio - analysisData.targetColorTotalRatio;
            boolean isMatched = analysisData.targetColorTotalRatio >= matchRatioThreshold
                    && analysisData.targetColorTotalRatio * 2 > otherChromaticRatio;
            double finalScore = analysisData.maxSimilarityScore;

            // 1. 분석 결과 저장
            AnalysisResult result = AnalysisResult.builder()
                    .analysisRequest(request)
                    .similarityScore(finalScore)
                    .matched(isMatched)
                    .build();
            analysisResultRepository.save(result);

            // 2. 색상 팔레트 저장
            List<DetectedColor> detectedList = new ArrayList<>();
            for (DetectedInfo info : analysisData.detectedColors) {
                detectedList.add(DetectedColor.builder()
                        .analysisRequest(request)
                        .colorHex(info.hex)
                        .ratio(info.ratio)
                        .build());
            }
            detectedColorRepository.saveAll(detectedList);
            
            // [핵심 조치] DB에 즉시 물리적으로 기록하도록 강제
            detectedColorRepository.flush();
            analysisResultRepository.flush();

            // 3. 마지막에 상태 변경 및 저장
            request.setStatus("COMPLETED");
            analysisRequestRepository.saveAndFlush(request);

            log.info("[Analysis End] Result saved and flushed for request {}", requestId);

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
        
        Mat resizedImage = new Mat();
        opencv_imgproc.resize(image, resizedImage, new Size(150, 150));
        int totalPixels = resizedImage.rows() * resizedImage.cols();

        Mat data = resizedImage.reshape(1, totalPixels);
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
            float b = centerIndexer.get(i, 0);
            float g = centerIndexer.get(i, 1);
            float r = centerIndexer.get(i, 2);

            Mat bgrPixel = new Mat(1, 1, opencv_core.CV_8UC3, new Scalar(b, g, r, 0));
            Mat hsvPixel = new Mat();
            opencv_imgproc.cvtColor(bgrPixel, hsvPixel, opencv_imgproc.COLOR_BGR2HSV);

            int h = hsvPixel.data().get(0) & 0xFF;
            int s = hsvPixel.data().get(1) & 0xFF;
            int v = hsvPixel.data().get(2) & 0xFF;

            double similarity = colorType.calculateSimilarity(h, s, v);
            double ratio = (double) clusterCounts[i] / totalPixels;
            String hex = String.format("#%02X%02X%02X", (int)r, (int)g, (int)b);

            analysisData.detectedColors.add(new DetectedInfo(hex, ratio));

            if (s >= 50 && v >= 50) {
                analysisData.totalChromaticRatio += ratio;
            }
            if (similarity > analysisData.maxSimilarityScore) {
                analysisData.maxSimilarityScore = similarity;
            }
            if (similarity >= similarityThreshold) {
                analysisData.targetColorTotalRatio += ratio;
            }
        }
        return analysisData;
    }

    private static class AnalysisData {
        double maxSimilarityScore = 0.0;
        double targetColorTotalRatio = 0.0;
        double totalChromaticRatio = 0.0;
        List<DetectedInfo> detectedColors = new ArrayList<>();
    }

    private static record DetectedInfo(String hex, double ratio) {}
}
