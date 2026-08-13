package com.myapps.web.myrpg.application.service;

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

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
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
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BattleService의 로그 분리(액션↔결산)를 검증하는 통합 테스트.
 *
 * <p>승리 턴: 결산 라인만 {@code actionLog.add} 호출, 액션 라인 미추가.
 * 시작 메서드: 하단 시작 로그 미추가.
 */
class BattleServiceLogSplitTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String SKILL_ID = "slash";
    private static final int MONSTER_MAX_HP = 300;
    private static final int HIGH_HP = 500;
    private static final long MONSTER_EXP = 30L;
    private static final long DROP_GOLD = 50L;

    private ActionLog actionLog;
    private BattleService battleService;

    /**
     * 각 테스트 전에 처치 시나리오로 BattleService를 구성한다.
     */
    @BeforeEach
    void setUp() {
        final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);

        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(999, 0, true, false, false, false,
                        List.of(new HitResult(999, true))));

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        final DropResult drop = new DropResult(DROP_GOLD, List.of(new DroppedItem("potion", 1)));
        when(rewardService.rollDrop(any(Monster.class))).thenReturn(drop);

        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Random random = new Random(42L);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(createDamageSkill()));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        when(itemCatalogService.byId("potion"))
                .thenReturn(Optional.empty());

        battleService = new BattleService(
                battleStateRepo, resolver, monsterService, aiService,
                rewardService, skillService, inventoryService, progressionService,
                characterService, statProgression, actionLog, random,
                skillCatalogService, characterSkillRepo, itemCatalogService);
    }

    /**
     * 승리 턴에서 결산 라인만 actionLog에 추가되고, 전투 액션 라인은 combatLines에만 담긴다.
     */
    @Test
    @DisplayName("승리 턴: 결산 라인만 actionLog에 추가, 액션 라인은 combatLines에만")
    void should_onlyAddSettlementToActionLog_when_victoryTurn() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 1, false);
        state.setTurnCount(2);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.combatLines())
                .as("combatLines에 전투 액션 라인이 있어야 한다")
                .isNotEmpty();

        final List<ActionLogEntry> entries = actionLog.getEntries();
        assertThat(entries)
                .as("actionLog에 결산 라인이 추가되어야 한다")
                .isNotEmpty();

        final boolean allSettlement = entries.stream()
                .allMatch(e -> e.message().contains("골드") || e.message().contains("획득")
                        || e.message().contains("경험치"));
        assertThat(allSettlement)
                .as("actionLog에 추가된 모든 라인이 결산 라인이어야 한다")
                .isTrue();

        final List<String> combatLineContents = result.combatLines();
        final boolean noSettlementInCombat = combatLineContents.stream()
                .noneMatch(line -> line.contains("골드") || line.contains("경험치를 획득"));
        assertThat(noSettlementInCombat)
                .as("combatLines에 결산 라인이 포함되지 않아야 한다")
                .isTrue();
    }

    /**
     * 비처치 일반 턴에서 actionLog에 아무것도 추가되지 않는다.
     */
    @Test
    @DisplayName("비처치 일반 턴: actionLog에 아무것도 추가되지 않음")
    void should_notAddToActionLog_when_normalNonKillTurn() {
        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(10, 5, false, false, false, false,
                        List.of(new HitResult(10, false))));

        final BattleService normalService = buildNonKillService(resolver);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = normalService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.combatLines())
                .as("일반 턴에서 combatLines는 비어있지 않아야 한다")
                .isNotEmpty();
        assertThat(actionLog.size())
                .as("비처치 일반 턴에서 actionLog에 아무것도 추가되지 않아야 한다")
                .isZero();
    }

    /**
     * start() 호출 시 actionLog에 시작 로그가 추가되지 않는다.
     */
    @Test
    @DisplayName("start(): 하단 시작 로그 미추가")
    void should_notAddStartLogToActionLog_when_battleStarts() {
        final CharacterProgress progress = createProgress(HIGH_HP);

        battleService.start(progress, MONSTER_ID, false);

        assertThat(actionLog.size())
                .as("start()에서 actionLog에 아무것도 추가되지 않아야 한다")
                .isZero();
    }

    /**
     * 승리 턴에서 actionLog의 모든 엔트리 타입이 "combat"인지 검증한다.
     */
    @Test
    @DisplayName("승리 턴: actionLog 결산 라인 타입은 combat")
    void should_useLogTypeCombat_when_settlementLinesAdded() {
        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 1, false);
        state.setTurnCount(2);

        battleService.takeTurn(progress, state, SKILL_ID);

        final boolean allCombatType = actionLog.getEntries().stream()
                .allMatch(e -> "combat".equals(e.type()));
        assertThat(allCombatType)
                .as("결산 라인의 타입은 'combat'이어야 한다")
                .isTrue();
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private BattleService buildNonKillService(final BattleResolver resolver) {
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
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Random random = new Random(42L);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(createDamageSkill()));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        return new BattleService(
                battleStateRepo, resolver, monsterService, aiService,
                rewardService, skillService, inventoryService, progressionService,
                characterService, statProgression, actionLog, random,
                skillCatalogService, characterSkillRepo, itemCatalogService);
    }

    private CharacterProgress createProgress(final int hp) {
        final CharacterProgress progress = new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null,
                hp, 100, 100, "dunbarton", 0, 500L);
        setId(progress, CHARACTER_ID);
        return progress;
    }

    private void setId(final CharacterProgress progress, final long id) {
        try {
            final java.lang.reflect.Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID, "너구리", MonsterType.NORMAL, 5, MONSTER_MAX_HP,
                20, 5, 50, MONSTER_EXP, new GoldDrop(10, 20), List.of(),
                List.of("소리", "행동1", "행동2"));
    }

    private DamageSkill createDamageSkill() {
        return new DamageSkill(
                SKILL_ID, "슬래시", SkillType.NORMAL, SkillTalent.MELEE, 5,
                createFullRankMap(90), "기본 베기");
    }

    private Map<SkillRank, Integer> createFullRankMap(final int baseValue) {
        return Map.ofEntries(
                Map.entry(SkillRank.F, baseValue), Map.entry(SkillRank.E, baseValue + 5),
                Map.entry(SkillRank.D, baseValue + 10), Map.entry(SkillRank.C, baseValue + 15),
                Map.entry(SkillRank.B, baseValue + 20), Map.entry(SkillRank.A, baseValue + 25),
                Map.entry(SkillRank.R9, baseValue + 30), Map.entry(SkillRank.R8, baseValue + 35),
                Map.entry(SkillRank.R7, baseValue + 40), Map.entry(SkillRank.R6, baseValue + 45),
                Map.entry(SkillRank.R5, baseValue + 50), Map.entry(SkillRank.R4, baseValue + 55),
                Map.entry(SkillRank.R3, baseValue + 60), Map.entry(SkillRank.R2, baseValue + 65),
                Map.entry(SkillRank.R1, baseValue + 70), Map.entry(SkillRank.MASTER, baseValue + 80));
    }
}
