package com.myapps.web.myrpg.application.service.battle;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BattleTurnResultFactory 단위 테스트")
class BattleTurnResultFactoryTest {

    private BattleTurnResultFactory factory;

    @BeforeEach
    void setUp() {
        factory = new BattleTurnResultFactory();
    }

    @Test
    @DisplayName("noOp: 공방 없는 무효 턴 결과를 반환해야 한다")
    void should_build_no_op_result() {
        // Given & When
        final BattleTurnResult result = factory.noOp();

        // Then
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
    @DisplayName("insufficient: 자원 부족 결과를 올바른 스킬 타입 및 자원 종류로 반환해야 한다")
    void should_build_insufficient_result_when_resource_shortage() {
        // Given
        final Skill skill =
                new DamageSkill(
                        "smash",
                        "스매시",
                        SkillType.HEAVY,
                        SkillTalent.MELEE,
                        10,
                        Map.of(SkillRank.F, 200),
                        "설명");

        // When
        final BattleTurnResult result = factory.insufficient(skill, ResourceKind.STAMINA);

        // Then
        assertThat(result.playerAction()).isEqualTo(SkillType.HEAVY);
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterAction()).isNull();
        assertThat(result.monsterDamage()).isZero();
        assertThat(result.resourceInsufficient()).isTrue();
        assertThat(result.insufficientKind()).isEqualTo(ResourceKind.STAMINA);
        assertThat(result.battleEnded()).isFalse();
        assertThat(result.outcome()).isEqualTo(Outcome.NONE);
        assertThat(result.playerHits()).isEmpty();
        assertThat(result.combatLines()).isEmpty();
        assertThat(result.dungeonClearResult()).isNull();
    }

    @Test
    @DisplayName("terminal: 도망 성공 시 FLED 결과와 라인을 포함하고 battleEnded=true여야 한다")
    void should_build_terminal_result_when_fled() {
        // Given
        final List<String> lines = List.of("도망에 성공했습니다.");

        // When
        final BattleTurnResult result = factory.terminal(Outcome.FLED, lines);

        // Then
        assertThat(result.playerAction()).isNull();
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterAction()).isNull();
        assertThat(result.monsterDamage()).isZero();
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.FLED);
        assertThat(result.combatLines()).containsExactly("도망에 성공했습니다.");
        assertThat(result.dungeonClearResult()).isNull();
    }

    @Test
    @DisplayName("terminal: 안전 종료 시 NONE 결과와 빈 라인을 포함하고 battleEnded=true여야 한다")
    void should_build_terminal_result_when_safe_terminated() {
        // Given & When
        final BattleTurnResult result = factory.terminal(Outcome.NONE, List.of());

        // Then
        assertThat(result.playerAction()).isNull();
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.NONE);
        assertThat(result.combatLines()).isEmpty();
    }

    @Test
    @DisplayName("monsterOnly: 몬스터 단독 행동 결과를 올바르게 조립해야 한다 (전투 진행 중)")
    void should_build_monster_only_result_when_ongoing() {
        // Given
        final List<String> lines = List.of("몬스터 공격");

        // When
        final BattleTurnResult result =
                factory.monsterOnly(SkillType.NORMAL, 25, Outcome.NONE, lines);

        // Then
        assertThat(result.playerAction()).isNull();
        assertThat(result.playerDamage()).isZero();
        assertThat(result.monsterAction()).isEqualTo(SkillType.NORMAL);
        assertThat(result.monsterDamage()).isEqualTo(25);
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
        assertThat(result.combatLines()).containsExactly("몬스터 공격");
        assertThat(result.dungeonClearResult()).isNull();
    }

    @Test
    @DisplayName("monsterOnly: 몬스터 단독 행동으로 사망 시 battleEnded=true 및 LOSE여야 한다")
    void should_build_monster_only_result_when_player_died() {
        // Given
        final List<String> lines = List.of("치명타 피격", "사망했습니다.");

        // When
        final BattleTurnResult result =
                factory.monsterOnly(SkillType.HEAVY, 100, Outcome.LOSE, lines);

        // Then
        assertThat(result.monsterAction()).isEqualTo(SkillType.HEAVY);
        assertThat(result.monsterDamage()).isEqualTo(100);
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.LOSE);
        assertThat(result.combatLines()).containsExactly("치명타 피격", "사망했습니다.");
    }

    @Test
    @DisplayName("combat: 모든 턴 필드를 19개 인자로 정확히 보존 조립해야 한다")
    void should_build_combat_result_with_all_fields() {
        // Given
        final DropResult reward = new DropResult(50L, List.of());
        final List<HitResult> playerHits = List.of(new HitResult(40, true));
        final List<String> combatLines = List.of("공격 성공", "몬스터 사망");
        final DungeonClearResult dungeonClear =
                new DungeonClearResult("alby_normal", "알비 던전", 500, 1000, List.of());

        // When
        final BattleTurnResult result =
                factory.combat(
                        SkillType.NORMAL,
                        40,
                        SkillType.DEFENSE,
                        0,
                        true,
                        false,
                        true,
                        false,
                        false,
                        true,
                        Outcome.WIN,
                        reward,
                        100,
                        playerHits,
                        combatLines,
                        dungeonClear);

        // Then
        assertThat(result.playerAction()).isEqualTo(SkillType.NORMAL);
        assertThat(result.playerDamage()).isEqualTo(40);
        assertThat(result.monsterAction()).isEqualTo(SkillType.DEFENSE);
        assertThat(result.monsterDamage()).isZero();
        assertThat(result.playerCritical()).isTrue();
        assertThat(result.monsterCritical()).isFalse();
        assertThat(result.blocked()).isTrue();
        assertThat(result.countered()).isFalse();
        assertThat(result.castFailure()).isFalse();
        assertThat(result.firstStrike()).isTrue();
        assertThat(result.resourceInsufficient()).isFalse();
        assertThat(result.insufficientKind()).isNull();
        assertThat(result.battleEnded()).isTrue();
        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        assertThat(result.reward()).isEqualTo(reward);
        assertThat(result.experienceGained()).isEqualTo(100L);
        assertThat(result.playerHits()).isEqualTo(playerHits);
        assertThat(result.combatLines()).isEqualTo(combatLines);
        assertThat(result.dungeonClearResult()).isEqualTo(dungeonClear);
    }
}
