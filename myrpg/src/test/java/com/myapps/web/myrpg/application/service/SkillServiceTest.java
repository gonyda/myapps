package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SkillService 핵심 메서드의 예시 단위 테스트.
 *
 * <p>랭크업 성공·AP 부족·조건 미충족·MASTER·습득·시드·임시 드라이버·카운팅 훅을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String WINDMILL_ID = "windmill";

    @Mock
    private CharacterSkillRepository characterSkillRepository;

    @Mock
    private CharacterProgressRepository characterProgressRepository;

    @Mock
    private SkillCatalogService skillCatalogService;

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(characterSkillRepository, characterProgressRepository, skillCatalogService);
    }

    @Test
    void should_rankUp_successfully_when_conditions_and_ap_are_met() {
        // F→E: 사용 5, 막타 1, AP 1
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 5, 1);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final boolean result = skillService.rankUp(progress, WINDMILL_ID);

        assertThat(result).isTrue();
        assertThat(skill.getRank()).isEqualTo(SkillRank.E);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getKillCount()).isZero();
        assertThat(progress.getAbilityPoints()).isEqualTo(9);
    }

    @Test
    void should_throw_InsufficientAbilityPointsException_when_ap_is_insufficient() {
        // F→E: 조건 충족이나 AP 0
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 5, 1);
        final CharacterProgress progress = createProgressWithAp(0);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> skillService.rankUp(progress, WINDMILL_ID))
                .isInstanceOf(InsufficientAbilityPointsException.class)
                .hasMessageContaining("AP 부족");

        // 상태 불변 확인
        assertThat(skill.getRank()).isEqualTo(SkillRank.F);
        assertThat(skill.getUsageCount()).isEqualTo(5);
        assertThat(skill.getKillCount()).isEqualTo(1);
        assertThat(progress.getAbilityPoints()).isZero();
    }

    @Test
    void should_return_false_when_conditions_not_met() {
        // 사용 횟수 부족 (F→E: 5 필요, 3만 있음)
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 3, 1);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));

        final boolean result = skillService.rankUp(progress, WINDMILL_ID);

        assertThat(result).isFalse();
        assertThat(skill.getRank()).isEqualTo(SkillRank.F);
        assertThat(progress.getAbilityPoints()).isEqualTo(10);
    }

    @Test
    void should_return_false_when_rank_is_master() {
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.MASTER, 9999, 9999);
        final CharacterProgress progress = createProgressWithAp(200);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));

        final boolean result = skillService.rankUp(progress, WINDMILL_ID);

        assertThat(result).isFalse();
        assertThat(skill.getRank()).isEqualTo(SkillRank.MASTER);
        assertThat(progress.getAbilityPoints()).isEqualTo(200);
    }

    @Test
    void should_calculate_rankupBonus_from_owned_skills() {
        // windmill(MELEE, rank A=order 5) → STR +5
        final CharacterSkill windmillA = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.A, 0, 0);
        final DamageSkill windmillCatalog = new DamageSkill(
                WINDMILL_ID, "윈드밀", SkillType.NORMAL, SkillTalent.MELEE, 15,
                java.util.Map.of(), "범위 공격");

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID))
                .thenReturn(List.of(windmillA));
        when(skillCatalogService.byId(WINDMILL_ID))
                .thenReturn(Optional.of(windmillCatalog));

        final Stats bonus = skillService.rankupBonus(CHARACTER_ID);

        assertThat(bonus.str()).isEqualTo(5);
        assertThat(bonus.dex()).isZero();
        assertThat(bonus.intelligence()).isZero();
        assertThat(bonus.critical()).isZero();
        assertThat(bonus.defense()).isZero();
    }

    @Test
    void should_calculate_all_masters_bonus_as_STR30_DEX30_INT30_DEF15() {
        // 7종 스킬 전부 MASTER(order=15): MELEE×2 + ARCHERY×2 + MAGIC×2 + COMMON×1
        final CharacterSkill smashMaster = new CharacterSkill(CHARACTER_ID, "smash", SkillRank.MASTER, 0, 0);
        final CharacterSkill windmillMaster = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.MASTER, 0, 0);
        final CharacterSkill magnumMaster = new CharacterSkill(CHARACTER_ID, "magnum_shot", SkillRank.MASTER, 0, 0);
        final CharacterSkill arrowMaster = new CharacterSkill(CHARACTER_ID, "arrow_revolver", SkillRank.MASTER, 0, 0);
        final CharacterSkill fireboltMaster = new CharacterSkill(CHARACTER_ID, "firebolt", SkillRank.MASTER, 0, 0);
        final CharacterSkill iceboltMaster = new CharacterSkill(CHARACTER_ID, "icebolt", SkillRank.MASTER, 0, 0);
        final CharacterSkill defenseMaster = new CharacterSkill(CHARACTER_ID, "defense", SkillRank.MASTER, 0, 0);

        final DamageSkill smashCatalog = new DamageSkill(
                "smash", "스매시", SkillType.HEAVY, SkillTalent.MELEE, 10,
                java.util.Map.of(), "강력한 일격");
        final DamageSkill windmillCatalog = new DamageSkill(
                WINDMILL_ID, "윈드밀", SkillType.NORMAL, SkillTalent.MELEE, 7,
                java.util.Map.of(), "범위 공격");
        final DamageSkill magnumCatalog = new DamageSkill(
                "magnum_shot", "매그넘 샷", SkillType.HEAVY, SkillTalent.ARCHERY, 12,
                java.util.Map.of(), "강궁");
        final DamageSkill arrowCatalog = new DamageSkill(
                "arrow_revolver", "애로우 리볼버", SkillType.NORMAL, SkillTalent.ARCHERY, 8,
                java.util.Map.of(), "연사");
        final DamageSkill fireboltCatalog = new DamageSkill(
                "firebolt", "파이어볼트", SkillType.NORMAL, SkillTalent.MAGIC, 15,
                java.util.Map.of(), "화염");
        final DamageSkill iceboltCatalog = new DamageSkill(
                "icebolt", "아이스볼트", SkillType.NORMAL, SkillTalent.MAGIC, 12,
                java.util.Map.of(), "빙결");
        final DefenseSkill defenseCatalog = new DefenseSkill(
                "defense", "디펜스", SkillType.DEFENSE, SkillTalent.COMMON, 5,
                java.util.Map.of(), java.util.Map.of(), "방어");

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID))
                .thenReturn(List.of(smashMaster, windmillMaster, magnumMaster, arrowMaster,
                        fireboltMaster, iceboltMaster, defenseMaster));
        when(skillCatalogService.byId("smash")).thenReturn(Optional.of(smashCatalog));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(windmillCatalog));
        when(skillCatalogService.byId("magnum_shot")).thenReturn(Optional.of(magnumCatalog));
        when(skillCatalogService.byId("arrow_revolver")).thenReturn(Optional.of(arrowCatalog));
        when(skillCatalogService.byId("firebolt")).thenReturn(Optional.of(fireboltCatalog));
        when(skillCatalogService.byId("icebolt")).thenReturn(Optional.of(iceboltCatalog));
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(defenseCatalog));

        final Stats bonus = skillService.rankupBonus(CHARACTER_ID);

        // MELEE×2×15=30, ARCHERY×2×15=30, MAGIC×2×15=30, COMMON×1×15=15
        assertThat(bonus.str()).isEqualTo(30);
        assertThat(bonus.dex()).isEqualTo(30);
        assertThat(bonus.intelligence()).isEqualTo(30);
        assertThat(bonus.defense()).isEqualTo(15);
        assertThat(bonus.critical()).isZero();
    }

    @Test
    void should_learnSkill_add_new_skill_at_F_rank() {
        when(skillCatalogService.byId(WINDMILL_ID))
                .thenReturn(Optional.of(createDummySkill(WINDMILL_ID)));
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.empty());
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillService.learnSkill(CHARACTER_ID, WINDMILL_ID);

        final ArgumentCaptor<CharacterSkill> captor = ArgumentCaptor.forClass(CharacterSkill.class);
        verify(characterSkillRepository).save(captor.capture());
        final CharacterSkill saved = captor.getValue();
        assertThat(saved.getSkillId()).isEqualTo(WINDMILL_ID);
        assertThat(saved.getRank()).isEqualTo(SkillRank.F);
        assertThat(saved.getUsageCount()).isZero();
        assertThat(saved.getKillCount()).isZero();
    }

    @Test
    void should_learnSkill_ignore_duplicate() {
        when(skillCatalogService.byId(WINDMILL_ID))
                .thenReturn(Optional.of(createDummySkill(WINDMILL_ID)));
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 0, 0)));

        skillService.learnSkill(CHARACTER_ID, WINDMILL_ID);

        verify(characterSkillRepository, never()).save(any());
    }

    @Test
    void should_learnSkill_reject_unknown_skillId() {
        when(skillCatalogService.byId("unknown")).thenReturn(Optional.empty());

        skillService.learnSkill(CHARACTER_ID, "unknown");

        verify(characterSkillRepository, never()).save(any());
    }

    @Test
    void should_seedDefault_learn_windmill() {
        when(skillCatalogService.byId(WINDMILL_ID))
                .thenReturn(Optional.of(createDummySkill(WINDMILL_ID)));
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.empty());
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillService.seedDefault(CHARACTER_ID);

        verify(characterSkillRepository).save(any(CharacterSkill.class));
    }

    @Test
    void should_onSkillUsed_increment_usage() {
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 3, 0);
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillService.onSkillUsed(CHARACTER_ID, WINDMILL_ID);

        assertThat(skill.getUsageCount()).isEqualTo(4);
    }

    @Test
    void should_onSkillKill_increment_kill() {
        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 0, 2);
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillService.onSkillKill(CHARACTER_ID, WINDMILL_ID);

        assertThat(skill.getKillCount()).isEqualTo(3);
    }

    private CharacterProgress createProgressWithAp(final int abilityPoints) {
        final CharacterProgress progress = new CharacterProgress(
                "테스트", 1, 1, 0L,
                com.myapps.web.myrpg.domain.model.TalentType.MELEE,
                null, 100, 100, 100, "tir-chonaill", abilityPoints, 0L);
        setId(progress, CHARACTER_ID);
        return progress;
    }

    private Skill createDummySkill(final String id) {
        return new DamageSkill(id, "더미", SkillType.NORMAL, SkillTalent.MELEE, 10,
                java.util.Map.of(), "테스트용");
    }

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
