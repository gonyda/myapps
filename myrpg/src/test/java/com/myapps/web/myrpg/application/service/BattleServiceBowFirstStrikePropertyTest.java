package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
 * 활 1턴 선제 규칙을 검증하는 프로퍼티 테스트.
 *
 * <p>{@code turnCount==1} + ARCHERY 재능이면 몬스터 피해 0, 플레이어 100% 적중, firstStrike 플래그 true. {@code
 * turnCount!=1}이거나 비활 재능이면 발동하지 않는다.
 *
 * <p>Feature: 008-battle-system, Property 8: 활 1턴 선제
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.6</b>
 */
class BattleServiceBowFirstStrikePropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final String BOW_SKILL_ID = "magnum-shot";
    private static final String MELEE_SKILL_ID = "windmill";
    private static final int MONSTER_MAX_HP = 200;
    private static final int HIGH_HP = 500;

    /**
     * turnCount==1 + ARCHERY 스킬일 때 선제 사격이 발동하는지 검증한다.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_triggerFirstStrike_when_bowAndTurnCountOne(@ForAll("seeds") final long seed) {
        final Random random = new Random(seed);
        final BattleService service = createServiceWithBowSkill(random);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.takeTurn(progress, state, BOW_SKILL_ID);

        assertThat(result.firstStrike())
                .as("turnCount==1 + ARCHERY에서 firstStrike 플래그가 true여야 한다")
                .isTrue();
        assertThat(result.monsterDamage()).as("선제 사격 시 몬스터 피해(플레이어에게)는 0이어야 한다").isEqualTo(0);
        assertThat(result.playerDamage()).as("선제 사격 시 플레이어 피해(몬스터에게)는 0보다 커야 한다").isGreaterThan(0);
    }

    /**
     * turnCount!=1 (2턴 이상)일 때 활 스킬이라도 선제가 발동하지 않는지 검증한다.
     *
     * @param seed 랜덤 시드
     * @param turnCount 2 이상의 턴 카운트
     */
    @Property(tries = 100)
    void should_notTriggerFirstStrike_when_turnCountNotOne(
            @ForAll("seeds") final long seed, @ForAll("turnCountsAboveOne") final int turnCount) {
        final Random random = new Random(seed);
        final BattleService service = createServiceWithBowSkill(random);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(turnCount);

        final BattleTurnResult result = service.takeTurn(progress, state, BOW_SKILL_ID);

        assertThat(result.firstStrike()).as("turnCount > 1이면 firstStrike는 false여야 한다").isFalse();
    }

    /**
     * 비활(근접) 재능 스킬일 때 turnCount==1이라도 선제가 발동하지 않는지 검증한다.
     *
     * @param seed 랜덤 시드
     */
    @Property(tries = 100)
    void should_notTriggerFirstStrike_when_nonArcheryTalent(@ForAll("seeds") final long seed) {
        final Random random = new Random(seed);
        final BattleService service = createServiceWithMeleeSkill(random);

        final CharacterProgress progress = createProgress();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.takeTurn(progress, state, MELEE_SKILL_ID);

        assertThat(result.firstStrike())
                .as("비활 재능 + turnCount==1이라도 firstStrike는 false여야 한다")
                .isFalse();
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

    /**
     * 2 이상의 턴 카운트 생성기.
     *
     * @return 턴 카운트 Arbitrary
     */
    @Provide
    Arbitrary<Integer> turnCountsAboveOne() {
        return Arbitraries.integers().between(2, 20);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private BattleService createServiceWithBowSkill(final Random random) {
        return buildService(random, createBowSkill(), BOW_SKILL_ID);
    }

    private BattleService createServiceWithMeleeSkill(final Random random) {
        return buildService(random, createMeleeSkill(), MELEE_SKILL_ID);
    }

    private BattleService buildService(
            final Random random, final DamageSkill skill, final String skillId) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.baseDamage(anyInt(), anyInt(), anyInt())).thenReturn(20);
        when(resolver.rollCritical(anyInt())).thenReturn(false);
        when(resolver.finalDamage(anyInt(), anyDouble(), any(Boolean.class))).thenReturn(20);
        when(resolver.multiHitDamage(anyInt(), anyInt(), anyInt(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(new HitResult(20, false)));
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(15, 10, false, false, false, false, List.of()));

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
                itemCatalogService);
    }

    private CharacterProgress createProgress() {
        return new CharacterProgress(
                "궁수",
                10,
                10,
                100L,
                TalentType.ARCHERY,
                null,
                HIGH_HP,
                100,
                100,
                "dunbarton",
                0,
                500L);
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

    private DamageSkill createBowSkill() {
        return new DamageSkill(
                BOW_SKILL_ID,
                "매그넘 샷",
                SkillType.NORMAL,
                SkillTalent.ARCHERY,
                8,
                createFullRankMap(100),
                "활 테스트 스킬");
    }

    private DamageSkill createMeleeSkill() {
        return new DamageSkill(
                MELEE_SKILL_ID,
                "윈드밀",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                5,
                createFullRankMap(100),
                "근접 테스트 스킬");
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
