package com.myapps.web.mystudy.interfaces.api;

import com.myapps.web.mystudy.application.dto.QuizResponse;
import com.myapps.web.mystudy.application.service.EnglishStudyService;
import com.myapps.web.mystudy.application.service.QuizService;
import com.myapps.web.mystudy.domain.model.EnglishStudy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 영어 학습 데이터의 웹 진입점을 제공하는 컨트롤러.
 *
 * <p>Thymeleaf 기반 HTML 뷰 렌더링과 REST API(JSON) 엔드포인트를 모두 제공합니다.
 */
@Controller
public class EnglishStudyController {

    private final EnglishStudyService englishStudyService;
    private final QuizService quizService;

    /**
     * EnglishStudyController를 생성합니다.
     *
     * @param englishStudyService 영어 학습 데이터 비즈니스 로직 서비스
     * @param quizService         퀴즈 생성 서비스
     */
    public EnglishStudyController(final EnglishStudyService englishStudyService,
                                  final QuizService quizService) {
        this.englishStudyService = englishStudyService;
        this.quizService = quizService;
    }

    /**
     * 영어 학습 페이지를 렌더링합니다.
     *
     * <p>전체 학습 데이터를 ID 역순으로 조회하여 모델에 추가한 후 Thymeleaf 뷰를 반환합니다.
     *
     * @param model Thymeleaf 뷰에 전달할 모델 객체
     * @return Thymeleaf 뷰 이름
     */
    @GetMapping("/english-study")
    public String englishStudy(final Model model) {
        final List<EnglishStudy> englishStudies = englishStudyService.findAllOrderByIdDesc();
        model.addAttribute("englishStudies", englishStudies);
        return "english_study";
    }

    /**
     * 모든 영어 학습 데이터를 JSON 형식으로 반환합니다.
     *
     * @return ID 내림차순으로 정렬된 영어 학습 데이터 목록
     */
    @GetMapping("/api/english-study")
    @ResponseBody
    public List<EnglishStudy> getAllEnglishStudies() {
        return englishStudyService.findAllOrderByIdDesc();
    }

    /**
     * 새로운 영어 학습 데이터를 저장합니다.
     *
     * @param englishStudy 저장할 영어 학습 데이터
     * @return 저장된 영어 학습 엔티티와 201 Created 상태 코드
     */
    @PostMapping("/api/english-study")
    @ResponseBody
    public ResponseEntity<EnglishStudy> addEnglishStudy(@RequestBody final EnglishStudy englishStudy) {
        final EnglishStudy savedStudy = englishStudyService.save(englishStudy);
        return new ResponseEntity<>(savedStudy, HttpStatus.CREATED);
    }

    /**
     * 랜덤 퀴즈를 생성하여 JSON 형식으로 반환합니다.
     *
     * <p>매 호출 시 EnglishStudy 데이터를 기반으로 랜덤한 객관식 퀴즈를 생성합니다.
     * 데이터가 없거나 퀴즈 생성에 실패하면 빈 문제 목록을 반환합니다.
     *
     * @return 퀴즈 문제 목록을 포함한 응답 DTO
     */
    @GetMapping("/api/english-study/quiz")
    @ResponseBody
    public QuizResponse getQuiz() {
        return quizService.generateQuiz();
    }
}
