package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 액션↔결산 로그 라우팅 프로퍼티 검증.
 *
 * <p>전투 액션 라인(플레이어/몬스터 행동·선제·캐스팅 실패·도망 실패)은 {@code BattleTurnResult.combatLines}에 담기고 화면 하단 {@code
 * ActionLog}에는 추가되지 않으며, 결산/사망/도망 성공 라인은 {@code ActionLog}에 추가된다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 8: 액션↔결산 로그 라우팅
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5</b>
 */
class BattleServiceLogRoutingPropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String SKILL_ID = "slash";
    private static final int MONSTER_MAX_HP = 300;
    private static final int HIGH_HP = 500;
    private static final int FLEE_THRESHOLD = 50;
    private static final int PERCENT_DIVISOR = 100;
    private static final long MONSTER_EXP = 30L;

    /**
     * 일반 전투 턴에서 combatLines에 액션 라인이 담기고 actionLog에는 추가되지 않는다.
     *
     * @param seed 난수 시드
     */
    @Property(tries = 100)
    void should_routeActionLinesToCombatLinesOnly_when_normalCombatTurn(
            @ForAll("seeds") final long seed) {
        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForNormalTurn(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.combatLines()).as("일반 턴에서 combatLines는 비어있지 않아야 한다").isNotEmpty();

        assertThat(actionLog.size()).as("일반 비처치 턴에서 actionLog에는 아무것도 추가되지 않아야 한다").isZero();
    }

    /**
     * 처치 턴에서 결산 라인(골드/아이템/경험치)이 actionLog에 추가되고, combatLines에는 전투 액션만 담긴다.
     *
     * @param seed 난수 시드
     */
    @Property(tries = 100)
    void should_routeSettlementToActionLog_when_monsterKilled(@ForAll("seeds") final long seed) {
        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForKillTurn(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 1, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.combatLines()).as("처치 턴에서 combatLines(액션 라인)는 비어있지 않아야 한다").isNotEmpty();

        assertThat(actionLog.size()).as("처치 시 결산 라인(골드/경험치)이 actionLog에 추가되어야 한다").isGreaterThan(0);

        final boolean hasSettlementLine =
                actionLog.getEntries().stream()
                        .anyMatch(e -> e.message().contains("골드") || e.message().contains("경험치"));
        assertThat(hasSettlementLine).as("actionLog에 결산 라인(골드 또는 경험치)이 포함되어야 한다").isTrue();

        final boolean hasActionLine =
                actionLog.getEntries().stream()
                        .anyMatch(e -> e.message().contains("피해") && !e.message().contains("획득"));
        assertThat(hasActionLine).as("actionLog에 전투 액션 라인(피해 등)이 포함되지 않아야 한다").isFalse();
    }

    /**
     * 사망 턴에서 사망/부활 메시지가 actionLog에 추가된다.
     *
     * @param seed 난수 시드
     */
    @Property(tries = 100)
    void should_routeDeathLineToActionLog_when_playerDies(@ForAll("seeds") final long seed) {
        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForDeathTurn(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(1);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.combatLines()).as("사망 턴에서도 combatLines는 존재해야 한다").isNotEmpty();

        final boolean hasDeathLine =
                actionLog.getEntries().stream()
                        .anyMatch(e -> e.message().contains("쓰러졌다") || e.message().contains("부활"));
        assertThat(hasDeathLine).as("사망 시 actionLog에 사망/부활 라인이 추가되어야 한다").isTrue();
    }

    /**
     * 도망 성공 시 "도망쳤다!" 메시지가 actionLog에 추가된다.
     *
     * @param seed 도망 성공을 보장하는 시드
     */
    @Property(tries = 100)
    void should_routeFleeSuccessToActionLog_when_fleeSucceeds(
            @ForAll("fleeSuccessSeeds") final long seed) {
        final Random testRandom = new Random(seed);
        final int roll = testRandom.nextInt(PERCENT_DIVISOR);
        if (roll >= FLEE_THRESHOLD) {
            return;
        }

        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForFlee(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        service.flee(progress, state);

        final boolean hasFleeSuccess =
                actionLog.getEntries().stream().anyMatch(e -> e.message().contains("도망쳤다"));
        assertThat(hasFleeSuccess).as("도망 성공 시 actionLog에 '도망쳤다!' 메시지가 추가되어야 한다").isTrue();
    }

    /**
     * 도망 실패 시 몬스터 피해 메시지가 combatLines에만 담기고 actionLog에는 추가되지 않는다.
     *
     * @param seed 도망 실패를 보장하는 시드
     */
    @Property(tries = 100)
    void should_routeFleeFailureToCombatLinesOnly_when_fleeFails(
            @ForAll("fleeFailSeeds") final long seed) {
        final Random testRandom = new Random(seed);
        final int roll = testRandom.nextInt(PERCENT_DIVISOR);
        if (roll < FLEE_THRESHOLD) {
            return;
        }

        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForFlee(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.flee(progress, state);

        assertThat(result.combatLines()).as("도망 실패 시 combatLines에 몬스터 피해 메시지가 담겨야 한다").isNotEmpty();

        final boolean hasFleeFailInActionLog =
                actionLog.getEntries().stream()
                        .anyMatch(e -> e.message().contains("도망 실패") || e.message().contains("피해"));
        assertThat(hasFleeFailInActionLog).as("도망 실패 시 actionLog에는 전투 라인이 추가되지 않아야 한다").isFalse();
    }

    /**
     * start() 호출 시 actionLog에 아무것도 추가되지 않는다.
     *
     * @param seed 난수 시드
     */
    @Property(tries = 100)
    void should_notAddToActionLog_when_battleStarts(@ForAll("seeds") final long seed) {
        final ActionLog actionLog = createActionLog();
        final BattleService service = buildServiceForStart(new Random(seed), actionLog);

        final CharacterProgress progress = createProgress(HIGH_HP);

        service.start(progress, MONSTER_ID, false);

        assertThat(actionLog.size()).as("전투 시작 시 actionLog에 아무것도 추가되지 않아야 한다").isZero();
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 일반 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs().between(0L, 100000L);
    }

    /**
     * 도망 성공 확률이 있는 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> fleeSuccessSeeds() {
        return Arbitraries.longs().between(0L, 50000L);
    }

    /**
     * 도망 실패 확률이 있는 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> fleeFailSeeds() {
        return Arbitraries.longs().between(0L, 50000L);
    }

    // ─── Service builders ───────────────────────────────────────────────────

    private BattleService buildServiceForNormalTurn(
            final Random random, final ActionLog actionLog) {
        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(
                        new ResolvedTurn(
                                10,
                                5,
                                false,
                                false,
                                false,
                                false,
                                List.of(new HitResult(10, false))));

        return buildBaseService(resolver, random, actionLog, false, false);
    }

    private BattleService buildServiceForKillTurn(final Random random, final ActionLog actionLog) {
        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(
                        new ResolvedTurn(
                                999,
                                0,
                                false,
                                false,
                                false,
                                false,
                                List.of(new HitResult(999, false))));

        return buildBaseService(resolver, random, actionLog, true, false);
    }

    private BattleService buildServiceForDeathTurn(final Random random, final ActionLog actionLog) {
        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(0, 999, false, false, false, false, List.of()));

        return buildBaseService(resolver, random, actionLog, false, true);
    }

    private BattleService buildServiceForFlee(final Random random, final ActionLog actionLog) {
        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.baseDamage(anyInt(), anyInt(), anyInt())).thenReturn(15);
        when(resolver.rollCritical(anyInt())).thenReturn(false);
        when(resolver.finalDamage(anyInt(), anyDouble(), any(Boolean.class))).thenReturn(15);

        return buildBaseService(resolver, random, actionLog, false, false);
    }

    private BattleService buildServiceForStart(final Random random, final ActionLog actionLog) {
        final BattleResolver resolver = mock(BattleResolver.class);
        return buildBaseService(resolver, random, actionLog, false, false);
    }

    private BattleService buildBaseService(
            final BattleResolver resolver,
            final Random random,
            final ActionLog actionLog,
            final boolean killScenario,
            final boolean deathScenario) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        if (killScenario) {
            final DropResult drop = new DropResult(50L, List.of(new DroppedItem("potion", 1)));
            when(rewardService.rollDrop(any(Monster.class))).thenReturn(drop);
        }

        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        if (deathScenario) {
            when(progressionService.die(any(CharacterProgress.class)))
                    .thenReturn(new DeathResult(10L));
        }

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(createDamageSkill()));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        return new BattleService(
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

    // ─── Helpers ────────────────────────────────────────────────────────────

    private ActionLog createActionLog() {
        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        return new ActionLog(clock);
    }

    private CharacterProgress createProgress(final int hp) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        10,
                        10,
                        100L,
                        TalentType.MELEE,
                        null,
                        hp,
                        100,
                        100,
                        "dunbarton",
                        0,
                        500L);
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
                "슬래시",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                5,
                createFullRankMap(90),
                "기본 베기");
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
                Map.entry(SkillRank.R1, baseValue + 70),
                        Map.entry(SkillRank.MASTER, baseValue + 80));
    }
}
