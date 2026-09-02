package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaterialItemTest {

    @Test
    @DisplayName("MaterialItem 생성 시 id, name, buyPrice가 올바르게 설정되고 type은 MATERIAL이어야 한다")
    void should_createMaterialItem_correctly() {
        final MaterialItem item = new MaterialItem("firewood", "장작", 20);

        assertThat(item.id()).isEqualTo("firewood");
        assertThat(item.name()).isEqualTo("장작");
        assertThat(item.buyPrice()).isEqualTo(20);
        assertThat(item.type()).isEqualTo(ItemType.MATERIAL);
        assertThat(item.type().code()).isEqualTo("material");
        assertThat(item.type().label()).isEqualTo("재료");
        assertThat(item.type().isStackable()).isTrue();
        assertThat(item.type().isEquipment()).isFalse();
    }

    @Test
    @DisplayName("ItemType.fromString으로 material 코드를 조회하면 ItemType.MATERIAL이 반환된다")
    void should_findMaterialItemType_fromString() {
        assertThat(ItemType.fromString("material")).contains(ItemType.MATERIAL);
    }

    @Test
    @DisplayName("description이 포함된 MaterialItem이 올바르게 생성된다")
    void should_createMaterialItem_withDescription() {
        final MaterialItem item =
                new MaterialItem("firewood", "장작", 20, "장작을 소모하여 캠프파이어를 할 수 있습니다.");

        assertThat(item.description()).isEqualTo("장작을 소모하여 캠프파이어를 할 수 있습니다.");
    }

    @Test
    @DisplayName("MaterialItem의 icon()은 MATERIAL 타입의 박스 이모지(📦)를 반환한다")
    void should_returnBoxEmoji_when_iconQueried() {
        final MaterialItem item = new MaterialItem("firewood", "장작", 20);

        assertThat(item.icon()).isEqualTo("📦");
    }
}
