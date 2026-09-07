package com.myapps.web.myrpg.application.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.config.GameProperties.BattleProperties;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillCombatResolverTest {

    private BattleResolver resolver;
    private CombatStatResolver combatStatResolver;
    private CharacterSkillRepository characterSkillRepository;
    private StatProgression statProgression;
    private SkillService skillService;
    private GameMessageService gameMessageService;
    private GameProperties gameProperties;
    private Random random;
    private SkillCombatResolver skillCombatResolver;

    private Monster monster;
    private CharacterProgress progress;
    private BattleState state;

    @BeforeEach
    void setUp() {
        resolver = mock(BattleResolver.class);
        combatStatResolver = mock(CombatStatResolver.class);
        characterSkillRepository = mock(CharacterSkillRepository.class);
        statProgression = mock(StatProgression.class);
        skillService = mock(SkillService.class);
        gameMessageService = new GameMessageService(null);

        final BattleProperties battleProperties =
                new BattleProperties(50, 20, 10, 0.1, 1.0, 1.0, 1.0, 1.5, 100, 150, 4, 3, 3);
        gameProperties = new GameProperties(null, null, null, battleProperties, null, null);
        random = new Random(42L);

        skillCombatResolver =
                new SkillCombatResolver(
                        resolver,
                        combatStatResolver,
                        characterSkillRepository,
                        statProgression,
                        skillService,
                        gameMessageService,
                        gameProperties,
                        random);

        monster =
                new Monster(
                        "goblin",
                        "고블린",
                        MonsterType.NORMAL,
                        5,
                        100,
                        25,
                        10,
                        5,
                        20L,
                        new GoldDrop(5, 20),
                        List.of(),
                        List.of());
        progress = CharacterProgress.createDefault();
        state = new BattleState(1L, "goblin", 100, false);
    }

    // ─── 1. isBowFirstStrike & monsterMultiplier ──────────────────────────

    @Test
    @DisplayName("1턴이고 활 무기를 착용한 경우 isBowFirstStrike는 true를 반환한다")
    void should_returnTrue_when_firstTurnAndArcheryTalent() {
        // Given
        state.setTurnCount(1);

        // When
        final boolean isBow = skillCombatResolver.isBowFirstStrike(state, SkillTalent.ARCHERY);

        // Then
        assertThat(isBow).isTrue();
    }

    @Test
    @DisplayName("2턴 이상이거나 활 무기가 아니면 isBowFirstStrike는 false를 반환한다")
    void should_returnFalse_when_notFirstTurnOrNotArchery() {
        // Given
        state.setTurnCount(2);

        // When & Then
        assertThat(skillCombatResolver.isBowFirstStrike(state, SkillTalent.ARCHERY)).isFalse();
        state.setTurnCount(1);
        assertThat(skillCombatResolver.isBowFirstStrike(state, SkillTalent.MELEE)).isFalse();
    }

    @Test
    @DisplayName("monsterAction이 HEAVY면 heavyMultiplier, 아니면 normalMultiplier를 반환한다")
    void should_resolveMonsterMultiplier_accordingToAction() {
        // Given & When & Then
        assertThat(skillCombatResolver.monsterMultiplier(SkillType.HEAVY)).isEqualTo(150);
        assertThat(skillCombatResolver.monsterMultiplier(SkillType.NORMAL)).isEqualTo(100);
    }

    // ─── 2. resolveMonsterPreemptiveDamage & resolveMonsterOnlyDamage ──────

    @Test
    @DisplayName("몬스터 선제공격 시 플레이어 방어력과 일반 배율을 기반으로 피해량을 계산한다")
    void should_calculateMonsterPreemptiveDamage_correctly() {
        // Given
        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(8);
        when(resolver.baseDamage(25, 100, 8)).thenReturn(17);
        when(resolver.rollCritical(5)).thenReturn(false);
        when(resolver.finalDamage(17, 1.0, false)).thenReturn(17);

        // When
        final int dmg = skillCombatResolver.resolveMonsterPreemptiveDamage(progress, monster);

        // Then
        assertThat(dmg).isEqualTo(17);
    }

    @Test
    @DisplayName("몬스터 행동이 DEFENSE면 resolveMonsterOnlyDamage는 0을 반환한다")
    void should_returnZero_when_monsterActionIsDefense() {
        // When
        final int dmg =
                skillCombatResolver.resolveMonsterOnlyDamage(progress, monster, SkillType.DEFENSE);

        // Then
        assertThat(dmg).isZero();
    }

    @Test
    @DisplayName("몬스터 행동이 공격인 경우 resolveMonsterOnlyDamage는 정상 피해량을 계산한다")
    void should_calculateMonsterOnlyDamage_when_monsterAttacks() {
        // Given
        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(5);
        when(resolver.baseDamage(25, 100, 5)).thenReturn(20);
        when(resolver.rollCritical(5)).thenReturn(false);
        when(resolver.finalDamage(20, 1.0, false)).thenReturn(20);

        // When
        final int dmg =
                skillCombatResolver.resolveMonsterOnlyDamage(progress, monster, SkillType.NORMAL);

        // Then
        assertThat(dmg).isEqualTo(20);
    }

    // ─── 3. resolveSkillCombat Dispatch (Visitor) ──────────────────────────

    @Test
    @DisplayName("DamageSkill 일반 공방 시 ResolvedTurn 결과를 반영하고 턴 결과를 반환한다")
    void should_resolveNormalDamageCombat_via_visitor() {
        // Given
        final DamageSkill skill =
                new DamageSkill(
                        "smash",
                        "스매시",
                        SkillType.HEAVY,
                        SkillTalent.MELEE,
                        10,
                        Map.of(SkillRank.F, 200),
                        "설명");
        when(combatStatResolver.attackPower(progress, SkillTalent.MELEE)).thenReturn(30);
        when(combatStatResolver.resolvePlayerMultiplier(skill, progress)).thenReturn(200);
        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(10);
        when(combatStatResolver.resolveEffectivePlayerCritical(skill, progress)).thenReturn(15);
        when(combatStatResolver.resolvePlayerBlockRate(skill, progress)).thenReturn(0);
        when(combatStatResolver.resolvePlayerCounterPercent(skill, progress)).thenReturn(0);
        when(combatStatResolver.resolvePlayerHitCount(skill)).thenReturn(1);

        final HitResult hit = new HitResult(45, false);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(45, 15, false, false, false, false, List.of(hit)));

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        skill,
                        progress,
                        monster,
                        state,
                        Optional.empty(),
                        combatLines,
                        SkillTalent.MELEE,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(outcome.combat().playerDamage()).isEqualTo(45);
        assertThat(outcome.combat().monsterDamage()).isEqualTo(15);
        assertThat(outcome.nextPreemptive()).isEqualTo(PreemptiveParty.NONE);
    }

    @Test
    @DisplayName("플레이어 방어 vs 몬스터 일반공격 시 다음 턴 선제 파티가 PLAYER로 결정된다")
    void should_setPlayerPreemptive_when_playerDefenseVsMonsterNormal() {
        // Given
        final DefenseSkill skill =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.MELEE,
                        5,
                        Map.of(SkillRank.F, 50),
                        Map.of(SkillRank.F, 0),
                        "설명");
        when(combatStatResolver.attackPower(progress, SkillTalent.MELEE)).thenReturn(20);
        when(combatStatResolver.resolvePlayerMultiplier(skill, progress)).thenReturn(100);
        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(15);
        when(combatStatResolver.resolveEffectivePlayerCritical(skill, progress)).thenReturn(10);
        when(combatStatResolver.resolvePlayerBlockRate(skill, progress)).thenReturn(100);
        when(combatStatResolver.resolvePlayerCounterPercent(skill, progress)).thenReturn(0);
        when(combatStatResolver.resolvePlayerHitCount(skill)).thenReturn(1);

        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(0, 0, false, false, true, false, List.of()));

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        skill,
                        progress,
                        monster,
                        state,
                        Optional.empty(),
                        combatLines,
                        SkillTalent.MELEE,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(outcome.nextPreemptive()).isEqualTo(PreemptiveParty.PLAYER);
        assertThat(outcome.combat().blocked()).isTrue();
    }

    @Test
    @DisplayName("UltimateSkill 실행 시 멀티히트 대미지를 계산하고 쿨타임을 갱신한다")
    void should_resolveUltimateCombat_and_setCooldown() {
        // Given
        final UltimateSkill ultimate =
                new UltimateSkill(
                        "final_hit",
                        "파이널 히트",
                        SkillType.ULTIMATE,
                        SkillTalent.MELEE,
                        0,
                        Map.of(SkillRank.F, 300),
                        Map.of(SkillRank.F, 3),
                        10,
                        Map.of(SkillRank.F, 2),
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "final_hit", SkillRank.F, 0, 0);

        when(combatStatResolver.attackPower(progress, SkillTalent.MELEE)).thenReturn(40);
        when(combatStatResolver.resolveEffectivePlayerCritical(ultimate, progress)).thenReturn(20);
        final List<HitResult> hits = List.of(new HitResult(50, true), new HitResult(50, false));
        when(resolver.multiHitDamage(eq(40), eq(300), eq(0), eq(1.0), eq(30), eq(3)))
                .thenReturn(hits);

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        ultimate,
                        progress,
                        monster,
                        state,
                        Optional.of(charSkill),
                        combatLines,
                        SkillTalent.MELEE,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(outcome.combat().playerDamage()).isEqualTo(100);
        assertThat(outcome.combat().playerCritical()).isTrue();
        assertThat(outcome.combat().firstStrike()).isTrue();
        assertThat(charSkill.getUltimateCooldown()).isEqualTo(2);
        verify(characterSkillRepository).save(charSkill);
    }

    @Test
    @DisplayName("RecoverySkill 실행 시 HP를 회복하고 몬스터 행동 피해를 산출한다")
    void should_resolveRecoveryCombat_and_healHp() {
        // Given
        final RecoverySkill recovery =
                new RecoverySkill(
                        "heal",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 30),
                        Map.of(SkillRank.F, 10),
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "heal", SkillRank.F, 0, 0);

        progress.damageHp(50); // HP 50 상태
        final int beforeHp = progress.getHpCurrent();

        when(statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent()))
                .thenReturn(new VitalMax(100, 100, 100));
        when(skillService.rankupVitalBonus(progress.getId())).thenReturn(new VitalMax(0, 0, 0));
        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(5);
        when(resolver.baseDamage(25, 100, 5)).thenReturn(15);
        when(resolver.rollCritical(5)).thenReturn(false);
        when(resolver.finalDamage(15, 1.0, false)).thenReturn(15);

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        recovery,
                        progress,
                        monster,
                        state,
                        Optional.of(charSkill),
                        combatLines,
                        SkillTalent.MAGIC,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(progress.getHpCurrent()).isEqualTo(beforeHp + 30);
        assertThat(outcome.combat().monsterDamage()).isEqualTo(15);
        assertThat(combatLines).anyMatch(line -> line.contains("힐링"));
    }

    @Test
    @DisplayName("BuffSkill 실행 시 마나실드 상태를 설정하고 몬스터 행동 피해를 산출한다")
    void should_resolveBuffCombat_and_setManaShield() {
        // Given
        final BuffSkill buff =
                new BuffSkill(
                        "mana_shield",
                        "마나 실드",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        5,
                        3,
                        Map.of(SkillRank.F, 50),
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "mana_shield", SkillRank.F, 0, 0);

        when(combatStatResolver.resolvePlayerDefense(progress)).thenReturn(5);
        when(resolver.baseDamage(25, 100, 5)).thenReturn(15);
        when(resolver.rollCritical(5)).thenReturn(false);
        when(resolver.finalDamage(15, 1.0, false)).thenReturn(15);

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        buff,
                        progress,
                        monster,
                        state,
                        Optional.of(charSkill),
                        combatLines,
                        SkillTalent.MAGIC,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(3);
        assertThat(state.getManaShieldAbsorbRate()).isEqualTo(50);
        assertThat(outcome.combat().monsterDamage()).isEqualTo(15);
        assertThat(combatLines).anyMatch(line -> line.contains("마나 실드"));
    }

    @Test
    @DisplayName("선제 턴에 CcSkill 성공 시 몬스터가 1턴 스턴 상태로 유지된다")
    void should_resolveCcCombat_and_preserveStun_when_preemptive() {
        // Given
        final CcSkill cc =
                new CcSkill(
                        "ice_bolt",
                        "아이스 볼트",
                        SkillType.CC,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 100), // 100% 성공
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "ice_bolt", SkillRank.F, 0, 0);

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        cc,
                        progress,
                        monster,
                        state,
                        Optional.of(charSkill),
                        combatLines,
                        SkillTalent.MAGIC,
                        true,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(state.getMonsterStunnedTurns()).isEqualTo(1);
        assertThat(outcome.combat().monsterDamage()).isZero();
        assertThat(combatLines).anyMatch(line -> line.contains("아이스 볼트"));
    }

    @Test
    @DisplayName("일반 턴에 CcSkill 성공 시 이번 턴 몬스터 공격이 무효화되고 스턴 1턴이 소비된다")
    void should_resolveCcCombat_and_neutralizeMonsterAction_when_normalTurn() {
        // Given
        final CcSkill cc =
                new CcSkill(
                        "ice_bolt",
                        "아이스 볼트",
                        SkillType.CC,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 100), // 100% 성공
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "ice_bolt", SkillRank.F, 0, 0);

        final List<String> combatLines = new ArrayList<>();
        final SkillCombatContext context =
                new SkillCombatContext(
                        cc,
                        progress,
                        monster,
                        state,
                        Optional.of(charSkill),
                        combatLines,
                        SkillTalent.MAGIC,
                        false,
                        SkillType.NORMAL);

        // When
        final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);

        // Then
        assertThat(state.getMonsterStunnedTurns()).isZero(); // 이번 턴에 1 소비됨
        assertThat(outcome.combat().monsterDamage()).isZero();
        assertThat(combatLines).anyMatch(line -> line.contains("아이스 볼트"));
        assertThat(combatLines).anyMatch(line -> line.contains("빙결/기절 상태로 행동 불가"));
    }

    // ─── 4. applySpecialSkillSideEffects ──────────────────────────────────

    @Test
    @DisplayName("DotSkill 사용 시 BattleState에 DoT 턴 수와 턴당 피해량을 설정한다")
    void should_applyDotSideEffects_when_dotSkillUsed() {
        // Given
        final DotSkill dotSkill =
                new DotSkill(
                        "poison",
                        "독",
                        SkillType.DEBUFF,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 100),
                        Map.of(SkillRank.F, 50),
                        Map.of(SkillRank.F, 3),
                        "설명");
        final CharacterSkill charSkill = new CharacterSkill(1L, "poison", SkillRank.F, 0, 0);
        when(combatStatResolver.attackPower(progress, SkillTalent.MAGIC)).thenReturn(40);

        final List<String> combatLines = new ArrayList<>();

        // When
        skillCombatResolver.applySpecialSkillSideEffects(
                progress,
                state,
                dotSkill,
                Optional.of(charSkill),
                monster,
                SkillTalent.MAGIC,
                combatLines);

        // Then
        assertThat(state.getDotTurnsLeft()).isEqualTo(3);
        assertThat(state.getDotDamagePerTurn()).isEqualTo(20); // (40 * 50) / 100 = 20
        assertThat(state.getNextAttackAmpPercent()).isEqualTo(30); // DEBUFF 타입 증폭
        assertThat(combatLines).anyMatch(line -> line.contains("도트 피해"));
        assertThat(combatLines).anyMatch(line -> line.contains("레이지 임팩트"));
    }
}
