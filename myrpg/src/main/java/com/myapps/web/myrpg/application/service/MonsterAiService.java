package com.myapps.web.myrpg.application.service;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 몬스터 가위바위보 AI 서비스.
 *
 * <p>몬스터의 전투 행동을 확률적으로 결정한다.
 * 일반(NORMAL) 34%, 강(HEAVY) 33%, 방어(DEFENSE) 33%의 고정 분포를 사용하며,
 * 몬스터별 개별 가중치 override는 없다.
 */
@Service
public class MonsterAiService {

    private static final int NORMAL_WEIGHT = 34;
    private static final int HEAVY_WEIGHT = 33;
    private static final int DEFENSE_WEIGHT = 33;
    private static final int PERCENT_BOUND = 100;

    private final Random random;

    /**
     * MonsterAiService를 생성합니다.
     *
     * @param random 무작위 행동 결정용 Random (테스트 시 시드 고정 가능)
     */
    public MonsterAiService(final Random random) {
        this.random = random;
    }

    /**
     * 주어진 roll 값으로부터 몬스터 행동 타입을 결정하는 순수 함수.
     *
     * <p>분포 규칙:
     * <ul>
     *   <li>0 ~ 33 (34개): {@link SkillType#NORMAL}</li>
     *   <li>34 ~ 66 (33개): {@link SkillType#HEAVY}</li>
     *   <li>67 ~ 99 (33개): {@link SkillType#DEFENSE}</li>
     * </ul>
     *
     * @param roll 0 이상 100 미만의 정수
     * @return 결정된 스킬 타입
     */
    public SkillType actionFor(final int roll) {
        if (roll < NORMAL_WEIGHT) {
            return SkillType.NORMAL;
        }
        if (roll < NORMAL_WEIGHT + HEAVY_WEIGHT) {
            return SkillType.HEAVY;
        }
        return SkillType.DEFENSE;
    }

    /**
     * 무작위로 몬스터의 다음 행동을 결정하여 반환한다.
     *
     * <p>이 메서드의 반환값은 6순위(전투 턴) 구현에서 실제 데미지 계산에 소비된다.
     * 6순위에서 전투 상태 머신이 이 메서드를 호출하여 몬스터 행동을 결정하고,
     * 행동별 배율과 플레이어 방어를 조합하여 최종 데미지를 산출한다.
     *
     * @return 무작위로 선택된 스킬 타입 (NORMAL 34%, HEAVY 33%, DEFENSE 33%)
     */
    public SkillType nextAction() {
        return actionFor(random.nextInt(PERCENT_BOUND));
    }
}
