package com.example.findcolor.service;

import com.example.findcolor.entity.ColorType;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * sampleimage 4장을 실제 분석하여 결과를 콘솔에 출력한다.
 * DB/Spring 컨텍스트 없이 OpenCV + ColorType만 사용.
 * K-means를 Lab 공간에서 수행 (ColorAnalysisService와 동일한 파이프라인).
 */
class ColorAnalysisReportTest {

    private static final double MATCH_SCORE_THRESHOLD = 15.0;
    private static final String SAMPLE_DIR = "sampleimage/";

    private record ClusterInfo(String hex, double ratio, double labL, double labA, double labB) {}

    @Test
    void printReportForAllSampleImages() throws IOException {
        String[] images = {
            "blueFlower.jpg",
            "yellow_tree.jpg",
            "grapes.jpeg",
            "pink_red_rose.jpeg",
            "blue_purple.jpg",
            "lemon.jpg",
            "sunshine.jpeg"
        };

        for (String imageName : images) {
            Path path = Path.of(SAMPLE_DIR + imageName);
            if (!Files.exists(path)) {
                System.out.println("⚠ 파일 없음: " + path.toAbsolutePath());
                continue;
            }
            byte[] fileBytes = Files.readAllBytes(path);
            List<ClusterInfo> clusters = extractClusters(fileBytes);
            printReport(imageName, clusters);
        }
    }

    private List<ClusterInfo> extractClusters(byte[] fileBytes) {
        Mat image = opencv_imgcodecs.imdecode(new Mat(fileBytes), opencv_imgcodecs.IMREAD_COLOR);
        Mat resized = new Mat();
        opencv_imgproc.resize(image, resized, new Size(150, 150));
        int totalPixels = resized.rows() * resized.cols();

        // K-means를 Lab 공간에서 수행
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

        int[] counts = new int[k];
        IntIndexer labelIdx = labels.createIndexer();
        for (int i = 0; i < totalPixels; i++) counts[labelIdx.get(i)]++;

        List<ClusterInfo> result = new ArrayList<>();
        FloatIndexer centerIdx = centers.createIndexer();

        for (int i = 0; i < k; i++) {
            float rawL = centerIdx.get(i, 0);
            float rawA = centerIdx.get(i, 1);
            float rawB = centerIdx.get(i, 2);

            double labL = rawL * 100.0 / 255.0;
            double labA = rawA - 128.0;
            double labB = rawB - 128.0;

            // hex: Lab → BGR 역변환
            Mat labPixel = new Mat(1, 1, opencv_core.CV_8UC3, new Scalar(rawL, rawA, rawB, 0));
            Mat bgrPixel = new Mat();
            opencv_imgproc.cvtColor(labPixel, bgrPixel, opencv_imgproc.COLOR_Lab2BGR);
            int r = bgrPixel.data().get(2) & 0xFF;
            int g = bgrPixel.data().get(1) & 0xFF;
            int b = bgrPixel.data().get(0) & 0xFF;
            String hex = String.format("#%02X%02X%02X", r, g, b);

            double ratio = (double) counts[i] / totalPixels;
            result.add(new ClusterInfo(hex, ratio, labL, labA, labB));
        }

        result.sort((a, c) -> Double.compare(c.ratio(), a.ratio()));
        return result;
    }

    private void printReport(String imageName, List<ClusterInfo> clusters) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + imageName);
        System.out.println("=".repeat(60));

        System.out.println("\n[ Color Palette ]");
        System.out.printf("  %-10s  %6s  %6s  %6s  %6s%n", "Hex", "Ratio%", "L", "a", "b");
        System.out.println("  " + "-".repeat(46));
        for (ClusterInfo c : clusters) {
            System.out.printf("  %-10s  %5.1f%%  %6.1f  %6.1f  %6.1f%n",
                c.hex(), c.ratio() * 100, c.labL(), c.labA(), c.labB());
        }

        System.out.println("\n[ Color Analysis ]");
        System.out.printf("  %-8s  %8s  %10s  %9s  %s%n",
            "Color", "MaxSim", "WgtScore", "TargetRat", "Matched?");
        System.out.println("  " + "-".repeat(55));

        for (ColorType colorType : ColorType.values()) {
            double maxSim = 0.0;
            double weightedScoreSum = 0.0;
            double targetColorTotalRatio = 0.0;
            double totalChromaticRatio = 0.0;

            for (ClusterInfo c : clusters) {
                double brightnessWeight = c.labL() < 12.0 ? 0.0
                    : c.labL() < 20.0 ? (c.labL() - 12.0) / 8.0 * 0.5 + 0.5
                    : 1.0;

                double chroma = Math.sqrt(c.labA() * c.labA() + c.labB() * c.labB());
                if (chroma >= 15.0 && c.labL() >= 12.0) {
                    totalChromaticRatio += c.ratio();
                }

                double sim = colorType.calculateSimilarity(c.labA(), c.labB());
                if (sim > maxSim) maxSim = sim;

                if (sim > 0) {
                    double eff = c.ratio() * brightnessWeight;
                    targetColorTotalRatio += eff;
                    weightedScoreSum += sim * eff;
                }
            }

            double otherChromatic = totalChromaticRatio - targetColorTotalRatio;
            boolean matched = weightedScoreSum >= MATCH_SCORE_THRESHOLD
                && targetColorTotalRatio * 2 > otherChromatic;

            System.out.printf("  %-8s  %7.1f%%  %10.2f  %8.1f%%  %s%n",
                colorType.name(),
                maxSim,
                weightedScoreSum,
                targetColorTotalRatio * 100,
                matched ? "✅ true" : "❌ false");
        }
    }
}
