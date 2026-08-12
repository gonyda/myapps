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
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.ResourceKind;
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
 * 자원 소모·부족 규칙을 검증하는 프로퍼티 테스트.
 *
 * <p>자원이 비용 미만이면 턴 미진행(resourceInsufficient=true, 피해 없음, 자원 불변),
 * 충분하면 정확히 비용만큼 차감되는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 10: 자원 소모·부족
 *
 * <p><b>Validates: Requirements 9.1, 9.2, 9.4</b>
 */
class BattleServiceResourcePropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String MELEE_SKILL_ID = "windmill";
    private static final String MAGIC_SKILL_ID = "firebolt";
    private static final int MONSTER_MAX_HP = 200;
    private static final int HIGH_HP = 500;
    private static final int STAMINA_COST = 10;
    private static final int MP_COST = 15;

    /**
     * 스태미나 부족 시 턴 미진행(resourceInsufficient=true, 피해 0)을 검증한다.
     *
     * @param currentStamina 현재 스태미나 (비용 미만)
     */
    @Property(tries = 100)
    void should_returnInsufficient_when_staminaBelowCost(
            @ForAll("insufficientStamina") final int currentStamina) {
        final Random random = new Random(42L);
        final BattleService service = createMeleeService(random);

        final CharacterProgress progress = new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null,
                HIGH_HP, 100, currentStamina, "dunbarton", 0, 500L);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, MELEE_SKILL_ID);

        assertThat(result.resourceInsufficient())
                .as("스태미나 부족 시 resourceInsufficient 플래그가 true여야 한다")
                .isTrue();
        assertThat(result.insufficientKind())
                .as("부족한 자원 종류는 STAMINA여야 한다")
                .isEqualTo(ResourceKind.STAMINA);
        assertThat(result.playerDamage())
                .as("자원 부족 시 플레이어 피해는 0이어야 한다")
                .isEqualTo(0);
        assertThat(result.monsterDamage())
                .as("자원 부족 시 몬스터 피해도 0이어야 한다")
                .isEqualTo(0);
        assertThat(progress.getStaminaCurrent())
                .as("자원 부족 시 스태미나는 변하지 않아야 한다")
                .isEqualTo(currentStamina);
    }

    /**
     * MP 부족 시 턴 미진행(resourceInsufficient=true)을 검증한다.
     *
     * @param currentMp 현재 MP (비용 미만)
     */
    @Property(tries = 100)
    void should_returnInsufficient_when_mpBelowCost(
            @ForAll("insufficientMp") final int currentMp) {
        final Random random = new Random(99L);
        final BattleService service = createMagicService(random);

        final CharacterProgress progress = new CharacterProgress(
                "마법사", 10, 10, 100L, TalentType.MAGIC, null,
                HIGH_HP, currentMp, 100, "dunbarton", 0, 500L);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        final BattleTurnResult result = service.takeTurn(progress, state, MAGIC_SKILL_ID);

        assertThat(result.resourceInsufficient())
                .as("MP 부족 시 resourceInsufficient 플래그가 true여야 한다")
                .isTrue();
        assertThat(result.insufficientKind())
                .as("부족한 자원 종류는 MP여야 한다")
                .isEqualTo(ResourceKind.MP);
        assertThat(progress.getMpCurrent())
                .as("자원 부족 시 MP는 변하지 않아야 한다")
                .isEqualTo(currentMp);
    }

    /**
     * 스태미나가 충분할 때 정확히 비용만큼 차감되는지 검증한다.
     *
     * @param currentStamina 충분한 스태미나
     */
    @Property(tries = 100)
    void should_deductExactCost_when_staminaSufficient(
            @ForAll("sufficientStamina") final int currentStamina) {
        final Random random = new Random(42L);
        final BattleService service = createMeleeService(random);

        final CharacterProgress progress = new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null,
                HIGH_HP, 100, currentStamina, "dunbarton", 0, 500L);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, MELEE_SKILL_ID);

        assertThat(progress.getStaminaCurrent())
                .as("스태미나는 정확히 비용만큼 차감되어야 한다")
                .isEqualTo(currentStamina - STAMINA_COST);
    }

    /**
     * MP가 충분할 때 정확히 비용만큼 차감되는지 검증한다.
     *
     * @param currentMp 충분한 MP
     */
    @Property(tries = 100)
    void should_deductExactCost_when_mpSufficient(
            @ForAll("sufficientMp") final int currentMp) {
        final Random random = new Random(99L);
        final BattleService service = createMagicService(random);

        final CharacterProgress progress = new CharacterProgress(
                "마법사", 10, 10, 100L, TalentType.MAGIC, null,
                HIGH_HP, currentMp, 100, "dunbarton", 0, 500L);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);

        service.takeTurn(progress, state, MAGIC_SKILL_ID);

        assertThat(progress.getMpCurrent())
                .as("MP는 정확히 비용만큼 차감되어야 한다")
                .isEqualTo(currentMp - MP_COST);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 스태미나 비용(10) 미만 값 생성기.
     *
     * @return 스태미나 Arbitrary
     */
    @Provide
    Arbitrary<Integer> insufficientStamina() {
        return Arbitraries.integers().between(0, STAMINA_COST - 1);
    }

    /**
     * MP 비용(15) 미만 값 생성기.
     *
     * @return MP Arbitrary
     */
    @Provide
    Arbitrary<Integer> insufficientMp() {
        return Arbitraries.integers().between(0, MP_COST - 1);
    }

    /**
     * 스태미나 비용(10) 이상 값 생성기.
     *
     * @return 스태미나 Arbitrary
     */
    @Provide
    Arbitrary<Integer> sufficientStamina() {
        return Arbitraries.integers().between(STAMINA_COST, 200);
    }

    /**
     * MP 비용(15) 이상 값 생성기.
     *
     * @return MP Arbitrary
     */
    @Provide
    Arbitrary<Integer> sufficientMp() {
        return Arbitraries.integers().between(MP_COST, 200);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private BattleService createMeleeService(final Random random) {
        return buildService(random, createMeleeDamageSkill(), MELEE_SKILL_ID);
    }

    private BattleService createMagicService(final Random random) {
        return buildService(random, createMagicDamageSkill(), MAGIC_SKILL_ID);
    }

    private BattleService buildService(final Random random,
                                       final DamageSkill skill,
                                       final String skillId) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(10, 5, false, false, false, false));

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

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID, "너구리", MonsterType.NORMAL, 5, MONSTER_MAX_HP,
                20, 5, 50, 30L, new GoldDrop(10, 20), List.of(), List.of("소리", "행동1", "행동2"));
    }

    private DamageSkill createMeleeDamageSkill() {
        return new DamageSkill(
                MELEE_SKILL_ID, "윈드밀", SkillType.NORMAL, SkillTalent.MELEE, STAMINA_COST,
                createFullRankMap(100),
                "근접 테스트 스킬");
    }

    private DamageSkill createMagicDamageSkill() {
        return new DamageSkill(
                MAGIC_SKILL_ID, "파이어볼트", SkillType.NORMAL, SkillTalent.MAGIC, MP_COST,
                createFullRankMap(100),
                "마법 테스트 스킬");
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
                Map.entry(SkillRank.R1, baseValue + 140), Map.entry(SkillRank.MASTER, baseValue + 150));
    }
}
