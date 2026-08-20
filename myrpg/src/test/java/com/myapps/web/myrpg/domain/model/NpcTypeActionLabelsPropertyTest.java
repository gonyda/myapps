package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * NpcType actionLabels 무결성 프로퍼티 테스트.
 *
 * <p>모든 {@link NpcType} 상수에 대해 {@code actionLabels}는 null이 아니며 비어 있지 않은 불변 목록이고, 특히 {@code
 * MAGIC_SCHOOL}은 {@code ["상점", "인챈트"]}, {@code BLACKSMITH}는 {@code ["상점", "수리"]}, {@code HEALER}는
 * {@code ["상점", "치료받기"]}를 정확히 포함함을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 10: NpcType actionLabels 무결성
 *
 * <p><b>Validates: Requirements 12.1, 12.4</b>
 */
class NpcTypeActionLabelsPropertyTest {

    /**
     * 모든 NpcType에 대해 actionLabels()는 null이 아니며 비어 있지 않고, 타입별 정의된 행동 라벨 계약을 만족함을 검증한다.
     *
     * @param npcType 임의의 NpcType 상수
     */
    @Property(tries = 100)
    void should_satisfyActionLabelsContract_when_anyNpcType(
            @ForAll("npcTypes") final NpcType npcType) {
        final List<String> actionLabels = npcType.actionLabels();

        assertThat(actionLabels)
                .as("actionLabels는 null이 아니고 비어 있지 않아야 합니다")
                .isNotNull()
                .isNotEmpty();

        switch (npcType) {
            case MAGIC_SCHOOL -> assertThat(actionLabels).isEqualTo(List.of("상점", "인챈트"));
            case BLACKSMITH -> assertThat(actionLabels).isEqualTo(List.of("상점", "수리"));
            case HEALER -> assertThat(actionLabels).isEqualTo(List.of("상점", "치료받기"));
            case BANK -> assertThat(actionLabels).isEqualTo(List.of("은행"));
            case CHIEF -> assertThat(actionLabels).isEqualTo(List.of("퀘스트"));
            case SCHOOL -> assertThat(actionLabels).isEqualTo(List.of("상점"));
        }
    }

    /**
     * 전체 NpcType 상수를 생성하는 Arbitrary 제공자.
     *
     * @return NpcType 상수 Arbitrary
     */
    @Provide
    Arbitrary<NpcType> npcTypes() {
        return Arbitraries.of(NpcType.values());
    }
}
