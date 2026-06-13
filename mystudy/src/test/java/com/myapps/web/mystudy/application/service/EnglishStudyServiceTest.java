package com.myapps.web.mystudy.application.service;

import com.myapps.web.mystudy.domain.model.EnglishStudy;
import com.myapps.web.mystudy.domain.repository.EnglishStudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EnglishStudyService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class EnglishStudyServiceTest {

    @Mock
    private EnglishStudyRepository englishStudyRepository;

    @InjectMocks
    private EnglishStudyService englishStudyService;

    @Test
    void should_returnAllStudiesOrderByIdDesc_when_findAllOrderByIdDescCalled() {
        // given
        final EnglishStudy study1 = new EnglishStudy();
        study1.setId(2L);
        study1.setEpisode(1L);
        study1.setKoreanSentence("안녕하세요");
        study1.setEnglishSentence("Hello");

        final EnglishStudy study2 = new EnglishStudy();
        study2.setId(1L);
        study2.setEpisode(1L);
        study2.setKoreanSentence("감사합니다");
        study2.setEnglishSentence("Thank you");

        final List<EnglishStudy> expected = List.of(study1, study2);
        when(englishStudyRepository.findAllByOrderByIdDesc()).thenReturn(expected);

        // when
        final List<EnglishStudy> result = englishStudyService.findAllOrderByIdDesc();

        // then
        assertThat(result).isEqualTo(expected);
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
        verify(englishStudyRepository).findAllByOrderByIdDesc();
    }

    @Test
    void should_saveAndReturnEntity_when_saveCalled() {
        // given
        final EnglishStudy englishStudy = new EnglishStudy();
        englishStudy.setEpisode(5L);
        englishStudy.setKoreanSentence("좋은 아침입니다");
        englishStudy.setEnglishSentence("Good morning");

        final EnglishStudy saved = new EnglishStudy();
        saved.setId(1L);
        saved.setEpisode(5L);
        saved.setKoreanSentence("좋은 아침입니다");
        saved.setEnglishSentence("Good morning");

        when(englishStudyRepository.save(englishStudy)).thenReturn(saved);

        // when
        final EnglishStudy result = englishStudyService.save(englishStudy);

        // then
        assertThat(result).isEqualTo(saved);
        assertThat(result.getId()).isEqualTo(1L);
        verify(englishStudyRepository).save(englishStudy);
    }
}
