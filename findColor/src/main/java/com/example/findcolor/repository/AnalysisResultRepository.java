package com.example.findcolor.repository;

import com.example.findcolor.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findByAnalysisRequestId(Long analysisId);
}
