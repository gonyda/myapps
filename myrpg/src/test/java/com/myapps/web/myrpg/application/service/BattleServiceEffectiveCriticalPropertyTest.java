package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
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
import com.myapps.web.myrpg.domain.model.VitalMax;
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
import org.mockito.ArgumentCaptor;

/**
 * 실효 크리티컬 = 캐릭터 크리 + critBonus (상한 1000) 프로퍼티 검증.
 *
 * <p>딜 스킬의 경우 캐릭터 기본 크리티컬에 스킬의 critBonus를 합산하고 상한 1000으로 보정, 방어 스킬의 경우 보너스를 가산하지 않으며, 몬스터 크리티컬은 스킬
 * critBonus에 영향받지 않는다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 5: 실효 크리 = 캐릭터 크리 + critBonus(상한)
 *
 * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5</b>
 */
class BattleServiceEffectiveCriticalPropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "test_monster";
    private static final String DAMAGE_SKILL_ID = "test_damage_skill";
    private static final String DEFENSE_SKILL_ID = "test_defense_skill";
    private static final int MONSTER_MAX_HP = 200;
    private static final int MONSTER_CRITICAL = 80;
    private static final int HIGH_HP = 500;
    private static final int CRITICAL_ROLL_MAX = 1000;

    /**
     * 딜 스킬의 실효 크리 = min(1000, 캐릭터 크리 + critBonus)를 검증한다.
     *
     * @param baseCritical 캐릭터 기본 크리티컬 (장비/스킬 보너스 없이 레벨 스탯만)
     * @param critBonus 딜 스킬의 critBonus (0~100)
     */
    @Property(tries = 100)
    void should_addCritBonus_when_damageSkill(
            @ForAll("baseCriticals") final int baseCritical,
            @ForAll("critBonuses") final int critBonus) {

        final BattleResolver resolver = mock(BattleResolver.class);
        final ArgumentCaptor<TurnInput> captor = ArgumentCaptor.forClass(TurnInput.class);

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

        final DamageSkill skill = createDamageSkillWithCritBonus(critBonus);
        final BattleService service =
                buildService(new Random(42L), resolver, skill, DAMAGE_SKILL_ID, baseCritical);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, DAMAGE_SKILL_ID);

        verify(resolver).resolve(captor.capture());
        final TurnInput captured = captor.getValue();

        final int expectedCritical = Math.min(CRITICAL_ROLL_MAX, baseCritical + critBonus);
        assertThat(captured.playerCritical())
                .as(
                        "실효 크리 = min(1000, baseCritical(%d) + critBonus(%d)) = %d",
                        baseCritical, critBonus, expectedCritical)
                .isEqualTo(expectedCritical);
    }

    /**
     * 방어 스킬 사용 시 critBonus가 가산되지 않음을 검증한다.
     *
     * @param baseCritical 캐릭터 기본 크리티컬
     */
    @Property(tries = 100)
    void should_notAddCritBonus_when_defenseSkill(@ForAll("baseCriticals") final int baseCritical) {

        final BattleResolver resolver = mock(BattleResolver.class);
        final ArgumentCaptor<TurnInput> captor = ArgumentCaptor.forClass(TurnInput.class);

        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(0, 5, false, false, false, false, List.of()));

        final DefenseSkill skill = createDefenseSkill();
        final BattleService service =
                buildServiceWithDefenseSkill(
                        new Random(42L), resolver, skill, DEFENSE_SKILL_ID, baseCritical);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, DEFENSE_SKILL_ID);

        verify(resolver).resolve(captor.capture());
        final TurnInput captured = captor.getValue();

        assertThat(captured.playerCritical())
                .as("방어 스킬은 critBonus를 가산하지 않으므로 기본 크리티컬(%d)과 같아야 한다", baseCritical)
                .isEqualTo(baseCritical);
    }

    /**
     * 몬스터 크리티컬이 스킬의 critBonus에 영향받지 않음을 검증한다.
     *
     * @param critBonus 딜 스킬의 critBonus
     */
    @Property(tries = 100)
    void should_notAffectMonsterCritical_when_playerHasCritBonus(
            @ForAll("critBonuses") final int critBonus) {

        final BattleResolver resolver = mock(BattleResolver.class);
        final ArgumentCaptor<TurnInput> captor = ArgumentCaptor.forClass(TurnInput.class);

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

        final DamageSkill skill = createDamageSkillWithCritBonus(critBonus);
        final BattleService service =
                buildService(new Random(42L), resolver, skill, DAMAGE_SKILL_ID, 100);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, DAMAGE_SKILL_ID);

        verify(resolver).resolve(captor.capture());
        final TurnInput captured = captor.getValue();

        assertThat(captured.monsterCritical())
                .as("몬스터 크리티컬(%d)은 플레이어 스킬 critBonus(%d)에 영향받지 않아야 한다", MONSTER_CRITICAL, critBonus)
                .isEqualTo(MONSTER_CRITICAL);
    }

    /**
     * 합계가 1000을 초과하면 1000으로 캡되는지 검증한다.
     *
     * @param baseCritical 기본 크리티컬 (950~1000 범위, 합계 초과 유도)
     * @param critBonus critBonus (50~100 범위)
     */
    @Property(tries = 100)
    void should_capAt1000_when_sumExceedsMax(
            @ForAll("highBaseCriticals") final int baseCritical,
            @ForAll("highCritBonuses") final int critBonus) {

        final BattleResolver resolver = mock(BattleResolver.class);
        final ArgumentCaptor<TurnInput> captor = ArgumentCaptor.forClass(TurnInput.class);

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

        final DamageSkill skill = createDamageSkillWithCritBonus(critBonus);
        final BattleService service =
                buildService(new Random(42L), resolver, skill, DAMAGE_SKILL_ID, baseCritical);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, DAMAGE_SKILL_ID);

        verify(resolver).resolve(captor.capture());
        final TurnInput captured = captor.getValue();

        assertThat(captured.playerCritical())
                .as(
                        "합계(%d + %d = %d)가 1000 초과 시 캡되어야 한다",
                        baseCritical, critBonus, baseCritical + critBonus)
                .isEqualTo(CRITICAL_ROLL_MAX);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 캐릭터 기본 크리티컬 생성기 (일반 범위).
     *
     * @return 기본 크리티컬 Arbitrary (50~900)
     */
    @Provide
    Arbitrary<Integer> baseCriticals() {
        return Arbitraries.integers().between(50, 900);
    }

    /**
     * critBonus 생성기 (0~100).
     *
     * @return critBonus Arbitrary
     */
    @Provide
    Arbitrary<Integer> critBonuses() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * 높은 기본 크리티컬 생성기 (상한 초과 유도).
     *
     * @return 높은 기본 크리티컬 Arbitrary (950~1000)
     */
    @Provide
    Arbitrary<Integer> highBaseCriticals() {
        return Arbitraries.integers().between(950, 1000);
    }

    /**
     * 높은 critBonus 생성기 (상한 초과 유도).
     *
     * @return 높은 critBonus Arbitrary (50~100)
     */
    @Provide
    Arbitrary<Integer> highCritBonuses() {
        return Arbitraries.integers().between(50, 100);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private BattleService buildService(
            final Random random,
            final BattleResolver resolver,
            final DamageSkill skill,
            final String skillId,
            final int baseCritical) {
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

        final StatProgression statProgression = mock(StatProgression.class);
        when(statProgression.levelStatsFor(anyInt(), any(TalentType.class))).thenReturn(Stats.ZERO);

        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(skillId)).thenReturn(Optional.of(skill));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, skillId, SkillRank.F, 0, 0)));

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

    private BattleService buildServiceWithDefenseSkill(
            final Random random,
            final BattleResolver resolver,
            final DefenseSkill skill,
            final String skillId,
            final int baseCritical) {
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

        final StatProgression statProgression = mock(StatProgression.class);
        when(statProgression.levelStatsFor(anyInt(), any(TalentType.class))).thenReturn(Stats.ZERO);

        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(skillId)).thenReturn(Optional.of(skill));

        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, skillId, SkillRank.F, 0, 0)));

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

    private CharacterProgress createProgress() {
        return new CharacterProgress(
                "전사", 1, 1, 100L, TalentType.MELEE, null, HIGH_HP, 100, 100, "dunbarton", 0, 500L);
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID,
                "테스트 몬스터",
                MonsterType.NORMAL,
                5,
                MONSTER_MAX_HP,
                20,
                5,
                MONSTER_CRITICAL,
                30L,
                new GoldDrop(10, 20),
                List.of(),
                List.of("대사1", "대사2", "대사3"));
    }

    private DamageSkill createDamageSkillWithCritBonus(final int critBonus) {
        return new DamageSkill(
                DAMAGE_SKILL_ID,
                "테스트 딜 스킬",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                5,
                createFullRankMap(100),
                "딜 스킬 설명",
                1,
                critBonus);
    }

    private DefenseSkill createDefenseSkill() {
        return new DefenseSkill(
                DEFENSE_SKILL_ID,
                "테스트 방어",
                SkillType.DEFENSE,
                SkillTalent.COMMON,
                5,
                createFullRankMap(30),
                createFullRankMap(20),
                "방어 스킬 설명");
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
                        Map.entry(SkillRank.MASTER, baseValue + 75));
    }
}
