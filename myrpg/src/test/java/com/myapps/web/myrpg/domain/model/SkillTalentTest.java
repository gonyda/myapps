package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** {@link SkillTalent}의 재능 매칭, 자원 종류, 랭크업 스탯 대상, fromString을 검증하는 단위 테스트. */
class SkillTalentTest {

    @Test
    void should_return_matching_talent_for_combat_talents() {
        assertThat(SkillTalent.MELEE.matchingTalent()).isEqualTo(Optional.of(TalentType.MELEE));
        assertThat(SkillTalent.ARCHERY.matchingTalent()).isEqualTo(Optional.of(TalentType.ARCHERY));
        assertThat(SkillTalent.MAGIC.matchingTalent()).isEqualTo(Optional.of(TalentType.MAGIC));
    }

    @Test
    void should_return_empty_matching_talent_for_common() {
        assertThat(SkillTalent.COMMON.matchingTalent()).isEmpty();
    }

    @Test
    void should_return_mp_resource_for_magic() {
        assertThat(SkillTalent.MAGIC.resourceKind()).isEqualTo(ResourceKind.MP);
    }

    @Test
    void should_return_stamina_resource_for_non_magic() {
        assertThat(SkillTalent.MELEE.resourceKind()).isEqualTo(ResourceKind.STAMINA);
        assertThat(SkillTalent.ARCHERY.resourceKind()).isEqualTo(ResourceKind.STAMINA);
        assertThat(SkillTalent.COMMON.resourceKind()).isEqualTo(ResourceKind.STAMINA);
    }

    @Test
    void should_return_correct_rankup_stat_target() {
        assertThat(SkillTalent.MELEE.rankupStatTarget()).isEqualTo(BonusTarget.STR);
        assertThat(SkillTalent.ARCHERY.rankupStatTarget()).isEqualTo(BonusTarget.DEX);
        assertThat(SkillTalent.MAGIC.rankupStatTarget()).isEqualTo(BonusTarget.INT);
        assertThat(SkillTalent.COMMON.rankupStatTarget()).isEqualTo(BonusTarget.DEF);
    }

    @Test
    void should_parse_valid_string() {
        assertThat(SkillTalent.fromString("MELEE")).isEqualTo(Optional.of(SkillTalent.MELEE));
        assertThat(SkillTalent.fromString("ARCHERY")).isEqualTo(Optional.of(SkillTalent.ARCHERY));
        assertThat(SkillTalent.fromString("MAGIC")).isEqualTo(Optional.of(SkillTalent.MAGIC));
        assertThat(SkillTalent.fromString("COMMON")).isEqualTo(Optional.of(SkillTalent.COMMON));
    }

    @Test
    void should_return_empty_for_unknown_string() {
        assertThat(SkillTalent.fromString("UNKNOWN")).isEmpty();
        assertThat(SkillTalent.fromString("melee")).isEmpty();
        assertThat(SkillTalent.fromString("")).isEmpty();
        assertThat(SkillTalent.fromString(null)).isEmpty();
        assertThat(SkillTalent.fromString("   ")).isEmpty();
    }

    @Test
    void should_have_exactly_four_constants() {
        assertThat(SkillTalent.values()).hasSize(4);
    }
}
