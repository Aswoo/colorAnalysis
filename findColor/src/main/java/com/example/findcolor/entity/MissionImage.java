package com.example.findcolor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mission_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String imageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
