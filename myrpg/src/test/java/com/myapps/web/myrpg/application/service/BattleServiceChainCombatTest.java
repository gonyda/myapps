package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link BattleService}의 던전 연동, 10% 연쇄 전투 및 보스전 배제 단위 테스트.
 *
 * <p>Requirements: 6.1, 6.2, 6.3, 6.4, 8.2, 9.3
 */
@ExtendWith(MockitoExtension.class)
class BattleServiceChainCombatTest {

    private static final String SKILL_ID = "smash";
    private static final String SPIDER_ID = "spider";
    private static final String GIANT_SPIDER_ID = "giant-spider";
    private static final long CHARACTER_ID = 1L;

    @Mock private BattleStateRepository battleStateRepo;

    @Mock private BattleResolver resolver;

    @Mock private MonsterService monsterService;

    @Mock private MonsterAiService aiService;

    @Mock private MonsterRewardService rewardService;

    @Mock private SkillService skillService;

    @Mock private InventoryService inventoryService;

    @Mock private ProgressionService progressionService;

    @Mock private CharacterService characterService;

    @Mock private SkillCatalogService skillCatalogService;

    @Mock private CharacterSkillRepository characterSkillRepo;

    @Mock private ItemCatalogService itemCatalogService;

    @Mock private DungeonService dungeonService;

    private StatProgression statProgression;
    private ActionLog actionLog;
    private Monster spider;
    private Monster giantSpider;
    private CharacterProgress character;

    @BeforeEach
    void setUp() {
        statProgression = new StatProgression();
        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);

        spider =
                new Monster(
                        SPIDER_ID,
                        "흰 거미",
                        MonsterType.NORMAL,
                        2,
                        50,
                        40,
                        2,
                        20,
                        24L,
                        new GoldDrop(6, 15),
                        List.of(),
                        List.of("1", "2", "3"),
                        100,
                        0);

        giantSpider =
                new Monster(
                        GIANT_SPIDER_ID,
                        "거대거미",
                        MonsterType.BOSS,
                        7,
                        380,
                        72,
                        12,
                        70,
                        350L,
                        new GoldDrop(150, 300),
                        List.of(),
                        List.of("1", "2", "3"),
                        60,
                        50);

        character = CharacterProgress.createDefault();
        setId(character, CHARACTER_ID);

        given(skillCatalogService.byId(SKILL_ID)).willReturn(Optional.of(createDamageSkill()));
        given(characterSkillRepo.findByCharacterIdAndSkillId(any(), anyString()))
                .willReturn(
                        Optional.of(new CharacterSkill(CHARACTER_ID, SKILL_ID, SkillRank.F, 0, 0)));
        given(inventoryService.equippedBonus())
                .willReturn(new EquippedBonusResult(Stats.ZERO, new VitalMax(0, 0, 0)));
        given(skillService.rankupBonus(any())).willReturn(Stats.ZERO);
        given(aiService.nextAction()).willReturn(SkillType.NORMAL);
    }

    @Test
    @DisplayName("일반 던전 방에서 몬스터 처치 시 10% 연쇄 전투가 발동하면 몬스터 HP가 리셋되고 전투가 지속된다")
    void should_triggerChainCombat_when_regularMonsterKilledAndChainCombatRollSucceeds() {
        // given (준비: 난수 5 -> 10% 미만 당첨, 던전 인스턴스 활성)
        final Random random = mock(Random.class);
        given(random.nextInt(100)).willReturn(5); // < 10 -> Chain combat triggers

        final BattleService service = createBattleService(random);
        final BattleState state = new BattleState(CHARACTER_ID, SPIDER_ID, 10, false);
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-1-0", "room-2-0");

        given(rewardService.rollDrop(any())).willReturn(new DropResult(20, List.of()));
        given(monsterService.byId(SPIDER_ID)).willReturn(Optional.of(spider));
        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));

        // When player deals 50 damage (kills 10 HP spider)
        given(resolver.resolve(any()))
                .willReturn(
                        new com.myapps.web.myrpg.domain.model.ResolvedTurn(
                                50, 0, false, false, false, false, List.of()));

        // when (실행: 스킬 사용 턴 진행)
        final BattleTurnResult result = service.takeTurn(character, state, SKILL_ID);

        // then (검증: 연쇄 전투 발동으로 outcome=NONE, 몬스터 HP는 maxHp(50)로 갱신, 기습 문구 추가)
        assertThat(result.battleEnded()).isFalse();
        assertThat(result.outcome()).isEqualTo(BattleTurnResult.Outcome.NONE);
        assertThat(state.getMonsterCurrentHp()).isEqualTo(50);
        assertThat(state.isActive()).isTrue();

        then(dungeonService).should().onMonsterDefeated(CHARACTER_ID, SPIDER_ID);
        then(dungeonService).should(never()).onBossDefeated(any());
        assertThat(result.combatLines()).anyMatch(line -> line.contains("무리가 추가로 기습해왔다!"));
    }

    @Test
    @DisplayName("일반 던전 방에서 몬스터 처치 시 10% 연쇄 전투가 미발동하면 전투가 정상 종료(WIN)된다")
    void should_endBattleWithWin_when_regularMonsterKilledAndChainCombatRollFails() {
        // given (준비: 난수 50 -> 10% 이상 낙첨)
        final Random random = mock(Random.class);
        given(random.nextInt(100)).willReturn(50); // >= 10 -> No chain combat

        final BattleService service = createBattleService(random);
        final BattleState state = new BattleState(CHARACTER_ID, SPIDER_ID, 10, false);
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-1-0", "room-2-0");

        given(rewardService.rollDrop(any())).willReturn(new DropResult(20, List.of()));
        given(monsterService.byId(SPIDER_ID)).willReturn(Optional.of(spider));
        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));

        given(resolver.resolve(any()))
                .willReturn(
                        new com.myapps.web.myrpg.domain.model.ResolvedTurn(
                                50, 0, false, false, false, false, List.of()));

        // when (실행: 스킬 사용 턴 진행)
        final BattleTurnResult result = service.takeTurn(character, state, SKILL_ID);

        // then (검증: outcome=WIN, state.active=false, onMonsterDefeated 호출)
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(BattleTurnResult.Outcome.WIN);
        assertThat(state.isActive()).isFalse();

        then(dungeonService).should().onMonsterDefeated(CHARACTER_ID, SPIDER_ID);
        then(dungeonService).should(never()).onBossDefeated(any());
    }

    @Test
    @DisplayName("보스방(giant-spider) 처치 시 연쇄 전투 판정을 배제하고 즉시 onBossDefeated를 호출하며 WIN 종료된다")
    void should_callOnBossDefeated_and_bypassChainCombat_when_bossKilled() {
        // given (준비: 보스 거대거미 전투, 던전 보스방 위치)
        final Random random = mock(Random.class);
        final BattleService service = createBattleService(random);
        final BattleState state = new BattleState(CHARACTER_ID, GIANT_SPIDER_ID, 20, false);
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-2-0", "room-2-0");

        given(rewardService.rollDrop(any())).willReturn(new DropResult(20, List.of()));
        given(monsterService.byId(GIANT_SPIDER_ID)).willReturn(Optional.of(giantSpider));
        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));

        given(resolver.resolve(any()))
                .willReturn(
                        new com.myapps.web.myrpg.domain.model.ResolvedTurn(
                                50, 0, false, false, false, false, List.of()));

        // when (실행: 스킬 사용 턴 진행)
        final BattleTurnResult result = service.takeTurn(character, state, SKILL_ID);

        // then (검증: 연쇄 전투 롤 없이 즉시 onBossDefeated 호출, outcome=WIN)
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(BattleTurnResult.Outcome.WIN);
        assertThat(state.isActive()).isFalse();

        then(dungeonService).should().onBossDefeated(CHARACTER_ID);
        then(dungeonService).should(never()).onMonsterDefeated(any(), any());
    }

    @Test
    @DisplayName("던전 내 전투 중 플레이어 사망 시 dungeonService.handlePlayerDeath가 호출된다")
    void should_callHandlePlayerDeath_when_playerDiesInBattle() {
        // given (준비: HP 10인 플레이어가 50 피해를 입어 사망)
        final Random random = mock(Random.class);
        final BattleService service = createBattleService(random);
        final BattleState state = new BattleState(CHARACTER_ID, SPIDER_ID, 50, false);

        character.damageHp(95); // Remaining HP: 5
        given(monsterService.byId(SPIDER_ID)).willReturn(Optional.of(spider));
        given(progressionService.die(character)).willReturn(new DeathResult(10));

        given(resolver.resolve(any()))
                .willReturn(
                        new com.myapps.web.myrpg.domain.model.ResolvedTurn(
                                0, 50, false, false, false, false, List.of()));

        // when (실행: 턴 진행)
        final BattleTurnResult result = service.takeTurn(character, state, SKILL_ID);

        // then (검증: 사망 처리 및 handlePlayerDeath 호출)
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(BattleTurnResult.Outcome.LOSE);
        then(dungeonService).should().handlePlayerDeath(CHARACTER_ID);
        then(progressionService).should().die(character);
    }

    private BattleService createBattleService(final Random random) {
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
                dungeonService);
    }

    private DamageSkill createDamageSkill() {
        return new DamageSkill(
                SKILL_ID,
                "스매시",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                10,
                Map.of(SkillRank.F, 150),
                "설명",
                1,
                0);
    }

    private DungeonInstance createDungeonInstance(
            final Long charId, final String currentRoomId, final String bossRoomId) {
        final MapNode startNode =
                new MapNode(
                        "room-0-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of());
        final MapNode room1 =
                new MapNode(
                        "room-1-0",
                        "던전 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-0-0", "room-2-0"),
                        List.of("spider"));
        final MapNode bossRoom =
                new MapNode(
                        "room-2-0",
                        "거대거미의 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        2,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of("giant-spider"));

        final MapGraph graph =
                new MapGraph(List.of(startNode, room1, bossRoom), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState(
                                        "room-2-0", false, false, List.of("giant-spider")));

        return new DungeonInstance(
                charId,
                "alby",
                "alby-entrance",
                "room-0-0",
                bossRoomId,
                currentRoomId,
                graph,
                roomStates);
    }

    private void setId(final CharacterProgress progress, final long id) {
        try {
            final java.lang.reflect.Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
