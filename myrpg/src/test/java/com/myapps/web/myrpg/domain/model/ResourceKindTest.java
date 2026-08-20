package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** {@link ResourceKind}의 라벨 및 상수 정의를 검증하는 단위 테스트. */
class ResourceKindTest {

    @Test
    void should_return_korean_labels() {
        assertThat(ResourceKind.STAMINA.label()).isEqualTo("스태미나");
        assertThat(ResourceKind.MP.label()).isEqualTo("MP");
    }

    @Test
    void should_have_exactly_two_constants() {
        assertThat(ResourceKind.values()).hasSize(2);
    }
}
