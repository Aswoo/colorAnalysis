package com.example.findcolor.controller;

import com.example.findcolor.dto.HistoryResponse;
import com.example.findcolor.security.UserDetailsImpl;
import com.example.findcolor.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final MissionService missionService;

    @GetMapping("/history")
    public ResponseEntity<Page<HistoryResponse>> getHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        return ResponseEntity.ok(missionService.getHistory(userDetails.getUser().getId(), pageable));
    }
}
