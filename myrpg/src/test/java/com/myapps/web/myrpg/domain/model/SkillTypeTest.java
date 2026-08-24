package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** {@link SkillType}의 라벨 및 {@code fromString} 변환을 검증하는 단위 테스트. */
class SkillTypeTest {

    @Test
    void should_return_korean_labels() {
        assertThat(SkillType.NORMAL.label()).isEqualTo("일반");
        assertThat(SkillType.HEAVY.label()).isEqualTo("강");
        assertThat(SkillType.DEFENSE.label()).isEqualTo("방어");
        assertThat(SkillType.RECOVERY.label()).isEqualTo("회복");
        assertThat(SkillType.ULTIMATE.label()).isEqualTo("궁극기");
        assertThat(SkillType.PASSIVE.label()).isEqualTo("패시브");
        assertThat(SkillType.BUFF.label()).isEqualTo("버프");
        assertThat(SkillType.DEBUFF.label()).isEqualTo("디버프");
        assertThat(SkillType.CC.label()).isEqualTo("제어");
        assertThat(SkillType.DOT.label()).isEqualTo("지속피해");
    }

    @Test
    void should_parse_valid_string() {
        assertThat(SkillType.fromString("NORMAL")).isEqualTo(Optional.of(SkillType.NORMAL));
        assertThat(SkillType.fromString("HEAVY")).isEqualTo(Optional.of(SkillType.HEAVY));
        assertThat(SkillType.fromString("DEFENSE")).isEqualTo(Optional.of(SkillType.DEFENSE));
        assertThat(SkillType.fromString("RECOVERY")).isEqualTo(Optional.of(SkillType.RECOVERY));
        assertThat(SkillType.fromString("ULTIMATE")).isEqualTo(Optional.of(SkillType.ULTIMATE));
        assertThat(SkillType.fromString("PASSIVE")).isEqualTo(Optional.of(SkillType.PASSIVE));
        assertThat(SkillType.fromString("BUFF")).isEqualTo(Optional.of(SkillType.BUFF));
        assertThat(SkillType.fromString("DEBUFF")).isEqualTo(Optional.of(SkillType.DEBUFF));
        assertThat(SkillType.fromString("CC")).isEqualTo(Optional.of(SkillType.CC));
        assertThat(SkillType.fromString("DOT")).isEqualTo(Optional.of(SkillType.DOT));
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
    void should_have_exactly_ten_constants() {
        assertThat(SkillType.values()).hasSize(10);
    }
}
