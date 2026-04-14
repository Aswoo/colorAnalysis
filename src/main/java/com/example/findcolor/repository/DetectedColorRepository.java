package com.example.findcolor.repository;

import com.example.findcolor.entity.DetectedColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedColorRepository extends JpaRepository<DetectedColor, Long> {
    List<DetectedColor> findByAnalysisRequestId(Long analysisId);
}
