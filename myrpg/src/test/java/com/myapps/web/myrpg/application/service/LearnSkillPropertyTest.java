package com.myapps.web.myrpg.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 스킬 습득 프로퍼티 테스트.
 *
 * <p>임의의 skillId에 대해 {@code learnSkill}이 올바르게 동작하는지 검증한다.
 *
 * <ul>
 *   <li>카탈로그에 존재 + 미보유 → F 랭크·카운트 0으로 추가 (save 호출)
 *   <li>이미 보유 → 변경 없음 (save 미호출)
 *   <li>카탈로그에 없음 → 추가하지 않음 (save 미호출)
 * </ul>
 *
 * <p>Feature: 005-skill-system, Property 13: 스킬 습득
 *
 * <p><b>Validates: Requirements 11.1, 11.2, 11.3</b>
 */
class LearnSkillPropertyTest {

    private static final Long CHARACTER_ID = 1L;

    /**
     * 카탈로그에 존재하고 미보유인 스킬을 습득하면 F 랭크·카운트 0으로 추가된다.
     *
     * @param skillId 카탈로그에 존재하는 임의의 스킬 ID
     */
    @Property(tries = 100)
    void should_addSkillAtRankF_when_catalogHasAndNotOwned(
            @ForAll("catalogSkillId") final String skillId) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService =
                new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final Skill catalogSkill = createDummySkill(skillId);
        when(mockCatalog.byId(skillId)).thenReturn(Optional.of(catalogSkill));
        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, skillId))
                .thenReturn(Optional.empty());
        when(mockRepo.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillService.learnSkill(CHARACTER_ID, skillId);

        verify(mockRepo).save(any(CharacterSkill.class));
        verify(mockRepo)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                saved ->
                                        saved.getCharacterId().equals(CHARACTER_ID)
                                                && saved.getSkillId().equals(skillId)
                                                && saved.getRank() == SkillRank.F
                                                && saved.getUsageCount() == 0
                                                && saved.getKillCount() == 0));
    }

    /**
     * 이미 보유한 스킬을 습득하려 하면 변경 없이 무시된다.
     *
     * @param skillId 이미 보유한 임의의 스킬 ID
     */
    @Property(tries = 100)
    void should_notChange_when_alreadyOwned(@ForAll("catalogSkillId") final String skillId) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService =
                new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final Skill catalogSkill = createDummySkill(skillId);
        when(mockCatalog.byId(skillId)).thenReturn(Optional.of(catalogSkill));

        final CharacterSkill existingSkill =
                new CharacterSkill(CHARACTER_ID, skillId, SkillRank.D, 50, 20);
        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, skillId))
                .thenReturn(Optional.of(existingSkill));

        skillService.learnSkill(CHARACTER_ID, skillId);

        verify(mockRepo, never()).save(any(CharacterSkill.class));
    }

    /**
     * 카탈로그에 없는 스킬을 습득하려 하면 추가하지 않는다.
     *
     * @param skillId 카탈로그에 없는 임의의 스킬 ID
     */
    @Property(tries = 100)
    void should_notAdd_when_catalogDoesNotHave(@ForAll("unknownSkillId") final String skillId) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService =
                new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        when(mockCatalog.byId(skillId)).thenReturn(Optional.empty());

        skillService.learnSkill(CHARACTER_ID, skillId);

        verify(mockRepo, never()).save(any(CharacterSkill.class));
        verify(mockRepo, never()).findByCharacterIdAndSkillId(any(), any());
    }

    // ── Providers ──

    /**
     * 카탈로그에 존재하는 것으로 설정될 임의의 스킬 ID를 생성한다.
     *
     * @return 스킬 ID Arbitrary
     */
    @Provide
    Arbitrary<String> catalogSkillId() {
        return Arbitraries.of(
                "windmill",
                "smash",
                "magnum_shot",
                "arrow_revolver",
                "firebolt",
                "icebolt",
                "defense");
    }

    /**
     * 카탈로그에 없는 것으로 처리될 임의의 스킬 ID를 생성한다.
     *
     * @return 미지 스킬 ID Arbitrary
     */
    @Provide
    Arbitrary<String> unknownSkillId() {
        return Arbitraries.of(
                "unknown_skill",
                "nonexistent",
                "fake_bolt",
                "invalid_skill",
                "no_such_skill",
                "random_xxx");
    }

    // ── Helpers ──

    private Skill createDummySkill(final String skillId) {
        final Map<SkillRank, Integer> multiplierByRank = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            multiplierByRank.put(rank, 100 + rank.order() * 10);
        }
        return new DamageSkill(
                skillId,
                "테스트 스킬",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                10,
                multiplierByRank,
                "테스트 효과");
    }
}
