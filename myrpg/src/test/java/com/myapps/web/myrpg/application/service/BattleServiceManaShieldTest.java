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
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
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

/** 마나 실드(Mana Shield) 피해 흡수 및 MP 고갈 오버플로 단위 테스트. */
class BattleServiceManaShieldTest {

    private static final long CHARACTER_ID = 1L;
    private static final String MONSTER_ID = "bear";
    private static final String BUFF_SKILL_ID = "mana_shield";
    private static final String ATTACK_SKILL_ID = "slash";

    private BattleStateRepository battleStateRepo;
    private CharacterSkillRepository characterSkillRepo;
    private SkillCatalogService skillCatalogService;
    private BattleService battleService;

    @BeforeEach
    void setUp() {
        battleStateRepo = mock(BattleStateRepository.class);
        when(battleStateRepo.save(any(BattleState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MonsterService monsterService = mock(MonsterService.class);
        // 곰: 공격력 100
        final Monster monster =
                new Monster(
                        MONSTER_ID,
                        "곰",
                        MonsterType.NORMAL,
                        15,
                        300,
                        100,
                        10,
                        10,
                        50L,
                        new GoldDrop(10, 50),
                        List.of(),
                        List.of("크아앙"));
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));

        final MonsterAiService aiService = mock(MonsterAiService.class);
        when(aiService.nextAction()).thenReturn(SkillType.NORMAL);

        final MonsterRewardService rewardService = mock(MonsterRewardService.class);
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
        final Random random = new Random(42L);

        skillCatalogService = mock(SkillCatalogService.class);
        final BuffSkill manaShield =
                new BuffSkill(
                        BUFF_SKILL_ID,
                        "마나 실드",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        10,
                        3, // 3턴 지속
                        Map.of(SkillRank.F, 50), // 50% 흡수
                        "마나 실드");
        when(skillCatalogService.byId(BUFF_SKILL_ID)).thenReturn(Optional.of(manaShield));

        final DamageSkill slash =
                new DamageSkill(
                        ATTACK_SKILL_ID,
                        "베기",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        5,
                        Map.of(SkillRank.F, 100),
                        "일반 공격");
        when(skillCatalogService.byId(ATTACK_SKILL_ID)).thenReturn(Optional.of(slash));

        characterSkillRepo = mock(CharacterSkillRepository.class);
        final SkillService skillService = mock(SkillService.class);
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

    private CharacterProgress createProgress(final int hp, final int mp) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "마법사", 1, 1, 0L, TalentType.MAGIC, null, hp, mp, 100, "node-1", 50, 1000L);
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
    @DisplayName("마나 실드 활성화 턴: 지속 턴수와 흡수율이 BattleState에 설정됨")
    void should_activateManaShield_when_buffSkillUsed() {
        // given
        final CharacterProgress progress = createProgress(100, 100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 300, false);
        final CharacterSkill charSkill = CharacterSkill.newSkill(CHARACTER_ID, BUFF_SKILL_ID);

        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, BUFF_SKILL_ID))
                .thenReturn(Optional.of(charSkill));

        // when: 마나 실드 시전
        final BattleTurnResult result = battleService.takeTurn(progress, state, BUFF_SKILL_ID);

        // then: 3턴 지속, 50% 흡수율 활성화
        assertThat(state.getManaShieldTurnsLeft()).isGreaterThanOrEqualTo(2);
        assertThat(state.getManaShieldAbsorbRate()).isEqualTo(50);
        assertThat(result.combatLines()).anyMatch(line -> line.contains("마나 실드 활성화"));
    }

    @Test
    @DisplayName("마나 실드 적용 중 피격 시 MP로 50% 흡수하고 남은 피해만 HP에 적용")
    void should_absorbDamageWithMp_when_manaShieldActive() {
        // given: HP 100, MP 100, 마나실드 3턴/50% 활성 상태
        final CharacterProgress progress = createProgress(100, 100);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 300, false);
        state.setManaShieldTurnsLeft(3);
        state.setManaShieldAbsorbRate(50);

        final CharacterSkill slashSkill = CharacterSkill.newSkill(CHARACTER_ID, ATTACK_SKILL_ID);
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, ATTACK_SKILL_ID))
                .thenReturn(Optional.of(slashSkill));

        // when: 일반 공격 턴 진행 (몬스터에게 피격 발생)
        final BattleTurnResult result = battleService.takeTurn(progress, state, ATTACK_SKILL_ID);

        // then: MP가 감소하고 combatLines에 흡수 메시지 기록
        assertThat(progress.getMpCurrent()).isLessThan(100);
        assertThat(result.combatLines()).anyMatch(line -> line.contains("마나 실드가 피해"));
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(2); // 1턴 차감
    }

    @Test
    @DisplayName("MP 부족 시 가용 MP까지만 흡수하고 잔여 피해는 HP로 관통(오버플로)")
    void should_overflowToHp_when_mpIsLow() {
        // given: HP 100, MP 5 (매우 적은 MP)
        final CharacterProgress progress = createProgress(100, 5);
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, 300, false);
        state.setManaShieldTurnsLeft(3);
        state.setManaShieldAbsorbRate(50);

        final CharacterSkill slashSkill = CharacterSkill.newSkill(CHARACTER_ID, ATTACK_SKILL_ID);
        when(characterSkillRepo.findByCharacterIdAndSkillId(CHARACTER_ID, ATTACK_SKILL_ID))
                .thenReturn(Optional.of(slashSkill));

        // when: 일반 공격 턴 진행
        battleService.takeTurn(progress, state, ATTACK_SKILL_ID);

        // then: MP는 0으로 고갈, HP는 잔여 피해로 감소
        assertThat(progress.getMpCurrent()).isZero();
        assertThat(progress.getHpCurrent()).isLessThan(100);
    }
}
