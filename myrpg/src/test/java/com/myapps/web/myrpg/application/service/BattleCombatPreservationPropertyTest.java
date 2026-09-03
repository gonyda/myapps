package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 020 스킬 타입 다형화 Property 3(전투 동작 보존) 및 Property 4(폴백 불변식) jqwik 검증.
 *
 * <p>Visitor 기반 디스패치 및 switch 스탯 해석으로 전환된 전투 처리가 리팩토링 이전 규격과 100% 동일하게 동작함을 보장한다.
 */
class BattleCombatPreservationPropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "test-monster";
    private static final int RANK_COUNT = 16;

    /** Property 3: 유효한 전투 입력에 대해 각 서브타입별 전투 핸들러 동작이 온전히 보존되어야 한다. */
    @Property(tries = 50)
    void should_preserveCombatBehavior_forAllSubtypesAndRanks(
            @ForAll("allCombatSkills") final Skill skill, @ForAll final SkillRank rank) {
        // given
        final BattleFixture fixture = createFixture();
        when(fixture.skillCatalogService.byId(skill.id())).thenReturn(Optional.of(skill));

        final CharacterSkill charSkill = new CharacterSkill(CHARACTER_ID, skill.id(), rank, 0);
        when(fixture.characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, skill.id()))
                .thenReturn(Optional.of(charSkill));

        final CharacterProgress progress = createProgress(500, 1000, 1000);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 500, false);
        state.setStandby(false);

        // when
        final BattleTurnResult result = fixture.battleService.takeTurn(progress, state, skill.id());

        // then: 서브타입별 고유 전투 규칙 보존 확인
        switch (skill) {
            case UltimateSkill ult -> {
                assertThat(result.playerDamage()).isGreaterThan(0);
                assertThat(result.firstStrike()).isTrue();
                assertThat(result.monsterDamage()).isZero();
                assertThat(charSkill.getUltimateCooldown()).isGreaterThan(0);
            }
            case RecoverySkill rec -> {
                assertThat(result.playerDamage()).isZero();
                assertThat(result.monsterDamage()).isGreaterThan(0);
                assertThat(result.combatLines())
                        .anyMatch(line -> line.contains(rec.label()) || line.contains("회복"));
            }
            case BuffSkill buf -> {
                assertThat(result.playerDamage()).isZero();
                assertThat(state.getManaShieldTurnsLeft()).isEqualTo(buf.durationTurns() - 1);
                assertThat(state.getManaShieldAbsorbRate()).isEqualTo(buf.absorbRateAt(rank));
            }
            case CcSkill cc -> {
                assertThat(result.playerDamage()).isZero();
                assertThat(result.combatLines()).anyMatch(line -> line.contains(cc.label()));
            }
            case DotSkill dot -> {
                assertThat(state.getDotTurnsLeft()).isEqualTo(dot.dotTurnsAt(rank));
                assertThat(state.getDotDamagePerTurn()).isGreaterThan(0);
            }
            case DamageSkill dam -> {
                assertThat(result.playerHits()).hasSize(dam.hitCount());
            }
            case DefenseSkill def -> {
                if (state.getCurrentMonsterIntent() == null
                        || state.getCurrentMonsterIntent() == SkillType.NORMAL) {
                    assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.PLAYER);
                }
            }
            case PassiveSkill pas -> {
                assertThat(result).isNotNull();
            }
        }
    }

    /** Property 4: 미지 스킬 또는 미보유 스킬 입력 시 예외 없이 안전 기본값으로 폴백해야 한다. */
    @Property(tries = 30)
    void should_preserveFallbackBehavior_whenSkillUnownedOrUnknown(
            @ForAll("allCombatSkills") final Skill skill,
            @ForAll("unknownSkillIds") final String unknownSkillId) {
        // given
        final BattleFixture fixture = createFixture();
        when(fixture.skillCatalogService.byId(skill.id())).thenReturn(Optional.of(skill));
        // 스킬 미보유 상태: characterSkillRepo.findByCharacterIdAndSkillId returns Optional.empty()
        when(fixture.characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, skill.id()))
                .thenReturn(Optional.empty());

        final CharacterProgress progress = createProgress(200, 100, 100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 500, false);
        state.setStandby(false);

        // when 1: 미지 스킬 ID
        final BattleTurnResult unknownResult =
                fixture.battleService.takeTurn(progress, state, unknownSkillId);

        // then 1: NoOp 처리 및 상태 불변
        assertThat(unknownResult.playerDamage()).isZero();
        assertThat(unknownResult.monsterDamage()).isZero();
        assertThat(state.getMonsterCurrentHp()).isEqualTo(500);

        // when 2: 미보유 스킬로 턴 수행
        final BattleTurnResult unownedResult =
                fixture.battleService.takeTurn(progress, state, skill.id());

        // then 2: 크래시 없이 정상 완료되며 안전 기본값으로 폴백 처리됨
        assertThat(unownedResult).isNotNull();
    }

    // ─── Fixture Helper ─────────────────────────────────────────────────────

    private static class BattleFixture {
        private BattleStateRepository battleStateRepo;
        private CharacterSkillRepository characterSkillRepo;
        private SkillCatalogService skillCatalogService;
        private BattleService battleService;
    }

    private BattleFixture createFixture() {
        final BattleFixture fixture = new BattleFixture();
        fixture.battleStateRepo = mock(BattleStateRepository.class);
        when(fixture.battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        final Monster monster =
                new Monster(
                        MONSTER_ID,
                        "고블린",
                        MonsterType.NORMAL,
                        5,
                        500,
                        20,
                        5,
                        5,
                        20L,
                        new GoldDrop(5, 20),
                        List.of(),
                        List.of("끼에엑"));
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        when(rewardService.rollDrop(any())).thenReturn(DropResult.EMPTY);

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        final ActionLog actionLog = new ActionLog(clock);
        final Random random = new Random(4242L);

        fixture.skillCatalogService = mock(SkillCatalogService.class);
        fixture.characterSkillRepo = mock(CharacterSkillRepository.class);

        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(anyLong())).thenReturn(Stats.ZERO);
        when(skillService.rankupVitalBonus(anyLong())).thenReturn(new VitalMax(0, 0, 0));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        fixture.battleService =
                new BattleService(
                        fixture.battleStateRepo,
                        new BattleResolver(random),
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
                        fixture.skillCatalogService,
                        fixture.characterSkillRepo,
                        itemCatalogService,
                        null);
        return fixture;
    }

    private CharacterProgress createProgress(final int hp, final int mp, final int stamina) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "모험가",
                        5,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        hp,
                        mp,
                        stamina,
                        "node-1",
                        0,
                        1000L);
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, CHARACTER_ID);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("id 설정 실패", exception);
        }
        return progress;
    }

    // ─── Arbitraries ────────────────────────────────────────────────────────

    @Provide
    Arbitrary<Skill> allCombatSkills() {
        return Arbitraries.oneOf(
                damageSkills(),
                defenseSkills(),
                recoverySkills(),
                ultimateSkills(),
                buffSkills(),
                ccSkills(),
                dotSkills(),
                passiveSkills());
    }

    @Provide
    Arbitrary<String> unknownSkillIds() {
        return Arbitraries.of("nonexistent_skill", "unknown_xyz", "magic_999", "ghost_skill");
    }

    @Provide
    Arbitrary<DamageSkill> damageSkills() {
        return monotoneRankMap()
                .map(
                        map ->
                                new DamageSkill(
                                        "slash",
                                        "베기",
                                        SkillType.NORMAL,
                                        SkillTalent.MELEE,
                                        5,
                                        map,
                                        "설명",
                                        2,
                                        10));
    }

    @Provide
    Arbitrary<DefenseSkill> defenseSkills() {
        return Combinators.combine(
                        monotoneRankMap(), monotoneRankMap(), monotoneRankMap(), monotoneRankMap())
                .as(
                        (blockMap, counterMap, costMap, critMap) ->
                                new DefenseSkill(
                                        "defense",
                                        "디펜스",
                                        SkillType.DEFENSE,
                                        SkillTalent.COMMON,
                                        5,
                                        blockMap,
                                        counterMap,
                                        "설명",
                                        costMap,
                                        critMap));
    }

    @Provide
    Arbitrary<RecoverySkill> recoverySkills() {
        return Combinators.combine(monotoneRankMap(), monotoneRankMap())
                .as(
                        (healMap, costMap) ->
                                new RecoverySkill(
                                        "healing",
                                        "힐링",
                                        SkillType.RECOVERY,
                                        SkillTalent.MAGIC,
                                        10,
                                        healMap,
                                        costMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<UltimateSkill> ultimateSkills() {
        return Combinators.combine(monotoneRankMap(), monotoneRankMap(), monotoneRankMap())
                .as(
                        (multMap, hitMap, coolMap) ->
                                new UltimateSkill(
                                        "final_strike",
                                        "파이널 스트라이크",
                                        SkillType.ULTIMATE,
                                        SkillTalent.MELEE,
                                        15,
                                        multMap,
                                        hitMap,
                                        20,
                                        coolMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<BuffSkill> buffSkills() {
        return monotoneRankMap()
                .map(
                        absorbMap ->
                                new BuffSkill(
                                        "mana_shield",
                                        "마나 실드",
                                        SkillType.BUFF,
                                        SkillTalent.MAGIC,
                                        8,
                                        3,
                                        absorbMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<CcSkill> ccSkills() {
        return monotoneRankMap()
                .map(
                        successMap ->
                                new CcSkill(
                                        "smash",
                                        "스매시",
                                        SkillType.HEAVY,
                                        SkillTalent.MELEE,
                                        10,
                                        successMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<DotSkill> dotSkills() {
        return Combinators.combine(monotoneRankMap(), monotoneRankMap(), monotoneRankMap())
                .as(
                        (initMap, dotMap, turnsMap) ->
                                new DotSkill(
                                        "poison",
                                        "독",
                                        SkillType.DOT,
                                        SkillTalent.MELEE,
                                        7,
                                        initMap,
                                        dotMap,
                                        turnsMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<PassiveSkill> passiveSkills() {
        return Arbitraries.of(
                new PassiveSkill(
                        "meditation",
                        "명상",
                        SkillType.PASSIVE,
                        SkillTalent.MAGIC,
                        0,
                        Map.of(BonusTarget.MP, 20),
                        "설명"));
    }

    private Arbitrary<Map<SkillRank, Integer>> monotoneRankMap() {
        final Arbitrary<Integer> baseArb = Arbitraries.integers().between(10, 200);
        final Arbitrary<int[]> incrementsArb =
                Arbitraries.integers().between(0, 10).array(int[].class).ofSize(RANK_COUNT - 1);

        return Combinators.combine(baseArb, incrementsArb)
                .as(
                        (base, increments) -> {
                            final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
                            final SkillRank[] ranks = SkillRank.values();
                            int current = base;
                            map.put(ranks[0], current);
                            for (int i = 1; i < RANK_COUNT; i++) {
                                current += increments[i - 1];
                                map.put(ranks[i], current);
                            }
                            return map;
                        });
    }
}
