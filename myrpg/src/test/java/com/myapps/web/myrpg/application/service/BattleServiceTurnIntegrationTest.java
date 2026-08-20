package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 전투 턴 오케스트레이션 통합 테스트.
 *
 * <p>협력자 호출을 Mockito verify로 검증한다: {@code onSkillUsed}/{@code onSkillKill}, {@code
 * reduceDurability(0.05)}, {@code saveTurn} + {@code BattleState} 저장, 처치 시 {@code rollDrop} →
 * {@code acquire} → {@code gainExperience}.
 *
 * <p><b>Validates: Requirements 10.2, 10.3, 10.5, 13.1, 14.1, 14.2</b>
 */
class BattleServiceTurnIntegrationTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String SKILL_ID = "windmill";
    private static final int MONSTER_MAX_HP = 200;
    private static final int HIGH_HP = 500;
    private static final double DURABILITY_PER_ATTACK = 0.05;
    private static final long MONSTER_EXP = 30L;

    private BattleStateRepository battleStateRepo;
    private BattleResolver resolver;
    private MonsterService monsterService;
    private MonsterAiService aiService;
    private MonsterRewardService rewardService;
    private SkillService skillService;
    private InventoryService inventoryService;
    private ProgressionService progressionService;
    private CharacterService characterService;
    private SkillCatalogService skillCatalogService;
    private CharacterSkillRepository characterSkillRepo;
    private ItemCatalogService itemCatalogService;
    private BattleService battleService;

    /** 각 테스트 전에 모든 모의 객체를 생성하고 BattleService를 구성한다. */
    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(10, 5, false, false, false, false, List.of()));

        monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        rewardService = mock(MonsterRewardService.class);
        skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        progressionService = mock(ProgressionService.class);
        characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(createDamageSkill()));

        characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        itemCatalogService = mock(ItemCatalogService.class);

        final StatProgression statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);
        final Random random = new Random(42L);

        battleService =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        rewardService,
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        statProgression,
                        actionLog,
                        random,
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService);
    }

    /** 일반 턴에서 onSkillUsed가 호출되는지 검증한다. */
    @Test
    @DisplayName("턴 진행 시 onSkillUsed가 호출된다")
    void should_callOnSkillUsed_when_turnProgresses() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(skillService).onSkillUsed(any(), eq(SKILL_ID));
    }

    /** 몬스터 처치 시 onSkillKill이 호출되는지 검증한다. */
    @Test
    @DisplayName("몬스터 처치 시 onSkillKill이 호출된다")
    void should_callOnSkillKill_when_monsterKilled() {
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(999, 0, false, false, false, false, List.of()));

        final DropResult drop = new DropResult(50L, List.of(new DroppedItem("potion", 1)));
        when(rewardService.rollDrop(any(Monster.class))).thenReturn(drop);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 1, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(skillService).onSkillKill(any(), eq(SKILL_ID));
    }

    /** 비방어 스킬 사용 시 reduceDurabilityAndAutoUnequip(0.05)가 호출되는지 검증한다. */
    @Test
    @DisplayName("공격 스킬 사용 시 내구도 감소가 호출된다")
    void should_callReduceDurability_when_attackSkillUsed() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(inventoryService)
                .reduceDurabilityAndAutoUnequip(eq(progress), eq(DURABILITY_PER_ATTACK));
    }

    /** 턴 진행 시 saveTurn과 BattleState가 저장되는지 검증한다. */
    @Test
    @DisplayName("턴 진행 시 saveTurn과 BattleState가 저장된다")
    void should_saveTurnAndBattleState_when_turnProgresses() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(characterService).saveTurn(eq(progress));
        verify(battleStateRepo).save(eq(state));
    }

    /** 몬스터 처치 시 rollDrop → acquire → gainExperience 체인이 호출되는지 검증한다. */
    @Test
    @DisplayName("몬스터 처치 시 보상 체인(rollDrop → acquire → gainExperience)이 호출된다")
    void should_processRewardChain_when_monsterKilled() {
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(999, 0, false, false, false, false, List.of()));

        final DropResult drop = new DropResult(100L, List.of(new DroppedItem("sword", 1)));
        when(rewardService.rollDrop(any(Monster.class))).thenReturn(drop);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 1, false);
        state.setTurnCount(2);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        verify(rewardService).rollDrop(any(Monster.class));
        verify(inventoryService).acquire(eq(progress), eq(drop));
        verify(progressionService).gainExperience(eq(progress), eq(MONSTER_EXP));
        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
    }

    /** 비처치 턴에서는 onSkillKill이 호출되지 않는지 검증한다. */
    @Test
    @DisplayName("비처치 턴에서 onSkillKill은 호출되지 않는다")
    void should_notCallOnSkillKill_when_monsterNotKilled() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(skillService, never()).onSkillKill(any(), anyString());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private CharacterProgress createProgress(final int hp) {
        return new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null, hp, 100, 100, "dunbarton", 0, 500L);
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID,
                "너구리",
                MonsterType.NORMAL,
                5,
                MONSTER_MAX_HP,
                20,
                5,
                50,
                MONSTER_EXP,
                new GoldDrop(10, 20),
                List.of(),
                List.of("소리", "행동1", "행동2"));
    }

    private DamageSkill createDamageSkill() {
        return new DamageSkill(
                SKILL_ID,
                "윈드밀",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                5,
                createFullRankMap(100),
                "테스트 스킬");
    }

    private Map<SkillRank, Integer> createFullRankMap(final int baseValue) {
        return Map.ofEntries(
                Map.entry(SkillRank.F, baseValue), Map.entry(SkillRank.E, baseValue + 10),
                Map.entry(SkillRank.D, baseValue + 20), Map.entry(SkillRank.C, baseValue + 30),
                Map.entry(SkillRank.B, baseValue + 40), Map.entry(SkillRank.A, baseValue + 50),
                Map.entry(SkillRank.R9, baseValue + 60), Map.entry(SkillRank.R8, baseValue + 70),
                Map.entry(SkillRank.R7, baseValue + 80), Map.entry(SkillRank.R6, baseValue + 90),
                Map.entry(SkillRank.R5, baseValue + 100), Map.entry(SkillRank.R4, baseValue + 110),
                Map.entry(SkillRank.R3, baseValue + 120), Map.entry(SkillRank.R2, baseValue + 130),
                Map.entry(SkillRank.R1, baseValue + 140),
                        Map.entry(SkillRank.MASTER, baseValue + 150));
    }
}
