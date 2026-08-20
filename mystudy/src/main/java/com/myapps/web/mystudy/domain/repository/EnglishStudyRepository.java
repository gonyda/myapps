package com.myapps.web.mystudy.domain.repository;

import com.myapps.web.mystudy.domain.model.EnglishStudy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 영어 학습 데이터에 대한 데이터 접근 인터페이스.
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성하며, EnglishStudy 엔티티의 CRUD 및 커스텀 조회 메서드를 제공합니다.
 */
public interface EnglishStudyRepository extends JpaRepository<EnglishStudy, Long> {

    /**
     * 모든 영어 학습 데이터를 ID 내림차순으로 조회합니다.
     *
     * @return ID 내림차순으로 정렬된 영어 학습 데이터 목록
     */
    List<EnglishStudy> findAllByOrderByIdDesc();
}
