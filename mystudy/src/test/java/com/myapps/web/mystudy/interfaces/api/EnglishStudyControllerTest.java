package com.myapps.web.mystudy.interfaces.api;

import com.myapps.web.mystudy.application.service.EnglishStudyService;
import com.myapps.web.mystudy.domain.model.EnglishStudy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * EnglishStudyController의 HTTP 엔드포인트를 검증하는 슬라이스 테스트.
 *
 * <p>MockMvc를 이용하여 실제 HTTP 요청/응답 흐름을 검증합니다.
 */
@WebMvcTest(EnglishStudyController.class)
class EnglishStudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnglishStudyService englishStudyService;

    @Test
    void should_returnThymeleafView_when_getEnglishStudyPage() throws Exception {
        final EnglishStudy study = new EnglishStudy();
        study.setId(1L);
        study.setEpisode(1L);
        study.setKoreanSentence("안녕하세요");
        study.setEnglishSentence("Hello");

        given(englishStudyService.findAllOrderByIdDesc()).willReturn(List.of(study));

        mockMvc.perform(get("/english-study"))
                .andExpect(status().isOk())
                .andExpect(view().name("englishstudy/english_study"))
                .andExpect(model().attributeExists("englishStudies"));
    }

    @Test
    void should_returnJsonList_when_getApiEnglishStudy() throws Exception {
        final EnglishStudy study = new EnglishStudy();
        study.setId(1L);
        study.setEpisode(2L);
        study.setKoreanSentence("감사합니다");
        study.setEnglishSentence("Thank you");

        given(englishStudyService.findAllOrderByIdDesc()).willReturn(List.of(study));

        mockMvc.perform(get("/api/english-study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].episode").value(2))
                .andExpect(jsonPath("$[0].koreanSentence").value("감사합니다"))
                .andExpect(jsonPath("$[0].englishSentence").value("Thank you"));
    }

    @Test
    void should_returnCreatedStatus_when_postNewEnglishStudy() throws Exception {
        final EnglishStudy inputStudy = new EnglishStudy();
        inputStudy.setEpisode(3L);
        inputStudy.setKoreanSentence("좋은 아침");
        inputStudy.setEnglishSentence("Good morning");

        final EnglishStudy savedStudy = new EnglishStudy();
        savedStudy.setId(10L);
        savedStudy.setEpisode(3L);
        savedStudy.setKoreanSentence("좋은 아침");
        savedStudy.setEnglishSentence("Good morning");

        given(englishStudyService.save(any(EnglishStudy.class))).willReturn(savedStudy);

        mockMvc.perform(post("/api/english-study")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputStudy)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.episode").value(3))
                .andExpect(jsonPath("$.koreanSentence").value("좋은 아침"))
                .andExpect(jsonPath("$.englishSentence").value("Good morning"));
    }
}
