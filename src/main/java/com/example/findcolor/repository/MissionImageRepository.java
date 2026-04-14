package com.example.findcolor.repository;

import com.example.findcolor.entity.MissionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionImageRepository extends JpaRepository<MissionImage, Long> {
    List<MissionImage> findByUserId(Long userId);
}
