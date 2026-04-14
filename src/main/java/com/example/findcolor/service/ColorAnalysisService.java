package com.example.findcolor.service;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ColorAnalysisService {

    /**
     * 이미지에서 지배적인 색상을 추출하고, 특정 색상(ex: GREEN)이 포함되어 있는지 판별합니다.
     */
    public boolean analyzeColorSentiment(MultipartFile file, String targetSentiment) throws IOException {
        // 1. 이미지를 OpenCV Mat 객체로 로드
        byte[] bytes = file.getBytes();
        Mat image = opencv_imgcodecs.imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_COLOR);
        
        if (image.empty()) throw new RuntimeException("이미지를 읽을 수 없습니다.");

        // 2. 연산 속도를 위해 이미지 리사이징 (200x200)
        Mat resizedImage = new Mat();
        opencv_imgproc.resize(image, resizedImage, new Size(200, 200));

        // 3. K-means 클러스터링을 위해 데이터를 1차원 행렬로 변환 (Float 타입)
        Mat data = resizedImage.reshape(1, resizedImage.rows() * resizedImage.cols());
        data.convertTo(data, opencv_core.CV_32F);

        // 4. K-means 실행 (K=5: 대표 색상 5개 추출)
        int k = 5;
        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0);
        
        opencv_core.kmeans(data, k, labels, criteria, 3, opencv_core.KMEANS_PP_CENTERS, centers);

        // 5. 추출된 대표 색상들을 HSV로 변환하여 타겟 색상과 비교
        return checkSentimentMatch(centers, targetSentiment);
    }

    private boolean checkSentimentMatch(Mat centers, String targetSentiment) {
        for (int i = 0; i < centers.rows(); i++) {
            // RGB(BGR) 값을 추출
            float b = centers.ptr(i, 0).getFloat();
            float g = centers.ptr(i, 1).getFloat();
            float r = centers.ptr(i, 2).getFloat();

            // HSV로 변환하여 체크 (단순화를 위해 여기서는 주요 색상 판별 로직만 포함)
            // 실제 구현시에는 더 정밀한 HSV 범위 체크 로직이 들어갑니다.
            if (isTargetColor(r, g, b, targetSentiment)) {
                return true; 
            }
        }
        return false;
    }

    private boolean isTargetColor(float r, float g, float b, String sentiment) {
        // 주니어용 단순화 예시: 초록색(GREEN) 느낌 판별
        if ("GREEN".equalsIgnoreCase(sentiment)) {
            return (g > r && g > b && g > 50); // 녹색 광도가 높을 때
        }
        if ("BLUE".equalsIgnoreCase(sentiment)) {
            return (b > r && b > g && b > 50); // 청색 광도가 높을 때
        }
        return false;
    }
}
