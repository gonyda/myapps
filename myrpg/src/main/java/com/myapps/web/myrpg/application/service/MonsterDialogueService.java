package com.myapps.web.myrpg.application.service;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.Monster;

/**
 * 몬스터 조우 대사 선택 서비스.
 *
 * <p>몬스터의 {@code lines} 목록(정확히 3개)에서 균등 무작위로
 * 대사 1개를 선택합니다. 시간대(TimeOfDay) 분기 없이 단순 랜덤 선택이며,
 * 카탈로그 검증에서 {@code lines}가 항상 3개임을 보장하므로 폴백 문구를 사용하지 않습니다.
 */
@Service
public class MonsterDialogueService {

    private static final int LINES_COUNT = 3;

    private final Random random;

    /**
     * MonsterDialogueService를 생성합니다.
     *
     * @param random 무작위 선택용 Random (테스트 시 시드 고정 가능)
     */
    public MonsterDialogueService(final Random random) {
        this.random = random;
    }

    /**
     * 몬스터의 조우 대사 3개 중 1개를 균등 무작위로 선택하여 반환합니다.
     *
     * <p>{@link Monster#lines()}는 카탈로그 로드 시 정확히 3개로 검증되므로,
     * 인덱스 범위 초과나 빈 목록 상황은 발생하지 않습니다.
     *
     * @param monster 대사를 선택할 몬스터
     * @return 선택된 대사 문자열 (비어 있지 않음)
     */
    public String selectLine(final Monster monster) {
        return monster.lines().get(random.nextInt(LINES_COUNT));
    }
}
