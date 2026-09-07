package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TurnInput;
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

/**
 * 022 BattleService God Class 분해 착수 전, 미커버 경계 동작을 고정하는 Characterization 테스트.
 *
 * <p>검증 항목:
 *
 * <ul>
 *   <li>finalizeTurn 3경로(takeTurn / 타임아웃 / 도망 실패) 상태 확정 동치성
 *   <li>BattleTurnResultFactory 각 결과 필드값 보존
 *   <li>DoT 틱 소모 및 명상 MP 회복 경계
 *   <li>던전 보스 판정(3종) 및 연쇄 조우(10%), 몬스터 카운트 차감 1회 보장
 * </ul>
 */
class BattleServiceDecompositionCharacterizationTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "goblin";
    private static final String SKILL_ID = "smash";
    private static final int MONSTER_MAX_HP = 200;

    private BattleStateRepository battleStateRepo;
    private BattleResolver resolver;
    private MonsterService monsterService;
    private MonsterAiService aiService;
    private MonsterRewardService rewardService;
    private SkillService skillService;
    private InventoryService inventoryService;
    private ProgressionService progressionService;
    private CharacterService characterService;
    private SkillCatalogService skillCatalogService;
    private CharacterSkillRepository characterSkillRepo;
    private ItemCatalogService itemCatalogService;
    private DungeonService dungeonService;
    private ActionLog actionLog;
    private Monster monster;
    private BattleService battleService;

    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        resolver = mock(BattleResolver.class);
        when(resolver.resolve(any(TurnInput.class)))
                .thenReturn(new ResolvedTurn(20, 10, false, false, false, false, List.of()));

        monster =
                new Monster(
                        MONSTER_ID,
                        "고블린",
                        MonsterType.NORMAL,
                        5,
                        MONSTER_MAX_HP,
                        25,
                        10,
                        5,
                        20L,
                        new GoldDrop(5, 20),
                        List.of(),
                        List.of("끼에엑"));
        monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));

        aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        rewardService = mock(MonsterRewardService.class);
        when(rewardService.rollDrop(any())).thenReturn(DropResult.EMPTY);

        skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);
        when(skillService.rankupVitalBonus(any())).thenReturn(new VitalMax(0, 0, 0));
        when(skillService.meditationTurnRegen(anyLong())).thenReturn(0);

        inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        progressionService = mock(ProgressionService.class);
        characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final DamageSkill damageSkill = createDamageSkill();
        skillCatalogService = mock(SkillCatalogService.class);
        when(skillCatalogService.byId(SKILL_ID)).thenReturn(Optional.of(damageSkill));

        characterSkillRepo = mock(CharacterSkillRepository.class);
        when(characterSkillRepo.findByCharacterIdAndSkillId(anyLong(), eq(SKILL_ID)))
                .thenReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));

        itemCatalogService = mock(ItemCatalogService.class);
        dungeonService = mock(DungeonService.class);
        when(dungeonService.getActiveDungeon(anyLong())).thenReturn(Optional.empty());

        final StatProgression statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);

        final Random random = new Random(100L);

        battleService =
                new BattleService(
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
                        dungeonService);
    }

    private DamageSkill createDamageSkill() {
        return new DamageSkill(
                SKILL_ID,
                "스매시",
                SkillType.HEAVY,
                SkillTalent.MELEE,
                10,
                Map.of(SkillRank.F, 200),
                "설명");
    }

    private CharacterProgress createProgress(final int hp, final int sp, final int mp) {
        final CharacterProgress progress = CharacterProgress.createDefault();
        setId(progress, CHARACTER_ID);
        if (progress.getHpCurrent() > hp) {
            progress.damageHp(progress.getHpCurrent() - hp);
        }
        if (progress.getStaminaCurrent() > sp) {
            progress.spendStamina(progress.getStaminaCurrent() - sp);
        }
        if (progress.getMpCurrent() > mp) {
            progress.spendMp(progress.getMpCurrent() - mp);
        }
        return progress;
    }

    private void setId(final Object target, final Long id) {
        try {
            final Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── 1. finalizeTurn 3경로 상태 확정 동치성 ──────────────────────────

    @Test
    @DisplayName(
            "takeTurn 경로: finalizeTurn 상태 확정(standby, turnCount+1, intent null, nextPreemptive) 및 저장 검증")
    void should_finalizeTurnStateEquivalently_when_takeTurn() {
        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setStandby(false);
        state.setTurnCount(1);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setPreemptiveParty(PreemptiveParty.PLAYER);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result).isNotNull();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getTurnCount()).isEqualTo(2);
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        verify(battleStateRepo).save(state);
        verify(characterService).saveTurn(progress);
    }

    @Test
    @DisplayName(
            "타임아웃 경로: finalizeTurn 상태 확정(standby, turnCount+1, intent null, preemptive NONE) 및 저장 검증")
    void should_finalizeTurnStateEquivalently_when_timeoutTurn() {
        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setStandby(false);
        state.setTurnCount(3);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setPreemptiveParty(PreemptiveParty.PLAYER);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "timeout");

        assertThat(result).isNotNull();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getTurnCount()).isEqualTo(4);
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        verify(battleStateRepo).save(state);
        verify(characterService).saveTurn(progress);
    }

    @Test
    @DisplayName(
            "도망 실패 경로: finalizeTurn 상태 확정(standby, turnCount+1, intent null, preemptive NONE) 및 저장 검증")
    void should_finalizeTurnStateEquivalently_when_fleeFails() {
        final Random fleeFailRandom = mock(Random.class);
        when(fleeFailRandom.nextInt(100)).thenReturn(60); // 50 이상 -> 실패

        final BattleService customBattleService =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        rewardService,
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        fleeFailRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService,
                        dungeonService);

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setStandby(false);
        state.setTurnCount(1);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setPreemptiveParty(PreemptiveParty.MONSTER);

        final BattleTurnResult result = customBattleService.flee(progress, state);

        assertThat(result.battleEnded()).isFalse();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getTurnCount()).isEqualTo(2);
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        verify(battleStateRepo).save(state);
        verify(characterService).saveTurn(progress);
    }

    // ─── 2. BattleTurnResultFactory 각 결과 필드값 보존 ───────────────────

    @Test
    @DisplayName("미등록 스킬 ID 요청 시 buildNoOpResult의 19개 필드 기본값 보존")
    void should_preserveResultFields_when_unknownSkillRequested() {
        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result =
                battleService.takeTurn(progress, state, "nonexistent_skill");

        assertThat(result.playerAction()).isNull();
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterAction()).isNull();
        assertThat(result.monsterDamage()).isZero();
        assertThat(result.playerCritical()).isFalse();
        assertThat(result.monsterCritical()).isFalse();
        assertThat(result.blocked()).isFalse();
        assertThat(result.countered()).isFalse();
        assertThat(result.castFailure()).isFalse();
        assertThat(result.firstStrike()).isFalse();
        assertThat(result.resourceInsufficient()).isFalse();
        assertThat(result.insufficientKind()).isNull();
        assertThat(result.battleEnded()).isFalse();
        assertThat(result.outcome()).isEqualTo(Outcome.NONE);
        assertThat(result.reward()).isNull();
        assertThat(result.experienceGained()).isZero();
        assertThat(result.playerHits()).isEmpty();
        assertThat(result.combatLines()).isEmpty();
        assertThat(result.dungeonClearResult()).isNull();
    }

    @Test
    @DisplayName("스태미나 부족 시 buildInsufficientResult의 19개 필드 기본값 보존")
    void should_preserveResultFields_when_spInsufficient() {
        final CharacterProgress progress = createProgress(100, 5, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.playerAction()).isEqualTo(SkillType.HEAVY);
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterAction()).isNull();
        assertThat(result.monsterDamage()).isZero();
        assertThat(result.playerCritical()).isFalse();
        assertThat(result.monsterCritical()).isFalse();
        assertThat(result.blocked()).isFalse();
        assertThat(result.countered()).isFalse();
        assertThat(result.castFailure()).isFalse();
        assertThat(result.firstStrike()).isFalse();
        assertThat(result.resourceInsufficient()).isTrue();
        assertThat(result.insufficientKind()).isEqualTo(ResourceKind.STAMINA);
        assertThat(result.battleEnded()).isFalse();
        assertThat(result.outcome()).isEqualTo(Outcome.NONE);
        assertThat(result.reward()).isNull();
        assertThat(result.experienceGained()).isZero();
        assertThat(result.playerHits()).isEmpty();
        assertThat(result.combatLines()).isEmpty();
        assertThat(result.dungeonClearResult()).isNull();
    }

    @Test
    @DisplayName("도망 성공 시 BattleTurnResult 필드 보존(battleEnded=true, outcome=FLED)")
    void should_preserveResultFields_when_fleeSucceeds() {
        final Random fleeSuccessRandom = mock(Random.class);
        when(fleeSuccessRandom.nextInt(100)).thenReturn(30); // 50 미만 -> 성공

        final BattleService customBattleService =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        rewardService,
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        fleeSuccessRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService,
                        dungeonService);

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = customBattleService.flee(progress, state);

        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.FLED);
        assertThat(state.isActive()).isFalse();
    }

    // ─── 3. DoT 틱 및 명상 MP 회복 경계 ─────────────────────────────────

    @Test
    @DisplayName("DoT 잔여 턴이 있을 때 틱 데미지 적용 및 턴 수 차감 검증")
    void should_applyDoTTick_when_dotTurnsLeftGreaterThanZero() {
        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setDotTurnsLeft(3);
        state.setDotDamagePerTurn(15);

        battleService.takeTurn(progress, state, SKILL_ID);

        // 몬스터 HP는 턴 데미지 + DoT 15 추가 차감됨
        // 잔여 DoT 턴은 3에서 2로 감소
        assertThat(state.getDotTurnsLeft()).isEqualTo(2);
    }

    @Test
    @DisplayName("명상 스킬 보유 시 턴 종료 시점에 MP 회복 적용 검증")
    void should_applyMeditationRegen_when_characterHasMeditation() {
        // 명상 회복량 10 설정
        when(skillService.meditationTurnRegen(CHARACTER_ID)).thenReturn(10);

        final CharacterProgress progress = createProgress(100, 50, 20); // MP 20
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        // 스매시는 MELEE 스킬이므로 MP 소모 없음, 명상으로 +10 MP 회복되어 30이 됨
        assertThat(progress.getMpCurrent()).isEqualTo(30);
        assertThat(result.combatLines())
                .anyMatch(line -> line.contains("메디테이션") && line.contains("+10"));
    }

    // ─── 4. 던전 보스 판정 / 연쇄 조우(10%) / 몬스터 카운트 차감 ────────

    @Test
    @DisplayName("던전 보스 판정 1: bossRoomId와 currentRoomId가 일치할 때 보스 격파 처리")
    void should_triggerBossDefeated_when_bossRoomIdMatches() {
        final DungeonInstance dungeon = mock(DungeonInstance.class);
        when(dungeon.bossRoomId()).thenReturn("room_boss");
        when(dungeon.currentRoomId()).thenReturn("room_boss");
        when(dungeonService.getActiveDungeon(CHARACTER_ID)).thenReturn(Optional.of(dungeon));
        when(dungeonService.onBossDefeated(CHARACTER_ID))
                .thenReturn(new DungeonClearResult("dungeon_1", "알비 던전", 1000, 500, List.of()));

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state =
                new BattleState(CHARACTER_ID, MONSTER_ID, 10, false); // HP 10으로 1타에 처치

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        assertThat(result.dungeonClearResult()).isNotNull();
        assertThat(state.isActive()).isFalse();
        verify(dungeonService).onBossDefeated(CHARACTER_ID);
        verify(dungeonService, never()).onMonsterDefeated(anyLong(), anyString());
    }

    @Test
    @DisplayName("던전 보스 판정 2: monster ID가 giant-spider일 때 보스 격파 처리")
    void should_triggerBossDefeated_when_monsterIdIsGiantSpider() {
        final Monster giantSpider =
                new Monster(
                        "giant-spider",
                        "거대거미",
                        MonsterType.NORMAL,
                        10,
                        10,
                        30,
                        10,
                        5,
                        100L,
                        new GoldDrop(50, 100),
                        List.of(),
                        List.of());
        when(monsterService.byId("giant-spider")).thenReturn(Optional.of(giantSpider));

        final DungeonInstance dungeon = mock(DungeonInstance.class);
        when(dungeon.bossRoomId()).thenReturn("room_boss");
        when(dungeon.currentRoomId()).thenReturn("room_1");
        when(dungeonService.getActiveDungeon(CHARACTER_ID)).thenReturn(Optional.of(dungeon));
        when(dungeonService.onBossDefeated(CHARACTER_ID))
                .thenReturn(new DungeonClearResult("dungeon_1", "알비 던전", 1000, 500, List.of()));

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, "giant-spider", 10, false);

        final BattleTurnResult result = battleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        assertThat(result.dungeonClearResult()).isNotNull();
        assertThat(state.isActive()).isFalse();
        verify(dungeonService).onBossDefeated(CHARACTER_ID);
    }

    @Test
    @DisplayName("던전 일반 몬스터 처치 시: 몬스터 카운트가 정확히 1회 차감되고 dungeonMonsterDeducted 플래그가 설정된다")
    void should_deductDungeonMonsterOnlyOnce_when_normalMonsterKilled() {
        final DungeonInstance dungeon = mock(DungeonInstance.class);
        when(dungeon.bossRoomId()).thenReturn("room_boss");
        when(dungeon.currentRoomId()).thenReturn("room_1");
        when(dungeonService.getActiveDungeon(CHARACTER_ID)).thenReturn(Optional.of(dungeon));

        final Random noChainRandom = mock(Random.class);
        when(noChainRandom.nextInt(100)).thenReturn(20);

        final BattleService customBattleService =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        rewardService,
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        noChainRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService,
                        dungeonService);

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 10, false);
        assertThat(state.isDungeonMonsterDeducted()).isFalse();

        final BattleTurnResult result = customBattleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        assertThat(state.isDungeonMonsterDeducted()).isTrue();
        verify(dungeonService, times(1)).onMonsterDefeated(CHARACTER_ID, MONSTER_ID);
    }

    @Test
    @DisplayName("던전 일반 몬스터 처치 후 10% 확률로 연쇄 조우 발동 시: 전투 계속(Outcome.NONE, HP 재충전, turnCount 1)")
    void should_triggerChainCombat_when_chainCombatRollSucceeds() {
        final DungeonInstance dungeon = mock(DungeonInstance.class);
        when(dungeon.bossRoomId()).thenReturn("room_boss");
        when(dungeon.currentRoomId()).thenReturn("room_1");
        when(dungeonService.getActiveDungeon(CHARACTER_ID)).thenReturn(Optional.of(dungeon));

        final Random chainRandom = mock(Random.class);
        when(chainRandom.nextInt(100)).thenReturn(5);

        final BattleService customBattleService =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        rewardService,
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        chainRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        itemCatalogService,
                        dungeonService);

        final CharacterProgress progress = createProgress(100, 50, 50);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 10, false);

        final BattleTurnResult result = customBattleService.takeTurn(progress, state, SKILL_ID);

        assertThat(result.outcome()).isEqualTo(Outcome.NONE);
        assertThat(result.battleEnded()).isFalse();
        assertThat(state.isActive()).isTrue();
        assertThat(state.getMonsterCurrentHp()).isEqualTo(monster.maxHp());
        assertThat(state.getTurnCount()).isEqualTo(2);
        assertThat(result.combatLines()).anyMatch(line -> line.contains("연쇄 조우"));
    }
}
