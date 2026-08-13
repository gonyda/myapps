package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BattleService의 스킬 critBonus 적용을 검증하는 단위 테스트.
 *
 * <p>smash(critBonus=80), magnum_shot(critBonus=100), firebolt(critBonus=0)를 구성하여
 * 각 스킬별 실효 크리티컬이 올바르게 계산되는지 확인한다.
 */
class BattleServiceCritBonusTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "test_monster";
    private static final int MONSTER_MAX_HP = 300;
    private static final int MONSTER_CRITICAL = 60;
    private static final int HIGH_HP = 500;
    private static final int BASE_CRITICAL_FROM_EQUIP = 200;

    @Test
    @DisplayName("smash(critBonus=80) 사용 시 실효 크리 = baseCritical + 80")
    void should_addSmashCritBonus_when_smashSkillUsed() {
        final DamageSkill smash = createSmash();
        final int expectedCritical = BASE_CRITICAL_FROM_EQUIP + 80;

        final int capturedCritical = capturePlayerCritical(smash, "smash");

        assertThat(capturedCritical)
                .as("smash(critBonus=80) 실효 크리 = %d + 80 = %d",
                        BASE_CRITICAL_FROM_EQUIP, expectedCritical)
                .isEqualTo(expectedCritical);
    }

    @Test
    @DisplayName("magnum_shot(critBonus=100) 사용 시 실효 크리 = baseCritical + 100")
    void should_addMagnumShotCritBonus_when_magnumShotUsed() {
        final DamageSkill magnumShot = createMagnumShot();
        final int expectedCritical = BASE_CRITICAL_FROM_EQUIP + 100;

        final int capturedCritical = capturePlayerCritical(magnumShot, "magnum_shot");

        assertThat(capturedCritical)
                .as("magnum_shot(critBonus=100) 실효 크리 = %d + 100 = %d",
                        BASE_CRITICAL_FROM_EQUIP, expectedCritical)
                .isEqualTo(expectedCritical);
    }

    @Test
    @DisplayName("firebolt(critBonus=0) 사용 시 실효 크리 = baseCritical + 0")
    void should_notAddCritBonus_when_fireboltsUsed() {
        final DamageSkill firebolt = createFirebolt();
        final int expectedCritical = BASE_CRITICAL_FROM_EQUIP;

        final int capturedCritical = capturePlayerCritical(firebolt, "firebolt");

        assertThat(capturedCritical)
                .as("firebolt(critBonus=0) 실효 크리 = %d + 0 = %d",
                        BASE_CRITICAL_FROM_EQUIP, expectedCritical)
                .isEqualTo(expectedCritical);
    }

    @Test
    @DisplayName("baseCritical + critBonus > 1000이면 1000으로 캡된다")
    void should_capAt1000_when_sumExceeds1000() {
        final DamageSkill magnumShot = createMagnumShot();

        final int highBaseCritical = 950;
        final int capturedCritical = capturePlayerCriticalWithBase(magnumShot, "magnum_shot", highBaseCritical);

        assertThat(capturedCritical)
                .as("950 + 100 = 1050 → 캡 1000")
                .isEqualTo(1000);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private int capturePlayerCritical(final DamageSkill skill, final String skillId) {
        return capturePlayerCriticalWithBase(skill, skillId, BASE_CRITICAL_FROM_EQUIP);
    }

    private int capturePlayerCriticalWithBase(final DamageSkill skill, final String skillId,
                                              final int baseCritical) {
        final BattleResolver resolver = mock(BattleResolver.class);
        final ArgumentCaptor<TurnInput> captor = ArgumentCaptor.forClass(TurnInput.class);

        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(10, 5, false, false, false, false, List.of(new HitResult(10, false))));

        final BattleService service = buildService(resolver, skill, skillId, baseCritical);
        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, skillId);

        verify(resolver).resolve(captor.capture());
        return captor.getValue().playerCritical();
    }

    private BattleService buildService(final BattleResolver resolver,
                                       final DamageSkill skill,
                                       final String skillId,
                                       final int baseCritical) {
        final Random random = new Random(42L);

        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);

        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        final InventoryService inventoryService = mock(InventoryService.class);
        final Stats critStats = new Stats(0, 0, 0, baseCritical, 0);
        when(inventoryService.equippedBonus())
                .thenReturn(new EquippedBonusResult(critStats, new VitalMax(0, 0, 0)));

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final com.myapps.web.myrpg.domain.model.StatProgression statProgression =
                mock(com.myapps.web.myrpg.domain.model.StatProgression.class);
        when(statProgression.levelStatsFor(anyInt(), any(TalentType.class)))
                .thenReturn(Stats.ZERO);

        final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(skillId)).thenReturn(Optional.of(skill));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(Optional.of(new CharacterSkill(CHARACTER_ID, skillId, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        return new BattleService(
                battleStateRepo, resolver, monsterService, aiService,
                rewardService, skillService, inventoryService, progressionService,
                characterService, statProgression, actionLog, random,
                skillCatalogService, characterSkillRepo, itemCatalogService);
    }

    private CharacterProgress createProgress() {
        return new CharacterProgress(
                "전사", 1, 1, 100L, TalentType.MELEE, null,
                HIGH_HP, 100, 100, "dunbarton", 0, 500L);
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID, "테스트 몬스터", MonsterType.NORMAL, 5, MONSTER_MAX_HP,
                20, 5, MONSTER_CRITICAL, 30L, new GoldDrop(10, 20), List.of(),
                List.of("대사1", "대사2", "대사3"));
    }

    private DamageSkill createSmash() {
        return new DamageSkill(
                "smash", "스매시", SkillType.HEAVY, SkillTalent.MELEE, 10,
                createFullRankMap(130), "강력한 일격", 1, 80);
    }

    private DamageSkill createMagnumShot() {
        return new DamageSkill(
                "magnum_shot", "매그넘 샷", SkillType.HEAVY, SkillTalent.ARCHERY, 12,
                createFullRankMap(140), "강력한 한 발", 1, 100);
    }

    private DamageSkill createFirebolt() {
        return new DamageSkill(
                "firebolt", "파이어볼트", SkillType.HEAVY, SkillTalent.MAGIC, 15,
                createFullRankMap(130), "화염 마법", 1, 0);
    }

    private Map<SkillRank, Integer> createFullRankMap(final int baseValue) {
        return Map.ofEntries(
                Map.entry(SkillRank.F, baseValue), Map.entry(SkillRank.E, baseValue + 8),
                Map.entry(SkillRank.D, baseValue + 16), Map.entry(SkillRank.C, baseValue + 24),
                Map.entry(SkillRank.B, baseValue + 32), Map.entry(SkillRank.A, baseValue + 40),
                Map.entry(SkillRank.R9, baseValue + 48), Map.entry(SkillRank.R8, baseValue + 56),
                Map.entry(SkillRank.R7, baseValue + 64), Map.entry(SkillRank.R6, baseValue + 72),
                Map.entry(SkillRank.R5, baseValue + 80), Map.entry(SkillRank.R4, baseValue + 88),
                Map.entry(SkillRank.R3, baseValue + 96), Map.entry(SkillRank.R2, baseValue + 104),
                Map.entry(SkillRank.R1, baseValue + 112), Map.entry(SkillRank.MASTER, baseValue + 120));
    }
}
