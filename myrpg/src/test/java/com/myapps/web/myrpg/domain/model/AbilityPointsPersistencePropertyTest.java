package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/**
 * abilityPoints·talent 포함 CharacterProgress 모든 필드의 영속 라운드트립 프로퍼티 테스트.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 15: 진행상황 영속 라운드트립
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 13.4</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AbilityPointsPersistencePropertyTest {

    private static final int NICKNAME_MIN_LENGTH = 1;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 100;
    private static final int ACCUMULATED_LEVEL_EXTRA_MAX = 200;
    private static final long EXPERIENCE_MAX = 100_000L;
    private static final int VITAL_MAX = 1000;
    private static final int NODE_ID_MAX = 99;
    private static final int ABILITY_POINTS_MAX = 300;

    private final TestEntityManager entityManager;

    AbilityPointsPersistencePropertyTest(final TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Feature: 004-talent-and-ability-points, Property 15: 진행상황 영속 라운드트립

    /**
     * abilityPoints·talent 포함 모든 필드를 저장 후 조회하면 값이 보존되는지 검증한다.
     *
     * <p>lastRebirthAt이 null인 경우를 포함하여, abilityPoints가 0~300 범위의 임의 값이고 talent가
     * 3종(MELEE/ARCHERY/MAGIC) 중 임의 값일 때 영속 라운드트립이 정상 동작하는지 확인한다.
     *
     * @param nickname 임의 닉네임 (1~10자)
     * @param currentLevel 현재 레벨 (1~100)
     * @param levelExtra 누적레벨 추가분 (0~200)
     * @param experience 경험치 (0~100,000)
     * @param talent 재능 유형 (3종 중 하나)
     * @param hpCurrent HP 현재값 (0~1000)
     * @param mpCurrent MP 현재값 (0~1000)
     * @param staminaCurrent Stamina 현재값 (0~1000)
     * @param nodeIndex 노드 인덱스 (1~99)
     * @param abilityPoints 보유 AP (0~300)
     */
    @Property(tries = 100)
    void should_preserveAbilityPointsAndTalent_when_savedAndLoaded(
            @ForAll("nicknames") final String nickname,
            @ForAll("currentLevels") final int currentLevel,
            @ForAll("levelExtras") final int levelExtra,
            @ForAll("experiences") final long experience,
            @ForAll("talents") final TalentType talent,
            @ForAll("vitals") final int hpCurrent,
            @ForAll("vitals") final int mpCurrent,
            @ForAll("vitals") final int staminaCurrent,
            @ForAll("nodeIndices") final int nodeIndex,
            @ForAll("abilityPointsValues") final int abilityPoints) {

        final int accumulatedLevel = currentLevel + levelExtra;
        final String currentNodeId = "node-" + nodeIndex;

        final CharacterProgress progress =
                new CharacterProgress(
                        nickname,
                        currentLevel,
                        accumulatedLevel,
                        experience,
                        talent,
                        null,
                        hpCurrent,
                        mpCurrent,
                        staminaCurrent,
                        currentNodeId,
                        abilityPoints,
                        0L);

        entityManager.persistAndFlush(progress);
        final Long savedId = progress.getId();
        entityManager.clear();

        final CharacterProgress found = entityManager.find(CharacterProgress.class, savedId);

        assertThat(found).isNotNull();
        assertThat(found.getAbilityPoints()).isEqualTo(abilityPoints);
        assertThat(found.getTalent()).isEqualTo(talent);
        assertThat(found.getNickname()).isEqualTo(nickname);
        assertThat(found.getCurrentLevel()).isEqualTo(currentLevel);
        assertThat(found.getAccumulatedLevel()).isEqualTo(accumulatedLevel);
        assertThat(found.getExperience()).isEqualTo(experience);
        assertThat(found.getLastRebirthAt()).isNull();
        assertThat(found.getHpCurrent()).isEqualTo(hpCurrent);
        assertThat(found.getMpCurrent()).isEqualTo(mpCurrent);
        assertThat(found.getStaminaCurrent()).isEqualTo(staminaCurrent);
        assertThat(found.getCurrentNodeId()).isEqualTo(currentNodeId);
    }

    /**
     * lastRebirthAt이 non-null이고 다양한 abilityPoints·talent 조합으로 저장 후 조회하면 모든 필드가 보존되는지 검증한다.
     *
     * @param nickname 임의 닉네임 (1~10자)
     * @param currentLevel 현재 레벨 (1~100)
     * @param levelExtra 누적레벨 추가분 (0~200)
     * @param experience 경험치 (0~100,000)
     * @param talent 재능 유형 (3종 중 하나)
     * @param lastRebirthAt 마지막 환생 시각
     * @param hpCurrent HP 현재값 (0~1000)
     * @param mpCurrent MP 현재값 (0~1000)
     * @param staminaCurrent Stamina 현재값 (0~1000)
     * @param nodeIndex 노드 인덱스 (1~99)
     * @param abilityPoints 보유 AP (0~300)
     */
    @Property(tries = 100)
    void should_preserveAllFieldsIncludingAbilityPoints_when_rebirthAtIsNonNull(
            @ForAll("nicknames") final String nickname,
            @ForAll("currentLevels") final int currentLevel,
            @ForAll("levelExtras") final int levelExtra,
            @ForAll("experiences") final long experience,
            @ForAll("talents") final TalentType talent,
            @ForAll("rebirthTimestamps") final LocalDateTime lastRebirthAt,
            @ForAll("vitals") final int hpCurrent,
            @ForAll("vitals") final int mpCurrent,
            @ForAll("vitals") final int staminaCurrent,
            @ForAll("nodeIndices") final int nodeIndex,
            @ForAll("abilityPointsValues") final int abilityPoints) {

        final int accumulatedLevel = currentLevel + levelExtra;
        final String currentNodeId = "node-" + nodeIndex;

        final CharacterProgress progress =
                new CharacterProgress(
                        nickname,
                        currentLevel,
                        accumulatedLevel,
                        experience,
                        talent,
                        lastRebirthAt,
                        hpCurrent,
                        mpCurrent,
                        staminaCurrent,
                        currentNodeId,
                        abilityPoints,
                        0L);

        entityManager.persistAndFlush(progress);
        final Long savedId = progress.getId();
        entityManager.clear();

        final CharacterProgress found = entityManager.find(CharacterProgress.class, savedId);

        assertThat(found).isNotNull();
        assertThat(found.getAbilityPoints()).isEqualTo(abilityPoints);
        assertThat(found.getTalent()).isEqualTo(talent);
        assertThat(found.getNickname()).isEqualTo(nickname);
        assertThat(found.getCurrentLevel()).isEqualTo(currentLevel);
        assertThat(found.getAccumulatedLevel()).isEqualTo(accumulatedLevel);
        assertThat(found.getExperience()).isEqualTo(experience);
        assertThat(found.getLastRebirthAt()).isEqualTo(lastRebirthAt);
        assertThat(found.getHpCurrent()).isEqualTo(hpCurrent);
        assertThat(found.getMpCurrent()).isEqualTo(mpCurrent);
        assertThat(found.getStaminaCurrent()).isEqualTo(staminaCurrent);
        assertThat(found.getCurrentNodeId()).isEqualTo(currentNodeId);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 닉네임 Arbitrary를 제공한다 (1~10자, 알파벳).
     *
     * @return 닉네임 Arbitrary
     */
    @Provide
    Arbitrary<String> nicknames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(NICKNAME_MIN_LENGTH)
                .ofMaxLength(NICKNAME_MAX_LENGTH);
    }

    /**
     * 현재 레벨 Arbitrary를 제공한다 (1~100).
     *
     * @return 현재 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> currentLevels() {
        return Arbitraries.integers().between(LEVEL_MIN, LEVEL_MAX);
    }

    /**
     * 누적레벨 추가분 Arbitrary를 제공한다 (0~200).
     *
     * @return 누적레벨 추가분 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levelExtras() {
        return Arbitraries.integers().between(0, ACCUMULATED_LEVEL_EXTRA_MAX);
    }

    /**
     * 경험치 Arbitrary를 제공한다 (0~100,000).
     *
     * @return 경험치 Arbitrary
     */
    @Provide
    Arbitrary<Long> experiences() {
        return Arbitraries.longs().between(0L, EXPERIENCE_MAX);
    }

    /**
     * 재능 유형 Arbitrary를 제공한다 (MELEE, ARCHERY, MAGIC 중 하나).
     *
     * @return 재능 유형 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 바이탈(HP/MP/Stamina) 현재값 Arbitrary를 제공한다 (0~1000).
     *
     * @return 바이탈 현재값 Arbitrary
     */
    @Provide
    Arbitrary<Integer> vitals() {
        return Arbitraries.integers().between(0, VITAL_MAX);
    }

    /**
     * 노드 인덱스 Arbitrary를 제공한다 (1~99).
     *
     * @return 노드 인덱스 Arbitrary
     */
    @Provide
    Arbitrary<Integer> nodeIndices() {
        return Arbitraries.integers().between(1, NODE_ID_MAX);
    }

    /**
     * 보유 AP Arbitrary를 제공한다 (0~300).
     *
     * @return AP Arbitrary
     */
    @Provide
    Arbitrary<Integer> abilityPointsValues() {
        return Arbitraries.integers().between(0, ABILITY_POINTS_MAX);
    }

    /**
     * 환생 시각 Arbitrary를 제공한다 (non-null, 최근 30일 이내).
     *
     * @return 환생 시각 Arbitrary
     */
    @Provide
    Arbitrary<LocalDateTime> rebirthTimestamps() {
        final LocalDateTime now = LocalDateTime.now();
        return Arbitraries.longs()
                .between(0L, 30L * 24 * 60)
                .map(minutesAgo -> now.minusMinutes(minutesAgo));
    }
}
