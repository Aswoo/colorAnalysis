package com.example.findcolor.controller;

import com.example.findcolor.dto.HistoryResponse;
import com.example.findcolor.security.UserDetailsImpl;
import com.example.findcolor.service.MissionService;
import com.example.findcolor.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MissionService missionService;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setup() {
        User user = User.builder().id(1L).email("test@example.com").password("password").build();
        userDetails = new UserDetailsImpl(user);
    }

    @Test
    void getHistoryShouldReturnHistoryPage() throws Exception {
        HistoryResponse item = HistoryResponse.builder()
                .id(1L)
                .imageUrl("http://example.com/image.jpg")
                .status("COMPLETED")
                .similarityScore(85.5)
                .matched(true)
                .createdAt(LocalDateTime.now())
                .isFavorite(false)
                .build();
        
        Page<HistoryResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        
        Mockito.when(missionService.getHistory(Mockito.eq(1L), Mockito.any())).thenReturn(page);

        mockMvc.perform(get("/api/analysis/history").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].matched").value(true));
    }
}
