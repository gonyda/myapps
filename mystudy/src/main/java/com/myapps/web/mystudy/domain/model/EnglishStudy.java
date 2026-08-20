package com.myapps.web.mystudy.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 영어 학습 데이터를 표현하는 JPA 엔티티.
 *
 * <p>회차별 한국어 문장과 영어 문장 쌍을 저장하며, Oracle Cloud DB의 ENGLISH_STUDY 테이블에 매핑됩니다.
 */
@Entity
public class EnglishStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long episode;

    private String koreanSentence;

    private String englishSentence;

    /** JPA 엔티티 hydration을 위한 기본 생성자. */
    public EnglishStudy() {}

    /**
     * 엔티티의 고유 식별자를 반환합니다.
     *
     * @return 엔티티 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 엔티티의 고유 식별자를 설정합니다.
     *
     * @param id 엔티티 ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 학습 회차 번호를 반환합니다.
     *
     * @return 회차 번호
     */
    public Long getEpisode() {
        return episode;
    }

    /**
     * 학습 회차 번호를 설정합니다.
     *
     * @param episode 회차 번호
     */
    public void setEpisode(final Long episode) {
        this.episode = episode;
    }

    /**
     * 한국어 문장을 반환합니다.
     *
     * @return 한국어 문장
     */
    public String getKoreanSentence() {
        return koreanSentence;
    }

    /**
     * 한국어 문장을 설정합니다.
     *
     * @param koreanSentence 한국어 문장
     */
    public void setKoreanSentence(final String koreanSentence) {
        this.koreanSentence = koreanSentence;
    }

    /**
     * 영어 문장을 반환합니다.
     *
     * @return 영어 문장
     */
    public String getEnglishSentence() {
        return englishSentence;
    }

    /**
     * 영어 문장을 설정합니다.
     *
     * @param englishSentence 영어 문장
     */
    public void setEnglishSentence(final String englishSentence) {
        this.englishSentence = englishSentence;
    }
}
