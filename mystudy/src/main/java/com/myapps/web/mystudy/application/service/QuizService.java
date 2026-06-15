package com.myapps.web.mystudy.application.service;

import com.myapps.web.mystudy.application.dto.QuizQuestionDto;
import com.myapps.web.mystudy.application.dto.QuizResponse;
import com.myapps.web.mystudy.domain.model.EnglishStudy;
import com.myapps.web.mystudy.domain.model.QuestionType;
import com.myapps.web.mystudy.domain.repository.EnglishStudyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 영어 학습 퀴즈 생성 서비스.
 *
 * <p>EnglishStudy 데이터를 기반으로 랜덤 객관식 퀴즈를 생성합니다.
 * 적격 에피소드(문장 2개 이상)에서 랜덤으로 문제를 선택하고,
 * 동일 에피소드 내 문장으로 객관식 보기를 구성합니다.
 */
@Service
public class QuizService {

    private static final int QUIZ_QUESTION_COUNT = 10;
    private static final int CHOICE_COUNT = 4;
    private static final int MIN_SENTENCES_PER_EPISODE = 2;

    private final EnglishStudyRepository englishStudyRepository;
    private final Random random;

    /**
     * QuizService를 생성합니다.
     *
     * <p>Spring DI에서 단일 생성자로 자동 주입됩니다.
     * 프로덕션에서는 {@code QuizConfig}에서 등록한 Random Bean이 주입되고,
     * 테스트에서는 시드가 고정된 Random을 직접 전달하여 결정적 테스트가 가능합니다.
     *
     * @param englishStudyRepository 영어 학습 데이터 저장소
     * @param random                 랜덤 객체
     */
    public QuizService(final EnglishStudyRepository englishStudyRepository, final Random random) {
        this.englishStudyRepository = englishStudyRepository;
        this.random = random;
    }

    /**
     * 랜덤 퀴즈를 생성합니다.
     *
     * <p>적격 에피소드(문장 2개 이상)에서 랜덤으로 문제를 선택하고,
     * 동일 에피소드 내 문장으로 객관식 보기를 구성합니다.
     *
     * @return 퀴즈 응답 DTO (문제 목록 포함)
     */
    public QuizResponse generateQuiz() {
        final Map<Long, List<EnglishStudy>> eligibleEpisodes = findEligibleEpisodes();

        if (eligibleEpisodes.isEmpty()) {
            return new QuizResponse(List.of());
        }

        final List<EnglishStudy> selectedSentences = selectRandomSentences(eligibleEpisodes);

        final List<QuizQuestionDto> questions = selectedSentences.stream()
                .map(sentence -> createQuestion(sentence, eligibleEpisodes))
                .toList();

        return new QuizResponse(questions);
    }

    /**
     * 적격 에피소드(문장 2개 이상)를 필터링하여 반환합니다.
     *
     * @return 에피소드 번호를 키로, 해당 에피소드 문장 목록을 값으로 갖는 Map
     */
    private Map<Long, List<EnglishStudy>> findEligibleEpisodes() {
        final List<EnglishStudy> allStudies = englishStudyRepository.findAll();

        return allStudies.stream()
                .collect(Collectors.groupingBy(EnglishStudy::getEpisode))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= MIN_SENTENCES_PER_EPISODE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 적격 에피소드에서 랜덤으로 문제 출제용 문장을 선택합니다.
     *
     * @param eligibleEpisodes 적격 에피소드 Map
     * @return 최대 10개의 랜덤 선택된 문장 목록
     */
    private List<EnglishStudy> selectRandomSentences(final Map<Long, List<EnglishStudy>> eligibleEpisodes) {
        final List<EnglishStudy> candidateSentences = new ArrayList<>(
                eligibleEpisodes.values().stream()
                        .flatMap(List::stream)
                        .toList()
        );

        Collections.shuffle(candidateSentences, random);

        final int questionCount = Math.min(QUIZ_QUESTION_COUNT, candidateSentences.size());
        return candidateSentences.subList(0, questionCount);
    }

    /**
     * 하나의 문장으로부터 퀴즈 문제를 생성합니다.
     *
     * @param sentence         문제로 출제할 문장
     * @param eligibleEpisodes 적격 에피소드 Map (보기 구성용)
     * @return 퀴즈 문제 DTO
     */
    private QuizQuestionDto createQuestion(final EnglishStudy sentence,
                                           final Map<Long, List<EnglishStudy>> eligibleEpisodes) {
        final QuestionType questionType = random.nextBoolean()
                ? QuestionType.ENGLISH_TO_KOREAN
                : QuestionType.KOREAN_TO_ENGLISH;

        final String questionText = (questionType == QuestionType.ENGLISH_TO_KOREAN)
                ? sentence.getEnglishSentence()
                : sentence.getKoreanSentence();

        final String correctAnswer = (questionType == QuestionType.ENGLISH_TO_KOREAN)
                ? sentence.getKoreanSentence()
                : sentence.getEnglishSentence();

        final List<String> choices = buildChoices(sentence, questionType, correctAnswer, eligibleEpisodes);
        final int answerIndex = choices.indexOf(correctAnswer);

        return new QuizQuestionDto(questionText, choices, answerIndex);
    }

    /**
     * 정답과 오답 보기를 조합하여 셔플된 보기 목록을 생성합니다.
     *
     * @param sentence         문제 출제 문장
     * @param questionType     출제 방향
     * @param correctAnswer    정답 텍스트
     * @param eligibleEpisodes 적격 에피소드 Map
     * @return 셔플된 보기 목록
     */
    private List<String> buildChoices(final EnglishStudy sentence,
                                      final QuestionType questionType,
                                      final String correctAnswer,
                                      final Map<Long, List<EnglishStudy>> eligibleEpisodes) {
        final List<EnglishStudy> episodeSentences = eligibleEpisodes.get(sentence.getEpisode());

        final List<String> wrongChoices = episodeSentences.stream()
                .filter(s -> !s.getId().equals(sentence.getId()))
                .map(s -> questionType == QuestionType.ENGLISH_TO_KOREAN
                        ? s.getKoreanSentence()
                        : s.getEnglishSentence())
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(wrongChoices, random);

        final int wrongChoiceCount = Math.min(CHOICE_COUNT - 1, wrongChoices.size());
        final List<String> choices = new ArrayList<>(wrongChoices.subList(0, wrongChoiceCount));
        choices.add(correctAnswer);

        Collections.shuffle(choices, random);
        return choices;
    }
}
