package com.myapps.web.mystudy.application.service;

import com.myapps.web.mystudy.application.dto.QuizQuestionDto;
import com.myapps.web.mystudy.application.dto.QuizResponse;
import com.myapps.web.mystudy.domain.model.EnglishStudy;
import com.myapps.web.mystudy.domain.repository.EnglishStudyRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QuizService의 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 퀴즈 생성 알고리즘의 정확성을 다양한 입력에 대해 검증합니다.
 */
@Tag("Feature: random-quiz-show")
class QuizServicePropertyTest {

    private static final int MIN_SENTENCES_PER_EPISODE = 2;

    /**
     * Property 1: 생성된 퀴즈의 모든 문제가 문장 2개 이상인 에피소드에서 출제되었는지 검증합니다.
     *
     * <p>For any generated quiz from any dataset, every question in the quiz must originate
     * from an episode that contains at least 2 sentences in the source data.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 1: Episode Eligibility")
    void allQuestionsMustOriginateFromEligibleEpisodes(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final Set<String> eligibleSentences = studies.stream()
                .collect(Collectors.groupingBy(EnglishStudy::getEpisode))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= MIN_SENTENCES_PER_EPISODE)
                .flatMap(entry -> entry.getValue().stream())
                .flatMap(study -> Stream.of(study.getEnglishSentence(), study.getKoreanSentence()))
                .collect(Collectors.toSet());

        for (final QuizQuestionDto question : response.questions()) {
            assertThat(eligibleSentences).contains(question.question());
        }
    }

    /**
     * Property 2: 생성된 퀴즈 문제 수가 min(10, 적격 문장 총 수)와 동일한지 검증합니다.
     *
     * <p>Validates: Requirements 2.3
     *
     * <p>For any dataset of EnglishStudy records, the number of questions generated equals
     * {@code min(10, totalEligibleSentences)} where totalEligibleSentences is the count of
     * sentences belonging to episodes with 2 or more sentences.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 2: Question Count Invariant")
    void questionCountEqualsMinOfTenAndTotalEligibleSentences(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final long totalEligibleSentences = studies.stream()
                .collect(Collectors.groupingBy(EnglishStudy::getEpisode))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= MIN_SENTENCES_PER_EPISODE)
                .flatMap(entry -> entry.getValue().stream())
                .count();

        final int expectedQuestionCount = (int) Math.min(10, totalEligibleSentences);
        assertThat(response.questions()).hasSize(expectedQuestionCount);
    }

    /**
     * Property 3: 출제 방향 매핑 정확성을 검증합니다.
     *
     * <p>Validates: Requirements 2.4, 2.5, 2.6
     *
     * <p>For any generated quiz question, if the question text equals an English sentence
     * from the source record, then the correct answer (at answerIndex) must equal that
     * record's Korean sentence, and vice versa.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 3: Direction Mapping Correctness")
    void directionMappingMustBeCorrect(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final Map<String, String> englishToKorean = new HashMap<>();
        final Map<String, String> koreanToEnglish = new HashMap<>();
        for (final EnglishStudy study : studies) {
            englishToKorean.put(study.getEnglishSentence(), study.getKoreanSentence());
            koreanToEnglish.put(study.getKoreanSentence(), study.getEnglishSentence());
        }

        for (final QuizQuestionDto question : response.questions()) {
            final String questionText = question.question();
            final String correctAnswer = question.choices().get(question.answerIndex());

            if (questionText.startsWith("English_")) {
                assertThat(correctAnswer)
                        .as("영어 문제의 정답은 대응하는 한국어 문장이어야 합니다: %s", questionText)
                        .isEqualTo(englishToKorean.get(questionText));
            } else if (questionText.startsWith("Korean_")) {
                assertThat(correctAnswer)
                        .as("한국어 문제의 정답은 대응하는 영어 문장이어야 합니다: %s", questionText)
                        .isEqualTo(koreanToEnglish.get(questionText));
            }
        }
    }

    /**
     * Property 4: 퀴즈 내 모든 문제가 서로 다른 소스 문장에서 출제되었는지 검증합니다.
     *
     * <p>Validates: Requirements 2.8
     *
     * <p>For any generated quiz, all questions must reference distinct source sentences —
     * no single EnglishStudy record is used as the basis for more than one question.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 4: No Duplicate Questions")
    void allQuestionsMustReferenceDistinctSourceSentences(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final List<String> questionTexts = response.questions().stream()
                .map(QuizQuestionDto::question)
                .toList();

        final Set<String> uniqueQuestionTexts = new HashSet<>(questionTexts);

        assertThat(uniqueQuestionTexts)
                .as("퀴즈 내 모든 문제는 서로 다른 소스 문장에서 출제되어야 합니다 (중복 없음)")
                .hasSize(questionTexts.size());
    }

    /**
     * Property 5: 모든 보기가 해당 문제의 에피소드 문장들에서만 구성되었는지 검증합니다.
     *
     * <p>Validates: Requirements 3.1
     *
     * <p>For any generated quiz question, all choices must be sentences from the same episode
     * as the question's source sentence, and they must be in the answer's language direction
     * (Korean if direction is ENGLISH_TO_KOREAN, English if KOREAN_TO_ENGLISH).
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 5: Choices From Same Episode")
    void allChoicesMustBeSentencesFromSameEpisodeInCorrectDirection(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final Map<Long, List<EnglishStudy>> episodeMap = studies.stream()
                .collect(Collectors.groupingBy(EnglishStudy::getEpisode));

        for (final QuizQuestionDto question : response.questions()) {
            final String questionText = question.question();

            final boolean isEnglishToKorean = questionText.startsWith("English_");

            final Long sourceEpisode = studies.stream()
                    .filter(s -> s.getEnglishSentence().equals(questionText)
                            || s.getKoreanSentence().equals(questionText))
                    .map(EnglishStudy::getEpisode)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "문제 텍스트가 데이터셋에 존재하지 않습니다: " + questionText));

            final Set<String> validChoices = episodeMap.get(sourceEpisode).stream()
                    .map(s -> isEnglishToKorean ? s.getKoreanSentence() : s.getEnglishSentence())
                    .collect(Collectors.toSet());

            for (final String choice : question.choices()) {
                assertThat(validChoices)
                        .as("보기 '%s'는 에피소드 %d의 %s 문장이어야 합니다",
                                choice, sourceEpisode,
                                isEnglishToKorean ? "한국어" : "영어")
                        .contains(choice);
            }
        }
    }

    /**
     * Property 6: 보기 수가 min(4, 에피소드 문장 수)와 동일하고 최소 2개 이상인지 검증합니다.
     *
     * <p>Validates: Requirements 3.2, 3.3
     *
     * <p>For any generated quiz question, the number of choices must equal
     * {@code min(4, sentenceCountInEpisode)} and must be at least 2.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 6: Choice Count Invariant")
    void choiceCountMustEqualMinOfFourAndEpisodeSentenceCount(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final Map<Long, List<EnglishStudy>> episodeMap = studies.stream()
                .collect(Collectors.groupingBy(EnglishStudy::getEpisode));

        for (final QuizQuestionDto question : response.questions()) {
            final String questionText = question.question();

            final Long sourceEpisode = studies.stream()
                    .filter(s -> s.getEnglishSentence().equals(questionText)
                            || s.getKoreanSentence().equals(questionText))
                    .map(EnglishStudy::getEpisode)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "문제 텍스트가 데이터셋에 존재하지 않습니다: " + questionText));

            final int episodeSentenceCount = episodeMap.get(sourceEpisode).size();
            final int expectedChoiceCount = Math.min(4, episodeSentenceCount);

            assertThat(question.choices())
                    .as("보기 수는 min(4, %d) = %d 이어야 합니다 (에피소드 %d)",
                            episodeSentenceCount, expectedChoiceCount, sourceEpisode)
                    .hasSize(expectedChoiceCount);

            assertThat(question.choices().size())
                    .as("보기 수는 최소 2개 이상이어야 합니다 (에피소드 %d)", sourceEpisode)
                    .isGreaterThanOrEqualTo(2);
        }
    }

    /**
     * Property 7: 정답이 choices 목록의 answerIndex 위치에 존재하며, answerIndex가 유효 범위인지 검증합니다.
     *
     * <p>Validates: Requirements 3.4
     *
     * <p>For any generated quiz question, the correct answer text must appear in the choices
     * list at the position specified by {@code answerIndex}, and {@code answerIndex} must be
     * a valid index (0 ≤ answerIndex &lt; choices.size()).
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 7: Answer Inclusion")
    void correctAnswerMustAppearAtAnswerIndexPosition(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        final Map<String, String> englishToKorean = new HashMap<>();
        final Map<String, String> koreanToEnglish = new HashMap<>();
        for (final EnglishStudy study : studies) {
            englishToKorean.put(study.getEnglishSentence(), study.getKoreanSentence());
            koreanToEnglish.put(study.getKoreanSentence(), study.getEnglishSentence());
        }

        for (final QuizQuestionDto question : response.questions()) {
            final int answerIndex = question.answerIndex();
            final List<String> choices = question.choices();

            assertThat(answerIndex)
                    .as("answerIndex는 0 이상이어야 합니다")
                    .isGreaterThanOrEqualTo(0);

            assertThat(answerIndex)
                    .as("answerIndex는 choices 크기(%d) 미만이어야 합니다", choices.size())
                    .isLessThan(choices.size());

            final String questionText = question.question();
            final String expectedAnswer;
            if (questionText.startsWith("English_")) {
                expectedAnswer = englishToKorean.get(questionText);
            } else {
                expectedAnswer = koreanToEnglish.get(questionText);
            }

            assertThat(choices.get(answerIndex))
                    .as("choices[%d]에 정답이 위치해야 합니다 (문제: %s)", answerIndex, questionText)
                    .isEqualTo(expectedAnswer);
        }
    }

    /**
     * Property 8: 각 문제의 choices 목록에 중복 문자열이 없는지 검증합니다.
     *
     * <p>Validates: Requirements 3.5
     *
     * <p>For any generated quiz question, the choices list must contain no duplicate strings —
     * every choice must be unique within its question.
     *
     * @param studies 랜덤 생성된 EnglishStudy 데이터셋
     */
    @Property(tries = 20)
    @Tag("Property 8: No Duplicate Choices")
    void choicesMustNotContainDuplicateStrings(
            @ForAll("englishStudyDatasets") final List<EnglishStudy> studies) {

        final EnglishStudyRepository mockRepository = mock(EnglishStudyRepository.class);
        when(mockRepository.findAll()).thenReturn(studies);

        final QuizService quizService = new QuizService(mockRepository, new Random(42));
        final QuizResponse response = quizService.generateQuiz();

        for (final QuizQuestionDto question : response.questions()) {
            final List<String> choices = question.choices();
            final Set<String> uniqueChoices = new HashSet<>(choices);

            assertThat(uniqueChoices)
                    .as("보기 목록에 중복 문자열이 없어야 합니다 (문제: %s)", question.question())
                    .hasSize(choices.size());
        }
    }

    /**
     * 다양한 에피소드/문장 수 조합의 EnglishStudy 데이터셋 Arbitrary를 생성합니다.
     *
     * <p>에피소드 수는 0~5개, 각 에피소드의 문장 수는 1~6개로 제한하여
     * 적격(2개 이상)과 비적격(1개) 에피소드가 혼합된 데이터셋을 생성합니다.
     *
     * @return EnglishStudy 리스트의 Arbitrary
     */
    @Provide
    Arbitrary<List<EnglishStudy>> englishStudyDatasets() {
        return Arbitraries.integers().between(0, 5).flatMap(episodeCount ->
                Arbitraries.integers().between(1, 6)
                        .list().ofSize(episodeCount)
                        .map(sentenceCounts -> {
                            final List<EnglishStudy> studies = new ArrayList<>();
                            long idCounter = 1L;
                            for (int ep = 0; ep < episodeCount; ep++) {
                                final long episode = ep + 1L;
                                final int sentenceCount = sentenceCounts.get(ep);
                                for (int s = 0; s < sentenceCount; s++) {
                                    final EnglishStudy study = new EnglishStudy();
                                    study.setId(idCounter);
                                    study.setEpisode(episode);
                                    study.setEnglishSentence("English_ep" + episode + "_s" + (s + 1));
                                    study.setKoreanSentence("Korean_ep" + episode + "_s" + (s + 1));
                                    idCounter++;
                                }
                            }
                            return studies;
                        })
        );
    }
}
