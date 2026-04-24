package com.example.findcolor.repository;

import com.example.findcolor.entity.AnalysisRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
    Page<AnalysisRequest> findByUserId(Long userId, Pageable pageable);
    Page<AnalysisRequest> findByUserIdAndIsFavoriteTrue(Long userId, Pageable pageable);
}
