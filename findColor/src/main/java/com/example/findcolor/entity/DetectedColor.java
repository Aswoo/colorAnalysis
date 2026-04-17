package com.example.findcolor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detected_colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedColor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisRequest analysisRequest;

    @Column(nullable = false)
    private String colorHex;

    private Double ratio;
}
