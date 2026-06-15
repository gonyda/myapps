package com.myapps.web.mystudy.application.service;

import com.myapps.web.mystudy.application.dto.QuizQuestionDto;
import com.myapps.web.mystudy.application.dto.QuizResponse;
import com.myapps.web.mystudy.domain.model.EnglishStudy;
import com.myapps.web.mystudy.domain.repository.EnglishStudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * QuizService의 단위 테스트.
 *
 * <p>퀴즈 생성 로직의 핵심 동작과 경계 조건을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private EnglishStudyRepository englishStudyRepository;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(englishStudyRepository, new Random(42));
    }

    @Test
    void should_returnEmptyQuiz_when_noDataExists() {
        when(englishStudyRepository.findAll()).thenReturn(List.of());

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).isEmpty();
    }

    @Test
    void should_returnEmptyQuiz_when_allEpisodesHaveOnlyOneSentence() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 2L, "Goodbye", "안녕히 가세요")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).isEmpty();
    }

    @Test
    void should_generateQuestions_when_episodeHasExactlyTwoSentences() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 1L, "Goodbye", "안녕히 가세요")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).hasSize(2);
        for (final QuizQuestionDto question : response.questions()) {
            assertThat(question.choices()).hasSize(2);
            assertThat(question.answerIndex()).isBetween(0, question.choices().size() - 1);
        }
    }

    @Test
    void should_generateExactly10Questions_when_moreThan10EligibleSentences() {
        final List<EnglishStudy> studies = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            studies.add(createStudy(i, 1L, "English " + i, "한국어 " + i));
        }
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).hasSize(10);
    }

    @Test
    void should_generateCorrectChoiceCount_when_episodeHasFourOrMoreSentences() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 1L, "Goodbye", "안녕히 가세요"),
                createStudy(3L, 1L, "Thank you", "감사합니다"),
                createStudy(4L, 1L, "Sorry", "죄송합니다"),
                createStudy(5L, 1L, "Please", "부탁합니다")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).hasSize(5);
        for (final QuizQuestionDto question : response.questions()) {
            assertThat(question.choices()).hasSize(4);
            assertThat(question.answerIndex()).isBetween(0, 3);
        }
    }

    @Test
    void should_haveNoDuplicateChoices_when_quizGenerated() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 1L, "Goodbye", "안녕히 가세요"),
                createStudy(3L, 1L, "Thank you", "감사합니다"),
                createStudy(4L, 1L, "Sorry", "죄송합니다")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        for (final QuizQuestionDto question : response.questions()) {
            assertThat(question.choices()).doesNotHaveDuplicates();
        }
    }

    @Test
    void should_excludeIneligibleEpisodes_when_mixedEpisodeData() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 1L, "Goodbye", "안녕히 가세요"),
                createStudy(3L, 2L, "Single", "단독")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        assertThat(response.questions()).hasSize(2);
    }

    @Test
    void should_haveCorrectAnswerInChoices_when_quizGenerated() {
        final List<EnglishStudy> studies = List.of(
                createStudy(1L, 1L, "Hello", "안녕하세요"),
                createStudy(2L, 1L, "Goodbye", "안녕히 가세요"),
                createStudy(3L, 1L, "Thank you", "감사합니다")
        );
        when(englishStudyRepository.findAll()).thenReturn(studies);

        final QuizResponse response = quizService.generateQuiz();

        for (final QuizQuestionDto question : response.questions()) {
            assertThat(question.answerIndex()).isGreaterThanOrEqualTo(0);
            assertThat(question.answerIndex()).isLessThan(question.choices().size());
        }
    }

    private EnglishStudy createStudy(final Long id, final Long episode,
                                     final String english, final String korean) {
        final EnglishStudy study = new EnglishStudy();
        study.setId(id);
        study.setEpisode(episode);
        study.setEnglishSentence(english);
        study.setKoreanSentence(korean);
        return study;
    }
}
