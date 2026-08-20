package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link CharacterSkill} 엔티티의 도메인 로직을 검증하는 단위 테스트. */
class CharacterSkillTest {

    @Test
    @DisplayName("newSkill은 F 랭크·카운트 0으로 생성한다")
    void should_createWithRankFAndZeroCounts_when_newSkill() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "windmill");

        assertThat(skill.getCharacterId()).isEqualTo(1L);
        assertThat(skill.getSkillId()).isEqualTo("windmill");
        assertThat(skill.getRank()).isEqualTo(SkillRank.F);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getKillCount()).isZero();
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
    @DisplayName("increaseKill은 막타 처치 수를 1 증가시킨다")
    void should_incrementKillByOne_when_increaseKill() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "smash");

        skill.increaseKill();
        skill.increaseKill();
        skill.increaseKill();

        assertThat(skill.getKillCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("setUsageCount는 사용 횟수를 지정 값으로 설정한다")
    void should_setUsageToGivenValue_when_setUsageCount() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "firebolt");

        skill.setUsageCount(100);

        assertThat(skill.getUsageCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("setKillCount는 막타 처치 수를 지정 값으로 설정한다")
    void should_setKillToGivenValue_when_setKillCount() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "firebolt");

        skill.setKillCount(50);

        assertThat(skill.getKillCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("rankUpTo는 랭크를 변경하고 카운트를 0으로 리셋한다")
    void should_changeRankAndResetCounts_when_rankUpTo() {
        final CharacterSkill skill = new CharacterSkill(1L, "windmill", SkillRank.F, 5, 1);

        skill.rankUpTo(SkillRank.E);

        assertThat(skill.getRank()).isEqualTo(SkillRank.E);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getKillCount()).isZero();
    }

    @Test
    @DisplayName("rankUpTo로 여러 단계 승급 후에도 카운트가 리셋된다")
    void should_resetCountsOnEachRankUp_when_multipleRankUps() {
        final CharacterSkill skill = CharacterSkill.newSkill(1L, "smash");

        skill.setUsageCount(10);
        skill.setKillCount(3);
        skill.rankUpTo(SkillRank.E);

        assertThat(skill.getRank()).isEqualTo(SkillRank.E);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getKillCount()).isZero();

        skill.setUsageCount(20);
        skill.setKillCount(6);
        skill.rankUpTo(SkillRank.D);

        assertThat(skill.getRank()).isEqualTo(SkillRank.D);
        assertThat(skill.getUsageCount()).isZero();
        assertThat(skill.getKillCount()).isZero();
    }
}
