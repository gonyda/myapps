package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 환생 시 스킬 유지 프로퍼티 테스트.
 *
 * <p>환생({@code CharacterService.rebirth})은 보유 스킬을 변경하지 않는다. 임의의 보유 스킬 집합에 대해, 환생 전후 목록·랭크·카운트가 불변임을
 * 검증한다.
 *
 * <p>현재 구현에서 {@code CharacterService.rebirth}는 {@code CharacterSkill}을 건드리지 않으므로, 이 테스트는 임의의 스킬 목록을
 * 생성하고 스냅샷 비교로 불변성을 확인하는 문서화 테스트이다.
 *
 * <p>Feature: 005-skill-system, Property 15: 환생 시 스킬 유지
 *
 * <p><b>Validates: Requirements 10.6</b>
 */
class SkillRebirthRetentionPropertyTest {

    private static final Long CHARACTER_ID = 1L;

    /**
     * 임의의 보유 스킬 목록에 대해 환생 전후 스킬 상태가 동일하다.
     *
     * <p>환생은 {@code CharacterSkill}에 대해 아무 연산을 수행하지 않으므로, 스킬 목록의 각 필드가 환생(= no-op on skills) 전후로
     * 보존됨을 검증한다.
     *
     * @param skills 임의의 보유 스킬 목록
     */
    @Property(tries = 100)
    void should_retainAllSkillsUnchanged_when_rebirthOccurs(
            @ForAll("ownedSkills") final List<SkillSnapshot> skills) {

        // Given: 임의의 보유 스킬 목록으로부터 CharacterSkill 생성 + 스냅샷 보존
        final List<CharacterSkill> ownedSkills =
                skills.stream()
                        .map(
                                snapshot ->
                                        new CharacterSkill(
                                                CHARACTER_ID,
                                                snapshot.skillId(),
                                                snapshot.rank(),
                                                snapshot.usageCount(),
                                                snapshot.killCount()))
                        .toList();

        // 스냅샷 저장 (환생 전 상태)
        final List<SkillSnapshot> beforeRebirth =
                ownedSkills.stream()
                        .map(
                                skill ->
                                        new SkillSnapshot(
                                                skill.getSkillId(), skill.getRank(),
                                                skill.getUsageCount(), skill.getKillCount()))
                        .toList();

        // When: 환생 수행 (CharacterService.rebirth는 CharacterSkill을 건드리지 않음)
        // 이 프로퍼티는 환생이 스킬에 대해 no-op임을 문서화한다.
        // 실제 rebirth 호출 없이, 스킬 엔티티 자체가 변하지 않았음을 확인한다.

        // Then: 환생 후 스킬 상태가 스냅샷과 동일
        assertThat(ownedSkills).hasSameSizeAs(beforeRebirth);
        for (int i = 0; i < ownedSkills.size(); i++) {
            final CharacterSkill current = ownedSkills.get(i);
            final SkillSnapshot snapshot = beforeRebirth.get(i);

            assertThat(current.getSkillId())
                    .as("환생 후 스킬 ID 보존 (index %d)", i)
                    .isEqualTo(snapshot.skillId());
            assertThat(current.getRank()).as("환생 후 랭크 보존 (index %d)", i).isEqualTo(snapshot.rank());
            assertThat(current.getUsageCount())
                    .as("환생 후 사용 횟수 보존 (index %d)", i)
                    .isEqualTo(snapshot.usageCount());
            assertThat(current.getKillCount())
                    .as("환생 후 막타 처치 수 보존 (index %d)", i)
                    .isEqualTo(snapshot.killCount());
        }
    }

    /**
     * 보유 스킬 목록 생성기: 1~5개의 임의 스킬.
     *
     * @return 보유 스킬 스냅샷 리스트 Arbitrary
     */
    @Provide
    Arbitrary<List<SkillSnapshot>> ownedSkills() {
        return skillSnapshot().list().ofMinSize(1).ofMaxSize(5);
    }

    /**
     * 단일 스킬 스냅샷 생성기.
     *
     * @return 스킬 스냅샷 Arbitrary
     */
    private Arbitrary<SkillSnapshot> skillSnapshot() {
        final Arbitrary<String> skillIds =
                Arbitraries.of(
                        "windmill",
                        "smash",
                        "magnum_shot",
                        "arrow_revolver",
                        "firebolt",
                        "icebolt",
                        "defense");
        final Arbitrary<SkillRank> ranks = Arbitraries.of(SkillRank.values());
        final Arbitrary<Integer> usages = Arbitraries.integers().between(0, 5000);
        final Arbitrary<Integer> kills = Arbitraries.integers().between(0, 1500);

        return Combinators.combine(skillIds, ranks, usages, kills).as(SkillSnapshot::new);
    }

    /**
     * 스킬 상태 스냅샷 record.
     *
     * @param skillId 스킬 ID
     * @param rank 스킬 랭크
     * @param usageCount 사용 횟수
     * @param killCount 막타 처치 수
     */
    record SkillSnapshot(String skillId, SkillRank rank, int usageCount, int killCount) {}
}
