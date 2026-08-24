package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BattleService} 공방 개시, 상성 해결, 타임아웃, 자원 부족 및 도망 처리 기능 단위 테스트.
 *
 * <p>대치에서 공방 페이즈로의 전환, 몬스터 의도 추첨, B안 전조 뱃지 및 지속시간 매핑, 활 1턴 선제 사격, 상성 턴 해결 후 대치 복귀, 자원 부족 처리 및 활동 로그
 * 기록, 도망 실패 시 대치 복귀를 검증한다.
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 2.4</b>
 */
class BattleServiceClashTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "gray-wolf";
    private static final int MONSTER_MAX_HP = 150;

    private BattleStateRepository battleStateRepo;
    private MonsterService monsterService;
    private MonsterAiService aiService;
    private InventoryService inventoryService;
    private ProgressionService progressionService;
    private CharacterService characterService;
    private SkillCatalogService skillCatalogService;
    private CharacterSkillRepository characterSkillRepo;
    private SkillService skillService;
    private ActionLog actionLog;
    private Random random;
    private BattleService battleService;

    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        monsterService = mock(MonsterService.class);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(createMonster()));

        aiService = mock(MonsterAiService.class);
        inventoryService = mock(InventoryService.class);
        when(inventoryService.combatSkills(any()))
                .thenReturn(
                        List.of(
                                new BattleSkillButton(
                                        "slash", "베기", SkillType.NORMAL, ResourceKind.STAMINA, 0)));
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);

        random = new Random(42);
        final BattleResolver resolver = new BattleResolver(random);
        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
        skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);

        progressionService = mock(ProgressionService.class);
        when(progressionService.die(any(CharacterProgress.class))).thenReturn(new DeathResult(10L));

        characterService = mock(CharacterService.class);
        when(characterService.saveTurn(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final StatProgression statProgression = new StatProgression();
        final Clock clock = Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("UTC"));
        actionLog = new ActionLog(clock);
        skillCatalogService = mock(SkillCatalogService.class);
        characterSkillRepo = mock(CharacterSkillRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final DungeonService dungeonService = mock(DungeonService.class);

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

    // ─── 공방 개시 (startClash) 테스트 ────────────────────────────────────────

    @Test
    @DisplayName("공방 개시 시 몬스터 일반공격 전조와 1.0초 타이머가 매핑되고 standby=false로 전환된다")
    void startClash_NormalMonsterIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        when(inventoryService.isBowEquipped()).thenReturn(false);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final BattleView view = battleService.startClash(progress, state);

        assertThat(state.isStandby()).isFalse();
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.NORMAL);
        verify(battleStateRepo).save(state);

        assertThat(view.standby()).isFalse();
        assertThat(view.fleeAvailable()).isFalse();
        assertThat(view.monsterIntent()).isEqualTo(SkillType.NORMAL);
        assertThat(view.clashDurationMs()).isEqualTo(1000);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("⚡ 일반공격 태세");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-normal");
        assertThat(view.bowFirstStrike()).isFalse();
    }

    @Test
    @DisplayName("공방 개시 시 몬스터 강공격 전조와 1.5초 타이머가 매핑된다")
    void startClash_HeavyMonsterIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        when(inventoryService.isBowEquipped()).thenReturn(false);
        when(aiService.nextAction()).thenReturn(SkillType.HEAVY);

        final BattleView view = battleService.startClash(progress, state);

        assertThat(state.isStandby()).isFalse();
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.HEAVY);
        assertThat(view.monsterIntent()).isEqualTo(SkillType.HEAVY);
        assertThat(view.clashDurationMs()).isEqualTo(1500);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("💥 강공격 차징 중!");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-heavy");
        assertThat(view.bowFirstStrike()).isFalse();
    }

    @Test
    @DisplayName("공방 개시 시 몬스터 방어태세 전조와 1.5초 타이머가 매핑된다")
    void startClash_DefenseMonsterIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        when(inventoryService.isBowEquipped()).thenReturn(false);
        when(aiService.nextAction()).thenReturn(SkillType.DEFENSE);

        final BattleView view = battleService.startClash(progress, state);

        assertThat(state.isStandby()).isFalse();
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.DEFENSE);
        assertThat(view.monsterIntent()).isEqualTo(SkillType.DEFENSE);
        assertThat(view.clashDurationMs()).isEqualTo(1500);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("🛡️ 방어 태세");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-defense");
        assertThat(view.bowFirstStrike()).isFalse();
    }

    @Test
    @DisplayName("활 착용 1턴째 공방 개시 시 몬스터 의도 없이 활 선제 사격 상태가 활성화된다")
    void startClash_BowFirstStrike_Turn1() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(1);
        when(inventoryService.isBowEquipped()).thenReturn(true);

        final BattleView view = battleService.startClash(progress, state);

        verify(aiService, never()).nextAction();
        assertThat(state.isStandby()).isFalse();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(view.bowFirstStrike()).isTrue();
        assertThat(view.monsterIntent()).isNull();
        assertThat(view.clashDurationMs()).isEqualTo(1500);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("🏹 선제 사격 기회!");
        assertThat(view.monsterStanceBadgeClass()).isNull();
    }

    @Test
    @DisplayName("활 착용 2턴째 공방 개시 시 정상적으로 몬스터 의도가 추첨된다")
    void startClash_BowTurn2_NormalClash() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(2);
        when(inventoryService.isBowEquipped()).thenReturn(true);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final BattleView view = battleService.startClash(progress, state);

        verify(aiService).nextAction();
        assertThat(state.isStandby()).isFalse();
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.NORMAL);
        assertThat(view.bowFirstStrike()).isFalse();
        assertThat(view.monsterIntent()).isEqualTo(SkillType.NORMAL);
        assertThat(view.clashDurationMs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("몬스터 카탈로그에 없는 몬스터 ID일 경우 예외가 발생한다")
    void startClash_UnknownMonster_ThrowsException() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state =
                new BattleState(CHARACTER_ID, "unknown-monster", MONSTER_MAX_HP, false);
        when(monsterService.byId("unknown-monster")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.startClash(progress, state))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("몬스터 정보를 찾을 수 없습니다.");
    }

    // ─── 타임아웃 (timeout) 턴 처리 테스트 ────────────────────────────────────

    @Test
    @DisplayName("타임아웃 발생 시 몬스터의 일반공격에 100% 무방비 피격되고 standby=true 대치로 복귀한다")
    void takeTurn_Timeout_NormalIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setStandby(false);
        state.setTurnCount(1);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "timeout");

        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(result.monsterDamage()).isPositive();
        assertThat(result.playerDamage()).isZero();
        assertThat(progress.getHpCurrent()).isEqualTo(100 - result.monsterDamage());

        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getTurnCount()).isEqualTo(2);
        verify(battleStateRepo).save(state);
        verify(characterService).saveTurn(progress);

        assertThat(result.combatLines())
                .anyMatch(line -> line.contains("시간 초과! 몬스터의 공격에 무방비로 피격되었습니다!"))
                .anyMatch(line -> line.contains("너구리의 일반공격!"));
        assertThat(result.battleEnded()).isFalse();
    }

    @Test
    @DisplayName("타임아웃 발생 시 몬스터의 강공격에 100% 풀 피격된다")
    void takeTurn_Timeout_HeavyIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.HEAVY);
        state.setStandby(false);
        state.setTurnCount(1);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "timeout");

        assertThat(result.monsterAction()).isEqualTo(SkillType.HEAVY);
        assertThat(result.monsterDamage()).isPositive();
        assertThat(result.playerDamage()).isZero();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(result.combatLines()).anyMatch(line -> line.contains("너구리의 강공격!"));
    }

    @Test
    @DisplayName("타임아웃 발생 시 몬스터가 방어 태세였다면 0 피해를 입는다")
    void takeTurn_Timeout_DefenseIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.DEFENSE);
        state.setStandby(false);
        state.setTurnCount(1);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "timeout");

        assertThat(result.monsterAction()).isEqualTo(SkillType.DEFENSE);
        assertThat(result.monsterDamage()).isZero();
        assertThat(progress.getHpCurrent()).isEqualTo(100);
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(result.combatLines()).anyMatch(line -> line.contains("너구리은(는) 방어 태세를 유지했습니다."));
    }

    @Test
    @DisplayName("타임아웃 피격으로 HP가 0이 되면 사망 처리(LOSE)가 수행된다")
    void takeTurn_Timeout_PlayerDies() {
        final CharacterProgress progress = createProgress(1); // 1 HP
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setStandby(false);
        state.setTurnCount(1);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "timeout");

        assertThat(progress.isDead()).isTrue();
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.LOSE);
        assertThat(state.isActive()).isFalse();
        verify(progressionService).die(progress);
    }

    // ─── 상성 턴 해결 및 자원 부족 테스트 (Task 8 & 9) ──────────────────────────

    @Test
    @DisplayName("정상 스킬 입력 시 저장된 몬스터 의도에 맞춰 상성이 해결되고 standby=true로 대치 복귀한다")
    void takeTurn_NormalSkill_ResolvesCombat_And_ReturnsToStandby() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.HEAVY);
        state.setStandby(false);
        state.setTurnCount(1);

        final DamageSkill slash = createDamageSkill("slash", "베기", SkillType.NORMAL, 0);
        when(skillCatalogService.byId("slash")).thenReturn(Optional.of(slash));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "slash");

        verify(aiService, never()).nextAction();
        assertThat(result.monsterAction()).isEqualTo(SkillType.HEAVY);
        assertThat(result.playerDamage()).isPositive();
        assertThat(result.monsterDamage()).isZero(); // 일반공격이 강공격 캔슬
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getTurnCount()).isEqualTo(2);
        verify(battleStateRepo).save(state);
        verify(characterService).saveTurn(progress);
    }

    @Test
    @DisplayName("전투 상태에 몬스터 의도가 null인 경우 monsterAiService로 폴백하여 상성을 해결한다")
    void takeTurn_FallbackToAiService_WhenIntentIsNull() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(null);
        state.setStandby(false);
        state.setTurnCount(1);

        final DamageSkill slash = createDamageSkill("slash", "베기", SkillType.NORMAL, 0);
        when(skillCatalogService.byId("slash")).thenReturn(Optional.of(slash));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final BattleTurnResult result = battleService.takeTurn(progress, state, "slash");

        verify(aiService).nextAction();
        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
    }

    @Test
    @DisplayName("스태미나 부족 시 자원을 소모하지 않고 actionLog에만 기록하며 턴과 공방 상태를 유지한다")
    void takeTurn_InsufficientResource_LogsMessage_And_DoesNotAdvanceTurn() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        10,
                        10,
                        100L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        0,
                        "dunbarton",
                        0,
                        500L); // 0 stamina
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setStandby(false);
        state.setTurnCount(1);

        final DamageSkill smash =
                createDamageSkill("smash", "스매시", SkillType.HEAVY, 5); // 5 stamina cost
        when(skillCatalogService.byId("smash")).thenReturn(Optional.of(smash));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "smash");

        assertThat(result.resourceInsufficient()).isTrue();
        assertThat(result.insufficientKind()).isEqualTo(ResourceKind.STAMINA);
        assertThat(progress.getStaminaCurrent()).isZero();
        assertThat(state.getTurnCount()).isEqualTo(1);
        assertThat(state.isStandby()).isFalse(); // 타이머/공방 페이즈 유지
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.NORMAL); // 의도 유지

        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().contains("스태미나이(가) 부족합니다."));
        verify(battleStateRepo, never()).save(state);
    }

    @Test
    @DisplayName("활 1턴 선제 사격 시 몬스터 의도 없이 스킬이 발동되고 100% 선제 피해를 입힌 뒤 대치로 복귀한다")
    void takeTurn_BowFirstStrike_Turn1_DealsFullDamage_And_ReturnsToStandby() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setTurnCount(1);
        state.setCurrentMonsterIntent(null);
        state.setStandby(false);

        when(inventoryService.isBowEquipped()).thenReturn(true);
        final DamageSkill magnumShot =
                new DamageSkill(
                        "magnum_shot",
                        "매그넘 샷",
                        SkillType.HEAVY,
                        SkillTalent.ARCHERY,
                        5,
                        createFullRankMap(200),
                        "활 강타");
        when(skillCatalogService.byId("magnum_shot")).thenReturn(Optional.of(magnumShot));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "magnum_shot"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "magnum_shot")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "magnum_shot");

        assertThat(result.firstStrike()).isTrue();
        assertThat(result.playerDamage()).isPositive();
        assertThat(result.monsterDamage()).isZero();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getTurnCount()).isEqualTo(2);
        verify(battleStateRepo).save(state);
    }

    @Test
    @DisplayName("도망 실패 시 몬스터의 일반공격 1회 피격을 받고 standby=true 대치로 복귀한다")
    void flee_Failure_DealsNormalMonsterDamage_And_ReturnsToStandby() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setStandby(true);
        state.setCurrentMonsterIntent(SkillType.HEAVY); // 이전 찌꺼기가 있더라도 초기화되어야 함
        state.setTurnCount(1);

        // random이 FLEE_SUCCESS_PERCENT(50) 이상을 반환하도록 설정된 Mock Random 서비스 구성
        final Random mockRandom = mock(Random.class);
        when(mockRandom.nextInt(100)).thenReturn(80); // >= 50 실패
        final BattleResolver resolver = new BattleResolver(mockRandom);
        final BattleService battleServiceWithMockRandom =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        mock(MonsterRewardService.class),
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        mockRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        mock(ItemCatalogService.class),
                        mock(DungeonService.class));

        final BattleTurnResult result = battleServiceWithMockRandom.flee(progress, state);

        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(result.monsterDamage()).isPositive();
        assertThat(result.battleEnded()).isFalse();
        assertThat(result.combatLines()).anyMatch(line -> line.contains("도망 실패!"));
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.getTurnCount()).isEqualTo(2);
        verify(battleStateRepo).save(state);
    }

    @Test
    @DisplayName("도망 성공 시 전투가 종료되고(Outcome.FLED) actionLog에 기록된다")
    void flee_Success_EndsBattle() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);

        final Random mockRandom = mock(Random.class);
        when(mockRandom.nextInt(100)).thenReturn(20); // < 50 성공
        final BattleResolver resolver = new BattleResolver(mockRandom);
        final BattleService battleServiceWithMockRandom =
                new BattleService(
                        battleStateRepo,
                        resolver,
                        monsterService,
                        aiService,
                        mock(MonsterRewardService.class),
                        skillService,
                        inventoryService,
                        progressionService,
                        characterService,
                        new StatProgression(),
                        actionLog,
                        mockRandom,
                        skillCatalogService,
                        characterSkillRepo,
                        mock(ItemCatalogService.class),
                        mock(DungeonService.class));

        final BattleTurnResult result = battleServiceWithMockRandom.flee(progress, state);

        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.FLED);
        assertThat(state.isActive()).isFalse();
        assertThat(actionLog.getEntries()).anyMatch(entry -> entry.message().contains("도망쳤다!"));
    }

    // ─── 선제 공격권 (PreemptiveParty) 테스트 ────────────────────────────────

    @Test
    @DisplayName("유저 디펜스 성공(vs 몬스터 일반공격) 시 다음 턴 선제권이 PLAYER로 저장된다")
    void takeTurn_PlayerDefenseVsMonsterNormal_GrantsPlayerPreemptiveNextTurn() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setStandby(false);
        state.setTurnCount(1);

        final DefenseSkill defense = createDefenseSkill("defense", "디펜스", 0);
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(defense));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "defense"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "defense")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "defense");

        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        // 디펜스 성공으로 몬스터 데미지가 경감되어 들어옴
        assertThat(progress.getHpCurrent()).isLessThan(100);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.PLAYER);
        assertThat(state.isStandby()).isTrue();
    }

    @Test
    @DisplayName("유저 카운터어택 성공(vs 몬스터 일반공격) 시 반격 피해를 입히지만 다음 턴 선제권은 NONE이다")
    void takeTurn_PlayerCounterAttackVsMonsterNormal_DoesNotGrantPreemptive() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.NORMAL);
        state.setStandby(false);
        state.setTurnCount(1);

        final DefenseSkill counter = createDefenseSkill("counter_attack", "카운터 어택", 0);
        when(skillCatalogService.byId("counter_attack")).thenReturn(Optional.of(counter));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "counter_attack"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "counter_attack")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "counter_attack");

        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        assertThat(state.isStandby()).isTrue();
    }

    @Test
    @DisplayName("유저 카운터어택 성공(vs 몬스터 강공격) 시 반격 피해를 입히지만 다음 턴 선제권은 NONE이다")
    void takeTurn_PlayerCounterAttackVsMonsterHeavy_DoesNotGrantPreemptive() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.HEAVY);
        state.setStandby(false);
        state.setTurnCount(1);

        final DefenseSkill counter = createDefenseSkill("counter_attack", "카운터 어택", 0);
        when(skillCatalogService.byId("counter_attack")).thenReturn(Optional.of(counter));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "counter_attack"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "counter_attack")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "counter_attack");

        assertThat(result.monsterAction()).isEqualTo(SkillType.HEAVY);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        assertThat(state.isStandby()).isTrue();
    }

    @Test
    @DisplayName("플레이어 선제권 상태에서 공방 개시 시 선제공격 찬스 뱃지가 부여되고 intent는 null이 된다")
    void startClash_WithPlayerPreemptive_SetsPreemptiveBadgeAndNullIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setPreemptiveParty(PreemptiveParty.PLAYER);

        final BattleView view = battleService.startClash(progress, state);

        verify(aiService, never()).nextAction();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("⚡ 선제 공격 찬스!");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-preemptive-player");
    }

    @Test
    @DisplayName("플레이어 선제권 턴 진행 시 일방적인 선제 공격이 적중하고 선제권이 NONE으로 소비된다")
    void takeTurn_WithPlayerPreemptive_ExecutesOneSidedPreemptiveStrike_AndResetsPreemptiveParty() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setPreemptiveParty(PreemptiveParty.PLAYER);
        state.setStandby(false);
        state.setTurnCount(2);

        final DamageSkill slash = createDamageSkill("slash", "베기", SkillType.NORMAL, 0);
        when(skillCatalogService.byId("slash")).thenReturn(Optional.of(slash));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "slash");

        assertThat(result.firstStrike()).isTrue();
        assertThat(result.playerDamage()).isPositive();
        assertThat(result.monsterDamage()).isZero();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        assertThat(state.isStandby()).isTrue();
    }

    @Test
    @DisplayName("플레이어 선제권 턴에서 디펜스 스킬 사용 시 0 피해로 처리되고 선제권이 NONE으로 소비된다")
    void
            takeTurn_WithPlayerPreemptive_WhenDefenseSkillUsed_DealsZeroDamage_AndResetsPreemptiveParty() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setPreemptiveParty(PreemptiveParty.PLAYER);
        state.setStandby(false);
        state.setTurnCount(2);

        final DefenseSkill defense =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        3,
                        Map.of(),
                        Map.of(),
                        "방어 태세를 취해 적의 일반 공격을 방어합니다.");
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(defense));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "defense"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "defense")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "defense");

        assertThat(result.firstStrike()).isTrue();
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterDamage()).isZero();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        assertThat(state.isStandby()).isTrue();
        assertThat(result.combatLines()).containsExactly("선제 공격 기회였으나 디펜스(방어) 태세를 취했다!");
    }

    @Test
    @DisplayName("몬스터 방어 태세에 유저가 일반공격을 쓰면 다음 턴 선제권이 MONSTER로 저장된다")
    void takeTurn_PlayerNormalVsMonsterDefense_GrantsMonsterPreemptiveNextTurn() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setCurrentMonsterIntent(SkillType.DEFENSE);
        state.setStandby(false);
        state.setTurnCount(1);

        final DamageSkill slash = createDamageSkill("slash", "베기", SkillType.NORMAL, 0);
        when(skillCatalogService.byId("slash")).thenReturn(Optional.of(slash));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "slash");

        assertThat(result.monsterAction()).isEqualTo(SkillType.DEFENSE);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.MONSTER);
        assertThat(state.isStandby()).isTrue();
    }

    @Test
    @DisplayName("몬스터 선제권 상태에서 공방 개시 시 확정 선제 일반공격 뱃지가 부여되고 intent는 NORMAL이 된다")
    void startClash_WithMonsterPreemptive_SetsMonsterPreemptiveBadgeAndNormalIntent() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setPreemptiveParty(PreemptiveParty.MONSTER);

        final BattleView view = battleService.startClash(progress, state);

        verify(aiService, never()).nextAction();
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.NORMAL);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("⚠️ 몬스터의 확정 선제 일반공격!");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-preemptive-monster");
    }

    @Test
    @DisplayName("몬스터 선제권 턴 진행 시 몬스터의 일반공격이 일방적으로 적중하고 선제권이 NONE으로 소비된다")
    void
            takeTurn_WithMonsterPreemptive_ExecutesOneSidedMonsterNormalStrike_AndResetsPreemptiveParty() {
        final CharacterProgress progress = createProgress(100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        state.setPreemptiveParty(PreemptiveParty.MONSTER);
        state.setStandby(false);
        state.setTurnCount(2);

        final DamageSkill slash = createDamageSkill("slash", "베기", SkillType.NORMAL, 0);
        when(skillCatalogService.byId("slash")).thenReturn(Optional.of(slash));
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                .thenReturn(Optional.of(CharacterSkill.newSkill(CHARACTER_ID, "slash")));

        final BattleTurnResult result = battleService.takeTurn(progress, state, "slash");

        assertThat(result.firstStrike()).isTrue();
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterDamage()).isPositive();
        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);
        assertThat(state.isStandby()).isTrue();
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

    private CharacterProgress createProgress(final int hp) {
        return new CharacterProgress(
                "전사", 10, 10, 100L, TalentType.MELEE, null, hp, 100, 100, "dunbarton", 0, 500L);
    }

    private DamageSkill createDamageSkill(
            final String id, final String label, final SkillType type, final int cost) {
        return new DamageSkill(
                id, label, type, SkillTalent.MELEE, cost, createFullRankMap(100), "테스트 스킬");
    }

    private DefenseSkill createDefenseSkill(final String id, final String label, final int cost) {
        return new DefenseSkill(
                id,
                label,
                SkillType.DEFENSE,
                SkillTalent.COMMON,
                cost,
                createFullRankMap(70),
                createFullRankMap(0),
                "테스트 방어 스킬");
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
