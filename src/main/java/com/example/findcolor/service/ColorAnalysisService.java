package com.example.findcolor.service;

import com.example.findcolor.entity.AnalysisRequest;
import com.example.findcolor.entity.AnalysisResult;
import com.example.findcolor.entity.ColorType;
import com.example.findcolor.repository.AnalysisRequestRepository;
import com.example.findcolor.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ColorAnalysisService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 비동기로 색상 분석을 수행하고 결과를 DB에 저장합니다.
     */
    @Async
    @Transactional
    public void processAnalysisAsync(Long requestId, MultipartFile file, String targetSentiment) {
        try {
            // 1. 분석 수행
            boolean isMatched = analyzeColorSentiment(file, targetSentiment);

            // 2. 요청 상태 업데이트 (COMPLETED)
            AnalysisRequest request = analysisRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("분석 요청을 찾을 수 없습니다. ID: " + requestId));
            request.setStatus("COMPLETED");

            // 3. 분석 결과 저장
            AnalysisResult result = AnalysisResult.builder()
                    .analysisRequest(request)
                    .similarityScore(isMatched ? 100.0 : 0.0)
                    .matched(isMatched)
                    .build();
            analysisResultRepository.save(result);

        } catch (Exception e) {
            // 실패 시 상태만 업데이트
            analysisRequestRepository.findById(requestId).ifPresent(request -> {
                request.setStatus("FAILED");
                analysisRequestRepository.save(request);
            });
        }
    }

    /**
     * 이미지에서 지배적인 색상을 추출하고, 특정 색상이 포함되어 있는지 판별합니다.
     */
    public boolean analyzeColorSentiment(MultipartFile file, String targetSentiment) throws IOException {
        // 분석 대상 Enum 찾기
        ColorType colorType = ColorType.fromString(targetSentiment);
        if (colorType == null) return false;

        byte[] bytes = file.getBytes();
        Mat image = opencv_imgcodecs.imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_COLOR);
        
        if (image.empty()) throw new RuntimeException("이미지를 읽을 수 없습니다.");

        // 1. 리사이징 (성능 최적화)
        Mat resizedImage = new Mat();
        opencv_imgproc.resize(image, resizedImage, new Size(200, 200));

        // 2. K-means 클러스터링
        Mat data = resizedImage.reshape(1, resizedImage.rows() * resizedImage.cols());
        data.convertTo(data, opencv_core.CV_32F);

        int k = 5;
        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0);
        opencv_core.kmeans(data, k, labels, criteria, 3, opencv_core.KMEANS_PP_CENTERS, centers);

        // 3. 추출된 대표 색상들(BGR)을 HSV로 변환하여 분석
        return checkSentimentMatch(centers, colorType);
    }

    private boolean checkSentimentMatch(Mat centers, ColorType colorType) {
        for (int i = 0; i < centers.rows(); i++) {
            float b = centers.ptr(i, 0).getFloat();
            float g = centers.ptr(i, 1).getFloat();
            float r = centers.ptr(i, 2).getFloat();

            // BGR -> HSV 변환
            Mat bgrPixel = new Mat(1, 1, opencv_core.CV_32FC3, new Scalar(b, g, r, 0));
            Mat hsvPixel = new Mat();
            opencv_imgproc.cvtColor(bgrPixel, hsvPixel, opencv_imgproc.COLOR_BGR2HSV);

            float h = hsvPixel.ptr(0, 0).getFloat();
            float s = hsvPixel.ptr(0, 1).getFloat();
            float v = hsvPixel.ptr(0, 2).getFloat();

            // Enum에 판별 로직 위임 (객체지향 설계)
            if (colorType.isMatch(h, s, v)) {
                return true; 
            }
        }
        return false;
    }
}
