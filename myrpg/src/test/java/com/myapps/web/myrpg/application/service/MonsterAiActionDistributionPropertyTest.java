package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 몬스터 가위바위보 AI의 행동 분포 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>{@link MonsterAiService#actionFor(int)} 순수 함수가 0~99 입력에 대해 정확히 34/33/33
 * 분포(NORMAL/HEAVY/DEFENSE)를 만족하며, 경계값(33/34/66/67)이 규칙과 일치함을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 7: 가위바위보 분포
 *
 * <p><b>Validates: Requirements 7.2, 7.3</b>
 */
class MonsterAiActionDistributionPropertyTest {

    private static final int NORMAL_COUNT = 34;
    private static final int HEAVY_COUNT = 33;
    private static final int DEFENSE_COUNT = 33;
    private static final int TOTAL_RANGE = 100;

    private final MonsterAiService service = new MonsterAiService(new Random(0));

    /**
     * 0~99 범위의 임의 roll 값에 대해 {@code actionFor}가 올바른 타입을 반환함을 검증한다.
     *
     * <p>roll &lt; 34 → NORMAL, 34 ≤ roll &lt; 67 → HEAVY, roll ≥ 67 → DEFENSE.
     *
     * @param roll 0 이상 100 미만의 임의 정수
     */
    @Property(tries = 100)
    void should_returnCorrectType_when_rollInRange(@ForAll("validRolls") final int roll) {
        final SkillType result = service.actionFor(roll);

        if (roll < NORMAL_COUNT) {
            assertThat(result).as("roll=%d should map to NORMAL", roll).isEqualTo(SkillType.NORMAL);
        } else if (roll < NORMAL_COUNT + HEAVY_COUNT) {
            assertThat(result).as("roll=%d should map to HEAVY", roll).isEqualTo(SkillType.HEAVY);
        } else {
            assertThat(result)
                    .as("roll=%d should map to DEFENSE", roll)
                    .isEqualTo(SkillType.DEFENSE);
        }
    }

    /**
     * 경계값(33/34/66/67)이 정확한 타입에 매핑됨을 검증한다.
     *
     * <p>33 → NORMAL(마지막), 34 → HEAVY(첫), 66 → HEAVY(마지막), 67 → DEFENSE(첫).
     */
    @Property(tries = 100)
    void should_mapBoundaryCorrectly_when_boundaryRolls() {
        assertThat(service.actionFor(33)).as("roll=33 is last NORMAL").isEqualTo(SkillType.NORMAL);
        assertThat(service.actionFor(34)).as("roll=34 is first HEAVY").isEqualTo(SkillType.HEAVY);
        assertThat(service.actionFor(66)).as("roll=66 is last HEAVY").isEqualTo(SkillType.HEAVY);
        assertThat(service.actionFor(67))
                .as("roll=67 is first DEFENSE")
                .isEqualTo(SkillType.DEFENSE);
    }

    /**
     * 0~99 전체 범위를 순회하여 각 타입의 개수가 정확히 34/33/33임을 검증한다.
     *
     * <p>분포 합산: NORMAL 34개(0~33), HEAVY 33개(34~66), DEFENSE 33개(67~99).
     */
    @Property(tries = 100)
    void should_produce34_33_33Distribution_when_allRollsIterated() {
        final Map<SkillType, Integer> counts = new EnumMap<>(SkillType.class);
        for (final SkillType type : SkillType.values()) {
            counts.put(type, 0);
        }

        for (int roll = 0; roll < TOTAL_RANGE; roll++) {
            final SkillType result = service.actionFor(roll);
            counts.merge(result, 1, Integer::sum);
        }

        assertThat(counts.get(SkillType.NORMAL))
                .as("NORMAL count should be exactly 34")
                .isEqualTo(NORMAL_COUNT);
        assertThat(counts.get(SkillType.HEAVY))
                .as("HEAVY count should be exactly 33")
                .isEqualTo(HEAVY_COUNT);
        assertThat(counts.get(SkillType.DEFENSE))
                .as("DEFENSE count should be exactly 33")
                .isEqualTo(DEFENSE_COUNT);
    }

    /**
     * 0 이상 100 미만의 정수를 균등하게 생성하는 Arbitrary 제공자.
     *
     * @return 유효 roll 범위(0~99)의 Arbitrary
     */
    @Provide
    Arbitrary<Integer> validRolls() {
        return Arbitraries.integers().between(0, TOTAL_RANGE - 1);
    }
}
