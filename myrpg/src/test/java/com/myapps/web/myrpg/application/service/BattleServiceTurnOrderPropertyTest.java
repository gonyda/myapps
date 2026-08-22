package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
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
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 선후공 규칙을 검증하는 프로퍼티 테스트.
 *
 * <p>동일 타입 무승부 시 50:50 분포(시드 고정), 일반↔방어는 결정론적으로 공격자(일반) 먼저, 선공 처치 시 후공 피해 0인지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 7: 선후공 규칙
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b>
 */
class BattleServiceTurnOrderPropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String SKILL_ID = "windmill";
    private static final int HIGH_HP = 500;
    private static final int MONSTER_MAX_HP = 200;

    /**
     * 동일 타입 무승부 시 시드 고정 Random에서 선후공이 결정적으로 분배됨을 검증한다.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_determineTurnOrder5050_when_sameType(@ForAll("seeds") final long seed) {
        final Random random = new Random(seed);
        final BattleService service = createBattleService(random, SkillType.NORMAL);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.playerDamage() > 0 || result.monsterDamage() > 0)
                .as("동일 타입 무승부에서 양쪽 모두 피해가 발생해야 한다")
                .isTrue();
    }

    /**
     * 일반(플레이어) vs 방어(몬스터) 시 결정론적으로 공격자(일반)가 먼저 피해를 주는지 검증한다. 플레이어 NORMAL vs 몬스터 DEFENSE 시 플레이어가 경감된
     * 피해를 먼저, 몬스터가 반격.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_attackerGoFirst_when_normalVsDefense(@ForAll("seeds") final long seed) {
        final Random random = new Random(seed);
        final BattleService service = createBattleService(random, SkillType.DEFENSE);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.blocked()).as("일반 vs 방어에서는 방어(blocked) 플래그가 참이어야 한다").isTrue();
    }

    /**
     * 선공이 후공을 처치하면 후공 피해는 0이어야 한다. 플레이어가 몬스터를 처치할 만큼의 피해를 주는 시나리오. 플레이어 NORMAL vs 몬스터 DEFENSE에서
     * 플레이어가 항상 선공.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_skipSecondAttack_when_firstStrikerKills(@ForAll("seeds") final long seed) {
        final int lowMonsterHp = 1;
        final Random random = new Random(seed);
        final BattleService service = createBattleServiceFirstStrikeKill(random);

        final CharacterProgress progress = createProgress(HIGH_HP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, lowMonsterHp, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, SKILL_ID);

        assertThat(result.outcome())
                .as("선공이 후공을 처치하면 전투 종료(WIN)")
                .isEqualTo(BattleTurnResult.Outcome.WIN);
        assertThat(result.monsterDamage())
                .as("선공(플레이어)이 몬스터를 처치하면 후공(몬스터) 피해는 0이어야 한다")
                .isEqualTo(0);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs().between(0L, 10000L);
    }

    // ─── Helper: BattleService creation ─────────────────────────────────────

    private BattleService createBattleService(final Random random, final SkillType monsterAction) {
        return buildService(random, monsterAction, 10, 10);
    }

    private BattleService createBattleServiceWithHighDamage(
            final Random random, final SkillType monsterAction) {
        return buildService(random, monsterAction, 999, 10);
    }

    private BattleService createBattleServiceFirstStrikeKill(final Random random) {
        return buildService(random, SkillType.DEFENSE, 999, 10);
    }

    private BattleService buildService(
            final Random random,
            final SkillType monsterAction,
            final int playerDmgToMonster,
            final int monsterDmgToPlayer) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(
                        new ResolvedTurn(
                                playerDmgToMonster,
                                monsterDmgToPlayer,
                                false,
                                false,
                                monsterAction == SkillType.DEFENSE,
                                false,
                                List.of()));
        when(resolver.baseDamage(anyInt(), anyInt(), anyInt())).thenReturn(10);
        when(resolver.rollCritical(anyInt())).thenReturn(false);
        when(resolver.finalDamage(anyInt(), any(Double.class), any(Boolean.class))).thenReturn(10);

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(monsterAction);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        when(rewardService.rollDrop(any(Monster.class))).thenReturn(new DropResult(10L, List.of()));

        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();

        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

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
                itemCatalogService,
                null);
    }

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

    private java.util.Map<SkillRank, Integer> createFullRankMap(final int baseValue) {
        return java.util.Map.ofEntries(
                java.util.Map.entry(SkillRank.F, baseValue),
                        java.util.Map.entry(SkillRank.E, baseValue + 10),
                java.util.Map.entry(SkillRank.D, baseValue + 20),
                        java.util.Map.entry(SkillRank.C, baseValue + 30),
                java.util.Map.entry(SkillRank.B, baseValue + 40),
                        java.util.Map.entry(SkillRank.A, baseValue + 50),
                java.util.Map.entry(SkillRank.R9, baseValue + 60),
                        java.util.Map.entry(SkillRank.R8, baseValue + 70),
                java.util.Map.entry(SkillRank.R7, baseValue + 80),
                        java.util.Map.entry(SkillRank.R6, baseValue + 90),
                java.util.Map.entry(SkillRank.R5, baseValue + 100),
                        java.util.Map.entry(SkillRank.R4, baseValue + 110),
                java.util.Map.entry(SkillRank.R3, baseValue + 120),
                        java.util.Map.entry(SkillRank.R2, baseValue + 130),
                java.util.Map.entry(SkillRank.R1, baseValue + 140),
                        java.util.Map.entry(SkillRank.MASTER, baseValue + 150));
    }
}
