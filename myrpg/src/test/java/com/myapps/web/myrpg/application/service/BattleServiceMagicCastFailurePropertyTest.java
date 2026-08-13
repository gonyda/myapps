package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 마법 캐스팅 실패를 검증하는 프로퍼티 테스트.
 *
 * <p>공격 마법 스킬에서 10% 확률로 캐스팅 실패하면 플레이어 피해 0, MP 소모,
 * 몬스터 행동 정상 처리. 방어(공통) 스킬은 실패하지 않는다.
 *
 * <p>Feature: 008-battle-system, Property 9: 마법 캐스팅 실패
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 9.5</b>
 */
class BattleServiceMagicCastFailurePropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String MAGIC_SKILL_ID = "firebolt";
    private static final String DEFENSE_SKILL_ID = "defense";
    private static final int MONSTER_MAX_HP = 200;
    private static final int HIGH_HP = 500;
    private static final int INITIAL_MP = 100;
    private static final int MAGIC_COST = 10;
    private static final int MAGIC_FAIL_THRESHOLD = 10;

    /**
     * 시드가 캐스팅 실패를 유발하는 경우 플레이어 피해 0, castFailure=true를 검증한다.
     * Random.nextInt(100) < 10 이면 캐스팅 실패.
     *
     * @param failSeed 실패를 유발하는 시드 (random.nextInt(100) < 10을 만족하도록 선별)
     */
    @Property(tries = 100)
    void should_castFail_when_magicAttackAndRandomBelow10(@ForAll("failSeeds") final long failSeed) {
        final Random testRandom = new Random(failSeed);
        final int firstRoll = testRandom.nextInt(100);
        if (firstRoll >= MAGIC_FAIL_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(failSeed);
        final BattleService service = createServiceWithMagicSkill(serviceRandom);

        final CharacterProgress progress = createProgress(INITIAL_MP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, MAGIC_SKILL_ID);

        assertThat(result.castFailure())
                .as("마법 캐스팅 실패 시 castFailure 플래그가 true여야 한다")
                .isTrue();
        assertThat(result.playerDamage())
                .as("캐스팅 실패 시 플레이어가 몬스터에게 주는 피해는 0이어야 한다")
                .isEqualTo(0);
    }

    /**
     * 캐스팅 실패 시에도 MP가 소모되는지 검증한다.
     *
     * @param failSeed 실패를 유발하는 시드
     */
    @Property(tries = 100)
    void should_consumeMP_when_castFailure(@ForAll("failSeeds") final long failSeed) {
        final Random testRandom = new Random(failSeed);
        final int firstRoll = testRandom.nextInt(100);
        if (firstRoll >= MAGIC_FAIL_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(failSeed);
        final BattleService service = createServiceWithMagicSkill(serviceRandom);

        final CharacterProgress progress = createProgress(INITIAL_MP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, MAGIC_SKILL_ID);

        assertThat(progress.getMpCurrent())
                .as("캐스팅 실패에도 MP는 소모되어야 한다")
                .isEqualTo(INITIAL_MP - MAGIC_COST);
    }

    /**
     * 방어(DEFENSE) 스킬은 MAGIC 재능이라도 캐스팅 실패하지 않음을 검증한다.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_neverCastFail_when_defenseSkill(@ForAll("seeds") final long seed) {
        final Random random = new Random(seed);
        final BattleService service = createServiceWithDefenseSkill(random);

        final CharacterProgress progress = createProgress(INITIAL_MP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, DEFENSE_SKILL_ID);

        assertThat(result.castFailure())
                .as("방어 스킬은 캐스팅 실패하지 않아야 한다")
                .isFalse();
    }

    /**
     * 캐스팅 실패 시 몬스터는 정상적으로 행동(피해를 줄 수 있음)하는지 검증한다.
     *
     * @param failSeed 실패를 유발하는 시드
     */
    @Property(tries = 100)
    void should_monsterActNormally_when_castFailure(@ForAll("failSeeds") final long failSeed) {
        final Random testRandom = new Random(failSeed);
        final int firstRoll = testRandom.nextInt(100);
        if (firstRoll >= MAGIC_FAIL_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(failSeed);
        final BattleService service = createServiceWithMagicSkillAndMonsterAttack(serviceRandom);

        final CharacterProgress progress = createProgress(INITIAL_MP);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, MAGIC_SKILL_ID);

        if (result.castFailure() && result.monsterAction() != SkillType.DEFENSE) {
            assertThat(result.monsterDamage())
                    .as("캐스팅 실패 시에도 몬스터가 공격이면 피해가 있어야 한다")
                    .isGreaterThan(0);
        }
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 캐스팅 실패를 유발할 수 있는 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> failSeeds() {
        return Arbitraries.longs().between(0L, 50000L);
    }

    /**
     * 일반 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs().between(0L, 10000L);
    }

    // ─── Helper methods ─────────────────────────────────────────────────────

    private BattleService createServiceWithMagicSkill(final Random random) {
        return buildService(random, createMagicDamageSkill(), MAGIC_SKILL_ID, SkillType.DEFENSE);
    }

    private BattleService createServiceWithMagicSkillAndMonsterAttack(final Random random) {
        return buildService(random, createMagicDamageSkill(), MAGIC_SKILL_ID, SkillType.NORMAL);
    }

    private BattleService createServiceWithDefenseSkill(final Random random) {
        return buildDefenseService(random);
    }

    private BattleService buildService(final Random random,
                                       final DamageSkill skill,
                                       final String skillId,
                                       final SkillType monsterAction) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.baseDamage(anyInt(), anyInt(), anyInt())).thenReturn(15);
        when(resolver.rollCritical(anyInt())).thenReturn(false);
        when(resolver.finalDamage(anyInt(), anyDouble(), any(Boolean.class))).thenReturn(15);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(15, 10, false, false, false, false, List.of()));

        final MonsterService monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(monsterAction);

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

    private BattleService buildDefenseService(final Random random) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(5, 0, false, false, true, true, List.of()));

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
        final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(DEFENSE_SKILL_ID)).thenReturn(Optional.of(createDefenseSkill()));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(Optional.of(new CharacterSkill(CHARACTER_ID, DEFENSE_SKILL_ID, SkillRank.F, 0, 0)));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        return new BattleService(
                battleStateRepo, resolver, monsterService, aiService,
                rewardService, skillService, inventoryService, progressionService,
                characterService, statProgression, actionLog, random,
                skillCatalogService, characterSkillRepo, itemCatalogService);
    }

    private CharacterProgress createProgress(final int mp) {
        return new CharacterProgress(
                "마법사", 10, 10, 100L, TalentType.MAGIC, null,
                HIGH_HP, mp, 100, "dunbarton", 0, 500L);
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID, "너구리", MonsterType.NORMAL, 5, MONSTER_MAX_HP,
                20, 5, 50, 30L, new GoldDrop(10, 20), List.of(), List.of("소리", "행동1", "행동2"));
    }

    private DamageSkill createMagicDamageSkill() {
        return new DamageSkill(
                MAGIC_SKILL_ID, "파이어볼트", SkillType.NORMAL, SkillTalent.MAGIC, MAGIC_COST,
                createFullRankMap(100),
                "마법 테스트 스킬");
    }

    private DefenseSkill createDefenseSkill() {
        return new DefenseSkill(
                DEFENSE_SKILL_ID, "디펜스", SkillType.DEFENSE, SkillTalent.COMMON, 5,
                createFullRankMap(30),
                createFullRankMap(20),
                "방어 테스트 스킬");
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
                Map.entry(SkillRank.R1, baseValue + 70), Map.entry(SkillRank.MASTER, baseValue + 75));
    }
}
