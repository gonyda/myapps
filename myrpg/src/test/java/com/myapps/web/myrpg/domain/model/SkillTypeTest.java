package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SkillType}의 라벨 및 {@code fromString} 변환을 검증하는 단위 테스트.
 */
class SkillTypeTest {

    @Test
    void should_return_korean_labels() {
        assertThat(SkillType.NORMAL.label()).isEqualTo("일반");
        assertThat(SkillType.HEAVY.label()).isEqualTo("강");
        assertThat(SkillType.DEFENSE.label()).isEqualTo("방어");
    }

    @Test
    void should_parse_valid_string() {
        assertThat(SkillType.fromString("NORMAL")).isEqualTo(Optional.of(SkillType.NORMAL));
        assertThat(SkillType.fromString("HEAVY")).isEqualTo(Optional.of(SkillType.HEAVY));
        assertThat(SkillType.fromString("DEFENSE")).isEqualTo(Optional.of(SkillType.DEFENSE));
    }

    @Test
    void should_return_empty_for_unknown_string() {
        assertThat(SkillType.fromString("UNKNOWN")).isEmpty();
        assertThat(SkillType.fromString("normal")).isEmpty();
        assertThat(SkillType.fromString("")).isEmpty();
        assertThat(SkillType.fromString(null)).isEmpty();
        assertThat(SkillType.fromString("   ")).isEmpty();
    }

    @Test
    void should_have_exactly_three_constants() {
        assertThat(SkillType.values()).hasSize(3);
    }
}
