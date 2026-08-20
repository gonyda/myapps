package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link GoldDrop}의 컴팩트 생성자 검증을 확인하는 단위 테스트.
 *
 * <p>유효한 범위({@code 0 ≤ min ≤ max})에서의 생성과, 범위 위반 시 {@link IllegalArgumentException} 발생을 검증한다.
 *
 * <p><b>Validates: Requirements 4.4</b>
 */
class GoldDropTest {

    /** min과 max가 모두 0인 경우 정상 생성됨을 검증한다. */
    @Test
    void should_createSuccessfully_when_minAndMaxAreZero() {
        final GoldDrop goldDrop = new GoldDrop(0, 0);

        assertThat(goldDrop.min()).isEqualTo(0);
        assertThat(goldDrop.max()).isEqualTo(0);
    }

    /** 유효한 범위(3~10)에서 정상 생성됨을 검증한다. */
    @Test
    void should_createSuccessfully_when_validRange() {
        final GoldDrop goldDrop = new GoldDrop(3, 10);

        assertThat(goldDrop.min()).isEqualTo(3);
        assertThat(goldDrop.max()).isEqualTo(10);
    }

    /** min과 max가 동일한 경우 정상 생성됨을 검증한다. */
    @Test
    void should_createSuccessfully_when_minEqualsMax() {
        final GoldDrop goldDrop = new GoldDrop(5, 5);

        assertThat(goldDrop.min()).isEqualTo(5);
        assertThat(goldDrop.max()).isEqualTo(5);
    }

    /** min이 음수인 경우 IllegalArgumentException이 발생함을 검증한다. */
    @Test
    void should_throwException_when_minIsNegative() {
        assertThatThrownBy(() -> new GoldDrop(-1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("min");
    }

    /** min이 max보다 큰 경우 IllegalArgumentException이 발생함을 검증한다. */
    @Test
    void should_throwException_when_minGreaterThanMax() {
        assertThatThrownBy(() -> new GoldDrop(10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("min");
    }
}
