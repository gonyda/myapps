package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Npc 도메인 모델 생성자 및 컴포넌트 동작을 검증하는 단위 테스트.
 *
 * <p>6-인자 보조 생성자(shopItems 기본값)와 7-인자 기본 생성자 동작,
 * shopItems 불변 리스트 보장을 확인한다.
 *
 * <p><b>Validates: Requirements 2.1, 15.3</b>
 */
class NpcTest {

    private final NpcLines sampleLines = new NpcLines(
            List.of("안녕하세요."),
            Map.of("morning", List.of("좋은 아침입니다."))
    );

    /**
     * 6-인자 보조 생성자로 Npc를 생성하면 shopItems가 빈 불변 목록으로 초기화됨을 검증한다.
     */
    @Test
    void should_initializeEmptyShopItems_when_sixArgConstructorUsed() {
        final Npc npc = new Npc(
                "duncan",
                "던컨",
                NpcType.CHIEF,
                "tir-chonaill",
                "친절한 촌장",
                sampleLines
        );

        assertThat(npc.id()).isEqualTo("duncan");
        assertThat(npc.name()).isEqualTo("던컨");
        assertThat(npc.type()).isEqualTo(NpcType.CHIEF);
        assertThat(npc.nodeId()).isEqualTo("tir-chonaill");
        assertThat(npc.personality()).isEqualTo("친절한 촌장");
        assertThat(npc.lines()).isEqualTo(sampleLines);
        assertThat(npc.shopItems()).isNotNull().isEmpty();
        assertThatThrownBy(() -> npc.shopItems().add("item1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 7-인자 정규 생성자로 Npc를 생성하면 전달된 shopItems가 올바르게 설정됨을 검증한다.
     */
    @Test
    void should_preserveShopItems_when_sevenArgConstructorUsed() {
        final List<String> shopItems = List.of("short_sword", "long_sword");
        final Npc npc = new Npc(
                "ferghus",
                "퍼거스",
                NpcType.BLACKSMITH,
                "tir-chonaill",
                "대장장이",
                sampleLines,
                shopItems
        );

        assertThat(npc.id()).isEqualTo("ferghus");
        assertThat(npc.name()).isEqualTo("퍼거스");
        assertThat(npc.type()).isEqualTo(NpcType.BLACKSMITH);
        assertThat(npc.nodeId()).isEqualTo("tir-chonaill");
        assertThat(npc.personality()).isEqualTo("대장장이");
        assertThat(npc.lines()).isEqualTo(sampleLines);
        assertThat(npc.shopItems()).containsExactly("short_sword", "long_sword");
    }
}
