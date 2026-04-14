package com.example.findcolor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analysis_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisRequest analysisRequest;

    private Double similarityScore;

    private Boolean matched;
}
