package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link CharacterSkill} 엔티티의 도메인 로직을 검증하는 단위 테스트. */
class CharacterSkillTest {

    @Test
    @DisplayName("newSkill은 F 랭크·사용 횟수 0으로 생성한다")
    void should_createWithRankFAndZeroCounts_when_newSkill() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "windmill");

        assertThat(skill.getCharacterId()).isEqualTo(1L);
        assertThat(skill.getSkillId()).isEqualTo("windmill");
        assertThat(skill.getRank()).isEqualTo(SkillRank.F);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getId()).isNull();
    }

    @Test
    @DisplayName("increaseUsage는 사용 횟수를 1 증가시킨다")
    void should_incrementUsageByOne_when_increaseUsage() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "smash");

        skill.increaseUsage();
        skill.increaseUsage();

        assertThat(skill.getUsageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("setUsageCount는 사용 횟수를 지정 값으로 설정한다")
    void should_setUsageToGivenValue_when_setUsageCount() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "firebolt");

        skill.setUsageCount(100);

        assertThat(skill.getUsageCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("rankUpTo는 랭크를 변경하고 사용 횟수를 0으로 리셋한다")
    void should_changeRankAndResetCounts_when_rankUpTo() {
        final CharacterSkill skill = new CharacterSkill(1L, "windmill", SkillRank.F, 5);

        skill.rankUpTo(SkillRank.E);

        assertThat(skill.getRank()).isEqualTo(SkillRank.E);
        assertThat(skill.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("rankUpTo로 여러 단계 승급 후에도 사용 횟수가 리셋된다")
    void should_resetCountsOnEachRankUp_when_multipleRankUps() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "smash");

        skill.setUsageCount(10);
        skill.rankUpTo(SkillRank.E);

        assertThat(skill.getRank()).isEqualTo(SkillRank.E);
        assertThat(skill.getUsageCount()).isZero();

        skill.setUsageCount(20);
        skill.rankUpTo(SkillRank.D);

        assertThat(skill.getRank()).isEqualTo(SkillRank.D);
        assertThat(skill.getUsageCount()).isZero();
    }
}
