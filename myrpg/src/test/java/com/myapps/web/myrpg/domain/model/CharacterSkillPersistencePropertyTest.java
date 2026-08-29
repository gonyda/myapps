package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.List;
import java.util.Optional;
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
 * CharacterSkill 엔티티의 영속 라운드트립 프로퍼티 테스트.
 *
 * <p>Feature: 005-skill-system, Property 16: 영속 라운드트립
 *
 * <p><b>Validates: Requirements 10.1, 10.5</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CharacterSkillPersistencePropertyTest {

    private static final long CHARACTER_ID_MIN = 1L;
    private static final long CHARACTER_ID_MAX = 10_000L;
    private static final int USAGE_COUNT_MAX = 5000;
    private static final int KILL_COUNT_MAX = 1500;
    private static final int SKILL_ID_MIN_LENGTH = 3;
    private static final int SKILL_ID_MAX_LENGTH = 20;

    private final TestEntityManager entityManager;
    private final CharacterSkillRepository repository;

    CharacterSkillPersistencePropertyTest(
            final TestEntityManager entityManager, final CharacterSkillRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    // Feature: 005-skill-system, Property 16: 영속 라운드트립

    /**
     * 임의의 유효한 CharacterSkill을 저장 후 findById로 조회하면 skillId·rank·usageCount가 모두 보존되는지 검증한다.
     *
     * @param characterId 임의 캐릭터 ID
     * @param skillId 임의 스킬 카탈로그 ID
     * @param rank 임의 스킬 랭크 (16종)
     * @param usageCount 임의 사용 횟수
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_savedAndFoundById(
            @ForAll("characterIds") final long characterId,
            @ForAll("skillIds") final String skillId,
            @ForAll("ranks") final SkillRank rank,
            @ForAll("usageCounts") final int usageCount) {

        final CharacterSkill skill = new CharacterSkill(characterId, skillId, rank, usageCount);

        entityManager.persistAndFlush(skill);
        final Long savedId = skill.getId();
        entityManager.clear();

        final Optional<CharacterSkill> found = repository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getCharacterId()).isEqualTo(characterId);
        assertThat(found.get().getSkillId()).isEqualTo(skillId);
        assertThat(found.get().getRank()).isEqualTo(rank);
        assertThat(found.get().getUsageCount()).isEqualTo(usageCount);
    }

    /**
     * 동일 캐릭터의 여러 스킬을 저장 후 findByCharacterId로 조회하면 해당 캐릭터의 모든 스킬이 반환되는지 검증한다.
     *
     * @param characterId 임의 캐릭터 ID
     * @param rank 임의 스킬 랭크
     * @param usageCount 임의 사용 횟수
     */
    @Property(tries = 100)
    void should_returnCorrectEntries_when_findByCharacterId(
            @ForAll("characterIds") final long characterId,
            @ForAll("ranks") final SkillRank rank,
            @ForAll("usageCounts") final int usageCount) {

        final long otherCharacterId = characterId + 1;
        final CharacterSkill skill1 = new CharacterSkill(characterId, "smash", rank, usageCount);
        final CharacterSkill skill2 = new CharacterSkill(characterId, "windmill", rank, usageCount);
        final CharacterSkill otherSkill =
                new CharacterSkill(otherCharacterId, "firebolt", rank, usageCount);

        entityManager.persistAndFlush(skill1);
        entityManager.persistAndFlush(skill2);
        entityManager.persistAndFlush(otherSkill);
        entityManager.clear();

        final List<CharacterSkill> results = repository.findByCharacterId(characterId);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(s -> s.getCharacterId().equals(characterId));
        assertThat(results)
                .extracting(CharacterSkill::getSkillId)
                .containsExactlyInAnyOrder("smash", "windmill");
    }

    /**
     * 특정 캐릭터의 특정 스킬을 저장 후 findByCharacterIdAndSkillId로 조회하면 정확한 엔트리가 반환되는지 검증한다.
     *
     * @param characterId 임의 캐릭터 ID
     * @param skillId 임의 스킬 카탈로그 ID
     * @param rank 임의 스킬 랭크
     * @param usageCount 임의 사용 횟수
     */
    @Property(tries = 100)
    void should_returnCorrectEntry_when_findByCharacterIdAndSkillId(
            @ForAll("characterIds") final long characterId,
            @ForAll("skillIds") final String skillId,
            @ForAll("ranks") final SkillRank rank,
            @ForAll("usageCounts") final int usageCount) {

        final CharacterSkill skill = new CharacterSkill(characterId, skillId, rank, usageCount);

        entityManager.persistAndFlush(skill);
        entityManager.clear();

        final Optional<CharacterSkill> found =
                repository.findByCharacterIdAndSkillId(characterId, skillId);

        assertThat(found).isPresent();
        assertThat(found.get().getSkillId()).isEqualTo(skillId);
        assertThat(found.get().getRank()).isEqualTo(rank);
        assertThat(found.get().getUsageCount()).isEqualTo(usageCount);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 캐릭터 ID Arbitrary를 제공한다 (1~10,000).
     *
     * @return 캐릭터 ID Arbitrary
     */
    @Provide
    Arbitrary<Long> characterIds() {
        return Arbitraries.longs().between(CHARACTER_ID_MIN, CHARACTER_ID_MAX);
    }

    /**
     * 스킬 카탈로그 ID Arbitrary를 제공한다 (3~20자, 알파벳 소문자 + 언더스코어).
     *
     * @return 스킬 ID Arbitrary
     */
    @Provide
    Arbitrary<String> skillIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(SKILL_ID_MIN_LENGTH)
                .ofMaxLength(SKILL_ID_MAX_LENGTH);
    }

    /**
     * 스킬 랭크 Arbitrary를 제공한다 (16종 중 하나).
     *
     * @return 스킬 랭크 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> ranks() {
        return Arbitraries.of(SkillRank.values());
    }

    /**
     * 사용 횟수 Arbitrary를 제공한다 (0~5,000).
     *
     * @return 사용 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> usageCounts() {
        return Arbitraries.integers().between(0, USAGE_COUNT_MAX);
    }
}
