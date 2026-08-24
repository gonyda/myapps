package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 결전 궁극기(Ultimate Skill) 절대 우위(Super-Priority) 및 쿨타임 메커니즘 단위 테스트. */
class BattleServiceUltimateTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "golem";
    private static final String ULTIMATE_ID = "final_hit";

    private BattleStateRepository battleStateRepo;
    private CharacterSkillRepository characterSkillRepo;
    private SkillCatalogService skillCatalogService;
    private SkillService skillService;
    private BattleService battleService;
    private ActionLog actionLog;

    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        final Monster monster =
                new Monster(
                        MONSTER_ID,
                        "골렘",
                        MonsterType.NORMAL,
                        10,
                        500,
                        50,
                        30,
                        10,
                        50L,
                        new GoldDrop(10, 50),
                        List.of(),
                        List.of("크르르"));
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.HEAVY);

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
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);
        final Random random = new Random(42L);

        skillCatalogService = mock(SkillCatalogService.class);
        final UltimateSkill ultimateSkill =
                new UltimateSkill(
                        ULTIMATE_ID,
                        "파이널 히트",
                        SkillType.ULTIMATE,
                        SkillTalent.MELEE,
                        15,
                        Map.of(SkillRank.F, 250),
                        Map.of(SkillRank.F, 5),
                        100,
                        Map.of(SkillRank.F, 30),
                        "결전 궁극기");
        when(skillCatalogService.byId(ULTIMATE_ID)).thenReturn(Optional.of(ultimateSkill));

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

    private CharacterProgress createProgress(final int hp, final int stamina, final int ap) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        10,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        hp,
                        50,
                        stamina,
                        "node-1",
                        ap,
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
    @DisplayName("궁극기 사용 시 절대 우위 발동: 적 방어 무시(100% 관통) 및 적 공격 완전 차단(0 피해), 쿨다운 설정")
    void should_dealPierceDamageAndCancelMonsterAttack_when_ultimateUsed() {
        // given
        final CharacterProgress progress = createProgress(100, 100, 10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 500, false);
        final CharacterSkill characterSkill = CharacterSkill.newSkill(CHARACTER_ID, ULTIMATE_ID);
        characterSkill.setUltimateCooldown(0); // 쿨타임 없음

        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, ULTIMATE_ID))
                .thenReturn(Optional.of(characterSkill));

        // when: 파이널 히트 사용
        final BattleTurnResult result = battleService.takeTurn(progress, state, ULTIMATE_ID);

        // then
        assertThat(result.playerDamage()).isGreaterThan(0);
        assertThat(result.monsterDamage()).isZero(); // 몬스터 공격 차단
        assertThat(result.firstStrike()).isTrue();
        assertThat(result.playerHits()).hasSize(5); // 5연타
        assertThat(characterSkill.getUltimateCooldown()).isEqualTo(30); // 30승 쿨다운 부여
        assertThat(progress.getStaminaCurrent()).isEqualTo(85); // 100 - 15 소모
        verify(characterSkillRepo).save(characterSkill);
    }

    @Test
    @DisplayName("궁극기 쿨타임 대기 중(cooldown > 0)일 때 사용 시도 시 행동 거부")
    void should_rejectUltimateUse_when_cooldownIsActive() {
        // given
        final CharacterProgress progress = createProgress(100, 100, 10);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 500, false);
        final CharacterSkill characterSkill = CharacterSkill.newSkill(CHARACTER_ID, ULTIMATE_ID);
        characterSkill.setUltimateCooldown(5); // 5승 대기 중

        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, ULTIMATE_ID))
                .thenReturn(Optional.of(characterSkill));

        // when: 쿨다운 중 파이널 히트 사용 시도
        final BattleTurnResult result = battleService.takeTurn(progress, state, ULTIMATE_ID);

        // then
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterDamage()).isZero();
        assertThat(actionLog.getEntries()).isNotEmpty();
        assertThat(actionLog.getEntries().get(0).message()).contains("궁극기 쿨타임 대기 중");
    }

    @Test
    @DisplayName("전투 승리 시 onBattleWon 호출되어 궁극기 쿨다운 1 감소")
    void should_decrementUltimateCooldown_onBattleWon() {
        // given
        final CharacterProgress progress = createProgress(100, 100, 10);
        final BattleState state =
                new BattleState(CHARACTER_ID, MONSTER_ID, 1, false); // 1 HP로 1타 즉시 사망
        final CharacterSkill characterSkill = CharacterSkill.newSkill(CHARACTER_ID, ULTIMATE_ID);
        characterSkill.setUltimateCooldown(0);

        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, ULTIMATE_ID))
                .thenReturn(Optional.of(characterSkill));

        // when
        final BattleTurnResult result = battleService.takeTurn(progress, state, ULTIMATE_ID);

        // then
        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        verify(skillService).onBattleWon(CHARACTER_ID);
    }
}
