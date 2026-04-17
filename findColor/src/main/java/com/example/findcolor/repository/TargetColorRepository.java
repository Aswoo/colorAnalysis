package com.example.findcolor.repository;

import com.example.findcolor.entity.TargetColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetColorRepository extends JpaRepository<TargetColor, Long> {
    List<TargetColor> findByAnalysisRequestId(Long analysisId);
}
