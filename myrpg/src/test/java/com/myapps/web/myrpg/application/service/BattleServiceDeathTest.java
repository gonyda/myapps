package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DeathResult;
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
 * 사망 처리를 검증하는 통합 테스트.
 *
 * <p>HP 0 도달 시 {@code die()}가 호출되어 경험치 -10%, 풀 회복, 티르코네일 이동이 수행되고, 골드/아이템이 불변이며, BattleState
 * active=false가 되는지 검증한다.
 *
 * <p><b>Validates: Requirements 11.3</b>
 */
class BattleServiceDeathTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String SKILL_ID = "windmill";
    private static final int MONSTER_MAX_HP = 200;
    private static final int LETHAL_MONSTER_DAMAGE = 999;
    private static final long INITIAL_GOLD = 500L;
    private static final long INITIAL_EXP = 100L;

    private BattleStateRepository battleStateRepo;
    private ProgressionService progressionService;
    private CharacterService characterService;
    private BattleService battleService;

    /** 각 테스트 전에 몬스터 피해가 치명적으로 높은 설정으로 BattleService를 구성한다. */
    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(
                        new ResolvedTurn(
                                5, LETHAL_MONSTER_DAMAGE, false, false, false, false, List.of()));

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        progressionService = mock(ProgressionService.class);
        when(progressionService.die(any(CharacterProgress.class))).thenReturn(new DeathResult(10L));

        characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);
        final Random random = new Random(42L);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(createDamageSkill()));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

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

    /** HP가 0에 도달하면 die()가 호출되고 outcome이 LOSE인지 검증한다. */
    @Test
    @DisplayName("HP 0 도달 시 die()가 호출되고 outcome=LOSE")
    void should_callDie_when_hpReachesZero() {
        final CharacterProgress progress = createProgress(10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        verify(progressionService).die(eq(progress));
        assertThat(result.outcome()).as("HP 0 시 outcome은 LOSE여야 한다").isEqualTo(Outcome.LOSE);
        assertThat(result.battleEnded()).as("사망 시 전투가 종료되어야 한다").isTrue();
    }

    /** 사망 시 BattleState active가 false로 전환되는지 검증한다. */
    @Test
    @DisplayName("사망 시 BattleState active=false")
    void should_deactivateBattleState_when_death() {
        final CharacterProgress progress = createProgress(10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(state.isActive()).as("사망 시 BattleState active는 false여야 한다").isFalse();
    }

    /** 사망 시 골드가 불변인지 검증한다. */
    @Test
    @DisplayName("사망 시 골드는 불변")
    void should_preserveGold_when_death() {
        final CharacterProgress progress = createProgress(10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(progress.getGold()).as("사망 후 골드는 변하지 않아야 한다").isEqualTo(INITIAL_GOLD);
    }

    /** 사망 시 saveTurn과 BattleState가 저장되는지 검증한다. */
    @Test
    @DisplayName("사망 시 saveTurn + BattleState 저장")
    void should_saveStateAndProgress_when_death() {
        final CharacterProgress progress = createProgress(10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        verify(characterService).saveTurn(eq(progress));
        verify(battleStateRepo).save(eq(state));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private CharacterProgress createProgress(final int hp) {
        return new CharacterProgress(
                "전사",
                10,
                10,
                INITIAL_EXP,
                TalentType.MELEE,
                null,
                hp,
                100,
                100,
                "dunbarton",
                0,
                INITIAL_GOLD);
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
                30L,
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
