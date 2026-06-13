package com.myapps.web.mystudy.application.service;

import com.myapps.web.mystudy.domain.model.EnglishStudy;
import com.myapps.web.mystudy.domain.repository.EnglishStudyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 영어 학습 데이터의 비즈니스 로직을 처리하는 서비스 클래스.
 *
 * <p>EnglishStudyRepository를 통해 영어 학습 데이터의 조회 및 저장 기능을 제공합니다.
 */
@Service
public class EnglishStudyService {

    private final EnglishStudyRepository englishStudyRepository;

    /**
     * EnglishStudyService를 생성합니다.
     *
     * @param englishStudyRepository 영어 학습 데이터 저장소
     */
    public EnglishStudyService(final EnglishStudyRepository englishStudyRepository) {
        this.englishStudyRepository = englishStudyRepository;
    }

    /**
     * 모든 영어 학습 데이터를 ID 역순으로 조회합니다.
     *
     * @return ID 내림차순으로 정렬된 영어 학습 데이터 목록
     */
    public List<EnglishStudy> findAllOrderByIdDesc() {
        return englishStudyRepository.findAllByOrderByIdDesc();
    }

    /**
     * 영어 학습 데이터를 저장합니다.
     *
     * @param englishStudy 저장할 영어 학습 엔티티
     * @return 저장된 영어 학습 엔티티
     */
    public EnglishStudy save(final EnglishStudy englishStudy) {
        return englishStudyRepository.save(englishStudy);
    }
}
