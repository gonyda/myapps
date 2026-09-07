package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
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
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import java.lang.reflect.Field;
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

/** 020 스킬 타입 다형화 리팩토링 착수 전, BattleService의 미커버 전투 분기 및 스탯 해석의 현행 동작을 고정하는 Characterization 테스트. */
class BattleServiceCombatCharacterizationTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "goblin";

    private BattleStateRepository battleStateRepo;
    private CharacterSkillRepository characterSkillRepo;
    private SkillCatalogService skillCatalogService;
    private SkillService skillService;
    private BattleService battleService;
    private ActionLog actionLog;
    private Monster monster;

    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        monster =
                new Monster(
                        MONSTER_ID,
                        "고블린",
                        MonsterType.NORMAL,
                        5,
                        200,
                        25,
                        10,
                        5,
                        20L,
                        new GoldDrop(5, 20),
                        List.of(),
                        List.of("끼에엑"));
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        when(rewardService.rollDrop(any()))
                .thenReturn(com.myapps.web.myrpg.application.dto.DropResult.EMPTY);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        final ProgressionService progressionService = mock(ProgressionService.class);
        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);
        final Random random = new Random(12345L);

        skillCatalogService = mock(SkillCatalogService.class);
        characterSkillRepo = mock(CharacterSkillRepository.class);
        skillService = mock(SkillService.class);
        when(skillService.rankupBonus(anyLong())).thenReturn(Stats.ZERO);
        when(skillService.rankupVitalBonus(anyLong())).thenReturn(new VitalMax(0, 0, 0));

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);

        battleService =
                new BattleService(
                        battleStateRepo,
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
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService,
                        null);
    }

    private CharacterProgress createProgress(final int hp, final int mp, final int stamina) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "모험가",
                        5,
                        1,
                        0L,
                        TalentType.MAGIC,
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

    @Test
    @DisplayName("RecoverySkill(힐링) 사용 시 플레이어 HP 회복되고 몬스터 공격을 받는다")
    void should_healPlayerAndTakeMonsterDamage_when_recoverySkillUsed() {
        // given
        final RecoverySkill healing =
                new RecoverySkill(
                        "healing",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 30),
                        Map.of(SkillRank.F, 12),
                        "체력 회복");
        when(skillCatalogService.byId("healing")).thenReturn(Optional.of(healing));

        final CharacterSkill charSkill = CharacterSkill.newSkill(CHARACTER_ID, "healing");
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                .thenReturn(Optional.of(charSkill));

        final CharacterProgress progress = createProgress(40, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 200, false);
        state.setStandby(false);

        // when
        final BattleTurnResult result = battleService.takeTurn(progress, state, "healing");

        // then
        assertThat(result.playerDamage()).isZero();
        assertThat(progress.getHpCurrent()).isGreaterThan(40);
        assertThat(result.monsterDamage()).isGreaterThan(0);
        assertThat(result.firstStrike()).isFalse();
    }

    @Test
    @DisplayName("BuffSkill(마나 실드) 사용 시 마나 실드 턴 및 흡수율 버프가 상태에 설정된다")
    void should_applyManaShieldBuff_when_buffSkillUsed() {
        // given
        final BuffSkill manaShield =
                new BuffSkill(
                        "mana_shield",
                        "마나 실드",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        8,
                        3,
                        Map.of(SkillRank.F, 20),
                        "피해 MP 흡수");
        when(skillCatalogService.byId("mana_shield")).thenReturn(Optional.of(manaShield));

        final CharacterSkill charSkill = CharacterSkill.newSkill(CHARACTER_ID, "mana_shield");
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "mana_shield"))
                .thenReturn(Optional.of(charSkill));

        final CharacterProgress progress = createProgress(80, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 200, false);
        state.setStandby(false);

        // when
        final BattleTurnResult result = battleService.takeTurn(progress, state, "mana_shield");

        // then
        assertThat(result.playerDamage()).isZero();
        // 3턴 버프 부여 후 당해 턴 몬스터 데미지 흡수로 1턴 소모되어 잔여 2턴
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(2);
        assertThat(state.getManaShieldAbsorbRate()).isEqualTo(20);
        assertThat(result.monsterDamage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("CcSkill(스매시) 성공 시 몬스터가 1턴 기절하고 당해 턴 몬스터 공격이 무효화된다")
    void should_stunMonsterAndPreventDamage_when_ccSkillSucceeds() {
        // given: 100% 성공률 CC 스킬
        final CcSkill smash =
                new CcSkill(
                        "smash",
                        "스매시",
                        SkillType.HEAVY,
                        SkillTalent.MELEE,
                        10,
                        Map.of(SkillRank.F, 100),
                        "기절 효과");
        when(skillCatalogService.byId("smash")).thenReturn(Optional.of(smash));

        final CharacterSkill charSkill = CharacterSkill.newSkill(CHARACTER_ID, "smash");
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "smash"))
                .thenReturn(Optional.of(charSkill));

        final CharacterProgress progress = createProgress(80, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 200, false);
        state.setStandby(false);

        // when
        final BattleTurnResult result = battleService.takeTurn(progress, state, "smash");

        // then
        assertThat(result.playerDamage()).isZero();
        // 당해 턴에 기절 1턴이 발동하여 몬스터 공격을 차단하고 1턴 소모됨 (0턴 남음)
        assertThat(state.getMonsterStunnedTurns()).isZero();
        assertThat(result.monsterDamage()).isZero(); // CC 성공으로 몬스터 턴 데미지 0
        assertThat(result.combatLines())
                .anyMatch(line -> line.contains("기절") || line.contains("스매시"));
    }

    @Test
    @DisplayName(
            "스탯 해석 characterization: resolvePlayerMultiplier, resolvePlayerBlockRate, resolvePlayerCounterPercent, resolvePlayerHitCount, resolveResourceCost fallback 동작")
    void should_verifyStatResolutionFallbacks() throws Exception {
        // given: 각 타입별 스킬 인스턴스
        final DamageSkill damageSkill =
                new DamageSkill(
                        "slash",
                        "베기",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        5,
                        Map.of(SkillRank.F, 120),
                        "설명",
                        2,
                        10);
        final DefenseSkill defenseSkill =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        5,
                        Map.of(SkillRank.F, 70),
                        Map.of(SkillRank.F, 0),
                        "설명",
                        Map.of(SkillRank.F, 6),
                        Map.of(SkillRank.F, 5));
        final RecoverySkill recoverySkill =
                new RecoverySkill(
                        "healing",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 30),
                        Map.of(SkillRank.F, 15),
                        "설명");
        final BuffSkill buffSkill =
                new BuffSkill(
                        "shield",
                        "보호",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        8,
                        3,
                        Map.of(SkillRank.F, 20),
                        "설명");
        final DotSkill dotSkill =
                new DotSkill(
                        "poison",
                        "독",
                        SkillType.DOT,
                        SkillTalent.MELEE,
                        7,
                        Map.of(SkillRank.F, 50),
                        Map.of(SkillRank.F, 3),
                        Map.of(SkillRank.F, 10),
                        "설명");
        final PassiveSkill passiveSkill =
                new PassiveSkill(
                        "meditation",
                        "명상",
                        SkillType.PASSIVE,
                        SkillTalent.MAGIC,
                        0,
                        Map.of(),
                        "설명");

        final CharacterProgress progress = createProgress(100, 100, 100);

        final Field statResolverField = BattleService.class.getDeclaredField("combatStatResolver");
        statResolverField.setAccessible(true);
        final com.myapps.web.myrpg.application.service.battle.CombatStatResolver statResolver =
                (com.myapps.web.myrpg.application.service.battle.CombatStatResolver)
                        statResolverField.get(battleService);

        // 1. resolveResourceCost: Defense/Recovery만 랭크별 소모 (스킬 미보유 시에는 skill.resourceCost 고정)
        assertThat(statResolver.resolveResourceCost(defenseSkill, progress)).isEqualTo(5);
        assertThat(statResolver.resolveResourceCost(recoverySkill, progress)).isEqualTo(10);
        assertThat(statResolver.resolveResourceCost(buffSkill, progress)).isEqualTo(8);

        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "defense"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "defense")));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "healing")));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "shield"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "shield")));

        assertThat(statResolver.resolveResourceCost(defenseSkill, progress)).isEqualTo(6);
        assertThat(statResolver.resolveResourceCost(recoverySkill, progress)).isEqualTo(15);
        assertThat(statResolver.resolveResourceCost(buffSkill, progress)).isEqualTo(8);

        // 2. resolvePlayerMultiplier: Damage -> policy 배율, Dot -> initialMultiplier, 그 외 ->
        // monsterNormalMultiplier(100)
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "poison"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "poison")));

        assertThat(statResolver.resolvePlayerMultiplier(damageSkill, progress)).isEqualTo(120);
        assertThat(statResolver.resolvePlayerMultiplier(dotSkill, progress)).isEqualTo(50);
        assertThat(statResolver.resolvePlayerMultiplier(buffSkill, progress)).isEqualTo(100);
        assertThat(statResolver.resolvePlayerMultiplier(passiveSkill, progress)).isEqualTo(100);

        // 3. resolvePlayerBlockRate: DefenseSkill만 블록률, 그 외 0
        assertThat(statResolver.resolvePlayerBlockRate(defenseSkill, progress)).isEqualTo(70);
        assertThat(statResolver.resolvePlayerBlockRate(damageSkill, progress)).isZero();
        assertThat(statResolver.resolvePlayerBlockRate(recoverySkill, progress)).isZero();

        // 4. resolvePlayerCounterPercent: DefenseSkill만 카운터, 그 외 0
        assertThat(statResolver.resolvePlayerCounterPercent(defenseSkill, progress)).isZero();
        assertThat(statResolver.resolvePlayerCounterPercent(damageSkill, progress)).isZero();

        // 5. resolvePlayerHitCount: DamageSkill만 ds.hitCount(), 그 외 1
        assertThat(statResolver.resolvePlayerHitCount(damageSkill)).isEqualTo(2);
        assertThat(statResolver.resolvePlayerHitCount(defenseSkill)).isEqualTo(1);
        assertThat(statResolver.resolvePlayerHitCount(recoverySkill)).isEqualTo(1);
        assertThat(statResolver.resolvePlayerHitCount(buffSkill)).isEqualTo(1);
    }
}
