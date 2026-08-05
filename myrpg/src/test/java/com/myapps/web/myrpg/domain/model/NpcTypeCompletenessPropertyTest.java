package com.myapps.web.myrpg.domain.model;

import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NpcType 매핑 완전성을 검증하는 프로퍼티 테스트.
 *
 * <p>모든 {@link NpcType} 상수에 대해 {@code label()}은 비어 있지 않은 문자열이고,
 * {@code actionLabels()}는 비어 있지 않은 목록임을 검증한다.
 * 즉 모든 타입이 라벨과 행동 정의를 빠짐없이 보유한다.
 *
 * <p>Feature: 002-npc-system, Property 9: Npc_Type 매핑 완전성(단일 소스)
 *
 * <p><b>Validates: Requirements 5.2, 5.3, 5.5</b>
 */
class NpcTypeCompletenessPropertyTest {

    /**
     * 모든 NpcType 상수의 label()은 비어 있지 않은 문자열이고,
     * actionLabels()는 비어 있지 않은 목록임을 검증한다.
     *
     * @param npcType 임의의 NpcType 상수
     */
    @Property(tries = 100)
    void should_haveLabelAndActionLabels_when_anyNpcType(@ForAll("npcTypes") final NpcType npcType) {
        // Then: label()은 비어 있지 않은 문자열
        final String label = npcType.label();
        assertThat(label).isNotNull().isNotEmpty();

        // Then: actionLabels()는 비어 있지 않은 목록
        final List<String> actionLabels = npcType.actionLabels();
        assertThat(actionLabels).isNotNull().isNotEmpty();
    }

    /**
     * NpcType 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 전체 NpcType 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<NpcType> npcTypes() {
        return Arbitraries.of(NpcType.values());
    }
}
