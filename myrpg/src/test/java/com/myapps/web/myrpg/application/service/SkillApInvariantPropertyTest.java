package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP 정합성 불변식(확장)을 검증하는 프로퍼티 테스트.
 *
 * <p>레벨업과 스킬 랭크업의 임의 시퀀스에 대해,
 * {@code abilityPoints == (accumulatedLevel - 1) - totalConsumedAP}가 항상 성립하는지 검증한다.
 * 여기서 {@code totalConsumedAP}는 보유 스킬 각각에 대해 F에서 현재 랭크까지의 apCost 합이다.
 *
 * <p>Feature: 005-skill-system, Property 11: AP 정합성 불변식(확장)
 *
 * <p><b>Validates: Requirements 6.6</b>
 */
class SkillApInvariantPropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String SKILL_ID = "windmill";

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * 임의의 누적 레벨과 랭크업 횟수 시퀀스에 대해,
     * 매 랭크업 후 AP 정합성 불변식이 성립하는지 검증한다.
     *
     * <p>생성 시나리오:
     * <ol>
     *   <li>accumulatedLevel을 10~200 사이로 생성</li>
     *   <li>초기 AP = accumulatedLevel - 1 (전량 사용 가능한 상태)</li>
     *   <li>랭크업 가능한 횟수까지 순차적으로 랭크업 실행</li>
     *   <li>매 랭크업 후 불변식 확인</li>
     * </ol>
     *
     * @param accumulatedLevel 누적 레벨 (10~200)
     * @param rankUpCount      실행할 랭크업 횟수 (1~15)
     */
    @Property(tries = 100)
    void should_maintain_ap_invariant_after_sequence_of_rankUps(
            @ForAll("accumulatedLevels") final int accumulatedLevel,
            @ForAll("rankUpCounts") final int rankUpCount) {

        // Given: 초기 AP = accumulatedLevel - 1
        final int initialAp = accumulatedLevel - 1;
        final CharacterProgress progress = new CharacterProgress(
                "테스트", 1, accumulatedLevel, 0L,
                TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", initialAp, 0L);
        setId(progress, CHARACTER_ID);

        // CharacterSkill: F 랭크에서 시작
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0);

        // Mock: 리포지토리
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        when(mockRepository.findByCharacterIdAndSkillId(anyLong(), anyString()))
                .thenReturn(Optional.of(skill));
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock: 카탈로그 (rankUp에서 직접 사용하지 않지만 SkillService 생성에 필요)
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        when(mockCatalog.byId(anyString()))
                .thenReturn(Optional.of(new DamageSkill(
                        SKILL_ID, "윈드밀", SkillType.NORMAL, SkillTalent.MELEE, 7,
                        Map.of(SkillRank.F, 35, SkillRank.E, 38),
                        "주변의 적을 베어 넘기는 회전 공격.")));
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalog);

        // When: 순차적으로 랭크업 수행 (최대 rankUpCount 또는 AP가 부족해질 때까지)
        int totalConsumedAp = 0;
        SkillRank currentRank = SkillRank.F;

        for (int i = 0; i < rankUpCount; i++) {
            if (currentRank.isMax()) {
                break;
            }

            final int apCost = skillRankPolicy.apCost(currentRank).orElseThrow();

            // AP가 부족하면 랭크업 중단
            if (progress.getAbilityPoints() < apCost) {
                break;
            }

            // 사용 횟수·막타 처치 조건을 충족시킴
            final var requirement = skillRankPolicy.requirement(currentRank).orElseThrow();
            skill.setUsageCount(requirement.requiredUsage());
            skill.setKillCount(requirement.requiredKills());

            // 랭크업 실행
            final boolean result = skillService.rankUp(progress, SKILL_ID);
            assertThat(result)
                    .as("랭크업 %d회차 성공 기대 (currentRank=%s)", i + 1, currentRank)
                    .isTrue();

            totalConsumedAp += apCost;
            currentRank = skill.getRank();

            // Then: 매 랭크업 후 불변식 확인
            final int expectedAp = (accumulatedLevel - 1) - totalConsumedAp;
            assertThat(progress.getAbilityPoints())
                    .as("AP 불변식 위반 (랭크업 %d회 후): AP=%d, expected=(accLv-1)-consumed=(%d-1)-%d=%d",
                            i + 1, progress.getAbilityPoints(),
                            accumulatedLevel, totalConsumedAp, expectedAp)
                    .isEqualTo(expectedAp);
        }

        // 최종 불변식 확인
        final int expectedFinalAp = (accumulatedLevel - 1) - totalConsumedAp;
        assertThat(progress.getAbilityPoints())
                .as("최종 AP 불변식: AP=%d, accLv=%d, consumed=%d",
                        progress.getAbilityPoints(), accumulatedLevel, totalConsumedAp)
                .isEqualTo(expectedFinalAp);
    }

    /**
     * 여러 스킬에 대한 랭크업 시퀀스에서도 AP 정합성 불변식이 성립하는지 검증한다.
     *
     * <p>2개의 스킬을 번갈아 랭크업하여, 전체 소모 AP 합산이 불변식을 만족하는지 확인한다.
     *
     * @param accumulatedLevel 누적 레벨 (20~200)
     * @param rankUpsPerSkill  각 스킬 랭크업 횟수 (1~7)
     */
    @Property(tries = 100)
    void should_maintain_ap_invariant_across_multiple_skills(
            @ForAll("largeAccumulatedLevels") final int accumulatedLevel,
            @ForAll("smallRankUpCounts") final int rankUpsPerSkill) {

        // Given: 초기 AP = accumulatedLevel - 1
        final int initialAp = accumulatedLevel - 1;
        final CharacterProgress progress = new CharacterProgress(
                "테스트", 1, accumulatedLevel, 0L,
                TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", initialAp, 0L);
        setId(progress, CHARACTER_ID);

        // 두 스킬: 각각 F 랭크에서 시작
        final String skillId1 = "windmill";
        final String skillId2 = "smash";
        final CharacterSkill skill1 = new CharacterSkill(CHARACTER_ID, skillId1, SkillRank.F, 0, 0);
        final CharacterSkill skill2 = new CharacterSkill(CHARACTER_ID, skillId2, SkillRank.F, 0, 0);

        // Mock: 리포지토리
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, skillId1))
                .thenReturn(Optional.of(skill1));
        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, skillId2))
                .thenReturn(Optional.of(skill2));
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        when(mockCatalog.byId(anyString()))
                .thenReturn(Optional.of(new DamageSkill(
                        "windmill", "윈드밀", SkillType.NORMAL, SkillTalent.MELEE, 7,
                        Map.of(SkillRank.F, 35, SkillRank.E, 38),
                        "주변의 적을 베어 넘기는 회전 공격.")));
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalog);

        // When: 번갈아 랭크업
        int totalConsumedAp = 0;

        for (int i = 0; i < rankUpsPerSkill; i++) {
            // 스킬1 랭크업 시도
            totalConsumedAp += attemptRankUp(skillService, progress, skill1, skillId1);

            // 중간 불변식 확인
            assertApInvariant(progress, accumulatedLevel, totalConsumedAp,
                    "스킬1 랭크업 " + (i + 1) + "회 후");

            // 스킬2 랭크업 시도
            totalConsumedAp += attemptRankUp(skillService, progress, skill2, skillId2);

            // 중간 불변식 확인
            assertApInvariant(progress, accumulatedLevel, totalConsumedAp,
                    "스킬2 랭크업 " + (i + 1) + "회 후");
        }
    }

    /**
     * 랭크업을 시도하고 소모된 AP를 반환한다.
     *
     * @param skillService 스킬 서비스
     * @param progress     캐릭터 진행상황
     * @param skill        대상 스킬 엔티티
     * @param skillId      스킬 ID
     * @return 소모된 AP (실패 시 0)
     */
    private int attemptRankUp(final SkillService skillService,
                              final CharacterProgress progress,
                              final CharacterSkill skill,
                              final String skillId) {
        if (skill.getRank().isMax()) {
            return 0;
        }

        final int apCost = skillRankPolicy.apCost(skill.getRank()).orElseThrow();
        if (progress.getAbilityPoints() < apCost) {
            return 0;
        }

        // 조건 충족
        final var requirement = skillRankPolicy.requirement(skill.getRank()).orElseThrow();
        skill.setUsageCount(requirement.requiredUsage());
        skill.setKillCount(requirement.requiredKills());

        final boolean result = skillService.rankUp(progress, skillId);
        if (result) {
            return apCost;
        }
        return 0;
    }

    /**
     * AP 정합성 불변식을 검증한다.
     *
     * @param progress         캐릭터 진행상황
     * @param accumulatedLevel 누적 레벨
     * @param totalConsumedAp  총 소모 AP
     * @param context          실패 시 맥락 설명
     */
    private void assertApInvariant(final CharacterProgress progress,
                                   final int accumulatedLevel,
                                   final int totalConsumedAp,
                                   final String context) {
        final int expectedAp = (accumulatedLevel - 1) - totalConsumedAp;
        assertThat(progress.getAbilityPoints())
                .as("AP 불변식 위반 (%s): AP=%d, expected=(accLv-1)-consumed=(%d-1)-%d=%d",
                        context, progress.getAbilityPoints(),
                        accumulatedLevel, totalConsumedAp, expectedAp)
                .isEqualTo(expectedAp);
    }

    /**
     * 누적 레벨 생성기: 10~200.
     *
     * @return 누적 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> accumulatedLevels() {
        return Arbitraries.integers().between(10, 200);
    }

    /**
     * 대형 누적 레벨 생성기: 20~200.
     *
     * @return 누적 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> largeAccumulatedLevels() {
        return Arbitraries.integers().between(20, 200);
    }

    /**
     * 랭크업 횟수 생성기: 1~15 (F→MASTER까지 최대 15회).
     *
     * @return 랭크업 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> rankUpCounts() {
        return Arbitraries.integers().between(1, 15);
    }

    /**
     * 적은 랭크업 횟수 생성기: 1~7.
     *
     * @return 랭크업 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> smallRankUpCounts() {
        return Arbitraries.integers().between(1, 7);
    }

    /**
     * CharacterProgress에 ID를 리플렉션으로 설정한다.
     *
     * @param progress 대상 진행상황
     * @param id       설정할 ID
     */
    private void setId(final CharacterProgress progress, final Long id) {
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("id 설정 실패", exception);
        }
    }
}
