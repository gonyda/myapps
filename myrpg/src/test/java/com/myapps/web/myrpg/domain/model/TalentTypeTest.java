package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TalentType} 열거형의 라벨 매핑을 검증하는 단위 테스트.
 *
 * <p>3개 상수({@code MELEE}, {@code ARCHERY}, {@code MAGIC})의
 * {@code label()} 반환값이 요구사항에 정의된 한글 라벨과 일치하는지 확인한다.
 *
 * <p><b>Validates: Requirements 9.1</b>
 */
class TalentTypeTest {

    /**
     * MELEE의 라벨은 "근접전투"임을 검증한다.
     */
    @Test
    void should_returnMeleeLabel_when_melee() {
        assertThat(TalentType.MELEE.label()).isEqualTo("근접전투");
    }

    /**
     * ARCHERY의 라벨은 "활"임을 검증한다.
     */
    @Test
    void should_returnArcheryLabel_when_archery() {
        assertThat(TalentType.ARCHERY.label()).isEqualTo("활");
    }

    /**
     * MAGIC의 라벨은 "마법"임을 검증한다.
     */
    @Test
    void should_returnMagicLabel_when_magic() {
        assertThat(TalentType.MAGIC.label()).isEqualTo("마법");
    }
}
