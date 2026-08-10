package com.myapps.web.myrpg.application.service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.Monster;

/**
 * 필드 진입 시 몬스터 선공 판정 서비스.
 *
 * <p>플레이어가 필드 노드로 이동할 때, 해당 노드에 배치된 몬스터 중
 * 하나가 선공(preemptive strike)을 걸 확률을 판정한다.
 * 선공 확률은 전 맵 고정 5%이며, 발동 시 배치 몬스터 중 균등 무작위로 1마리를 선택한다.
 */
@Service
public class MonsterEncounterService {

    private static final int PREEMPTIVE_STRIKE_PERCENT = 5;
    private static final int PERCENT_BOUND = 100;

    private final Random random;

    /**
     * MonsterEncounterService를 생성합니다.
     *
     * @param random 판정용 Random (테스트 시 시드 고정 가능)
     */
    public MonsterEncounterService(final Random random) {
        this.random = random;
    }

    /**
     * 주어진 roll 값이 선공 발동 조건을 만족하는지 판정한다.
     *
     * <p>순수 함수로, {@code roll < 5} 이면 발동(true)을 반환한다.
     * roll은 [0, 99] 범위를 기대한다.
     *
     * @param roll 0 이상 99 이하의 정수
     * @return roll이 선공 확률(5%) 미만이면 true
     */
    public boolean triggers(final int roll) {
        return roll < PREEMPTIVE_STRIKE_PERCENT;
    }

    /**
     * 노드 배치 몬스터 목록에 대해 선공 판정을 수행하고, 발동 시 대상 몬스터를 반환한다.
     *
     * <p>이 메서드는 선공 판정과 대상 몬스터 선택만 수행한다.
     * 실제 전투 진입(POST /battle/start)과 선공 턴 처리(몬스터가 먼저 공격)는
     * 6순위(전투 시스템) 구현에서 이 반환값을 소비하여 처리한다.
     * 현재는 클라이언트가 alert('몬스터 선공 발동')을 표시하는 신호로만 사용되며,
     * 6순위에서 alert를 전투 진입 POST로 교체한다.
     *
     * @param monsters 현재 노드에 배치된 몬스터 목록 (null 또는 빈 목록 가능)
     * @return 선공 발동 시 선택된 몬스터, 미발동이거나 몬스터가 없으면 빈 Optional
     */
    public Optional<Monster> rollPreemptiveStrike(final List<Monster> monsters) {
        if (monsters == null || monsters.isEmpty()) {
            return Optional.empty();
        }
        if (!triggers(random.nextInt(PERCENT_BOUND))) {
            return Optional.empty();
        }
        return Optional.of(monsters.get(random.nextInt(monsters.size())));
    }
}
