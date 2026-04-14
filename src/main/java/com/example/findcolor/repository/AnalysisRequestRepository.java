package com.example.findcolor.repository;

import com.example.findcolor.entity.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
    List<AnalysisRequest> findByUserId(Long userId);
    List<AnalysisRequest> findByUserIdAndIsFavoriteTrue(Long userId);
}
