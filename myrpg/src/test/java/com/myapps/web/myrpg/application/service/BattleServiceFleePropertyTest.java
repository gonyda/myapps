package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 도망 판정을 검증하는 프로퍼티 테스트.
 *
 * <p>50% 성공(active=false, outcome=FLED), 실패(몬스터 1회 피해, 전투 유지),
 * 실패로 HP 0이면 사망 처리로 이어지는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 13: 도망 판정
 *
 * <p><b>Validates: Requirements 12.3, 12.4, 12.5, 12.6</b>
 */
class BattleServiceFleePropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "raccoon";
    private static final int MONSTER_MAX_HP = 200;
    private static final int MONSTER_DAMAGE = 15;
    private static final int FLEE_THRESHOLD = 50;

    /**
     * 도망 성공 시 active=false, outcome=FLED를 검증한다.
     * random.nextInt(100) < 50 이면 도망 성공.
     *
     * @param successSeed 도망 성공을 유발하는 시드
     */
    @Property(tries = 100)
    void should_fleeSuccessfully_when_rollBelow50(@ForAll("successSeeds") final long successSeed) {
        final Random testRandom = new Random(successSeed);
        final int roll = testRandom.nextInt(100);
        if (roll >= FLEE_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(successSeed);
        final BattleService service = createService(serviceRandom);

        final CharacterProgress progress = createProgress(200);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.flee(progress, state);

        assertThat(result.outcome())
                .as("도망 성공 시 outcome은 FLED여야 한다")
                .isEqualTo(Outcome.FLED);
        assertThat(result.battleEnded())
                .as("도망 성공 시 전투가 종료되어야 한다")
                .isTrue();
        assertThat(state.isActive())
                .as("도망 성공 시 BattleState active는 false여야 한다")
                .isFalse();
    }

    /**
     * 도망 실패 시 몬스터 1회 피해 적용 후 전투 유지를 검증한다.
     * random.nextInt(100) >= 50 이면 도망 실패.
     *
     * @param failSeed 도망 실패를 유발하는 시드
     */
    @Property(tries = 100)
    void should_takeDamageAndContinue_when_fleeFail(@ForAll("failSeeds") final long failSeed) {
        final Random testRandom = new Random(failSeed);
        final int roll = testRandom.nextInt(100);
        if (roll < FLEE_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(failSeed);
        final BattleService service = createService(serviceRandom);

        final int initialHp = 200;
        final CharacterProgress progress = createProgress(initialHp);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.flee(progress, state);

        assertThat(result.monsterDamage())
                .as("도망 실패 시 몬스터 1회 피해가 있어야 한다")
                .isGreaterThan(0);
        assertThat(result.outcome())
                .as("도망 실패 시 HP가 남아있으면 전투 계속(NONE)")
                .isEqualTo(Outcome.NONE);
        assertThat(state.isActive())
                .as("도망 실패 시 전투가 계속되어야 한다")
                .isTrue();
    }

    /**
     * 도망 실패로 HP가 0이 되면 사망 처리(outcome=LOSE)를 검증한다.
     *
     * @param failSeed 도망 실패를 유발하는 시드
     */
    @Property(tries = 100)
    void should_triggerDeath_when_fleeFailAndHpReachesZero(@ForAll("failSeeds") final long failSeed) {
        final Random testRandom = new Random(failSeed);
        final int roll = testRandom.nextInt(100);
        if (roll < FLEE_THRESHOLD) {
            return;
        }

        final Random serviceRandom = new Random(failSeed);
        final BattleService service = createServiceWithDeath(serviceRandom);

        final int veryLowHp = 1;
        final CharacterProgress progress = createProgress(veryLowHp);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = service.flee(progress, state);

        assertThat(result.outcome())
                .as("도망 실패 + HP 0에서 outcome은 LOSE여야 한다")
                .isEqualTo(Outcome.LOSE);
        assertThat(state.isActive())
                .as("사망 시 BattleState active는 false여야 한다")
                .isFalse();
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 도망 성공 가능성이 있는 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> successSeeds() {
        return Arbitraries.longs().between(0L, 50000L);
    }

    /**
     * 도망 실패 가능성이 있는 시드 생성기.
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> failSeeds() {
        return Arbitraries.longs().between(0L, 50000L);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private BattleService createService(final Random random) {
        return buildService(random, false);
    }

    private BattleService createServiceWithDeath(final Random random) {
        return buildService(random, true);
    }

    private BattleService buildService(final Random random, final boolean deathScenario) {
        final BattleStateRepository battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BattleResolver resolver = mock(BattleResolver.class);
        when(resolver.baseDamage(anyInt(), anyInt(), anyInt())).thenReturn(MONSTER_DAMAGE);
        when(resolver.rollCritical(anyInt())).thenReturn(false);
        when(resolver.finalDamage(anyInt(), anyDouble(), any(Boolean.class))).thenReturn(MONSTER_DAMAGE);

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
        if (deathScenario) {
            when(progressionService.die(any(CharacterProgress.class)))
                    .thenReturn(new DeathResult(10L));
        }

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);

        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepo = mock(CharacterSkillRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        return new BattleService(
                battleStateRepo, resolver, monsterService, aiService,
                rewardService, skillService, inventoryService, progressionService,
                characterService, statProgression, actionLog, random,
                skillCatalogService, characterSkillRepo, itemCatalogService);
    }

    private CharacterProgress createProgress(final int hp) {
        return new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null,
                hp, 100, 100, "dunbarton", 0, 500L);
    }

    private Monster createMonster() {
        return new Monster(
                MONSTER_ID, "너구리", MonsterType.NORMAL, 5, MONSTER_MAX_HP,
                20, 5, 50, 30L, new GoldDrop(10, 20), List.of(), List.of("소리", "행동1", "행동2"));
    }
}
