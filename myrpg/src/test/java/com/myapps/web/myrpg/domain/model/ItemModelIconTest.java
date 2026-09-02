package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link Item} 구현체들의 {@code icon()} 반환값이 각 분류/종류별 SSOT 이모지와 일치하는지 검증하는 단위 테스트. */
class ItemModelIconTest {

    @Test
    @DisplayName("PotionItem의 icon()은 항상 🧪이어야 한다")
    void should_returnPotionEmoji_when_potionItemIconQueried() {
        final PotionItem potion = new PotionItem("hp_potion_30", "생명력 30 포션", 30, 50);

        assertThat(potion.icon()).isEqualTo("🧪");
        assertThat(potion.icon()).isEqualTo(ItemType.POTION.emoji());
    }

    @Test
    @DisplayName("MaterialItem의 icon()은 항상 📦이어야 한다")
    void should_returnMaterialEmoji_when_materialItemIconQueried() {
        final MaterialItem material = new MaterialItem("firewood", "장작", 20);

        assertThat(material.icon()).isEqualTo("📦");
        assertThat(material.icon()).isEqualTo(ItemType.MATERIAL.emoji());
    }

    @Test
    @DisplayName("EquipmentItem의 icon()은 각 EquipmentKind의 emoji()와 일치해야 한다")
    void should_returnKindEmoji_when_equipmentItemIconQueried() {
        final EquipmentItem sword =
                new EquipmentItem(
                        "short_sword",
                        "숏소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        300,
                        15);
        final EquipmentItem helmet =
                new EquipmentItem(
                        "beginner_helmet",
                        "초보자용 투구",
                        ItemType.ARMOR,
                        EquipmentKind.HELMET,
                        List.of(),
                        null,
                        20);
        final EquipmentItem body =
                new EquipmentItem(
                        "beginner_armor",
                        "초보자용 갑옷",
                        ItemType.ARMOR,
                        EquipmentKind.ARMOR_BODY,
                        List.of(),
                        null,
                        20);
        final EquipmentItem gloves =
                new EquipmentItem(
                        "beginner_gloves",
                        "초보자용 장갑",
                        ItemType.ARMOR,
                        EquipmentKind.GLOVES,
                        List.of(),
                        null,
                        20);
        final EquipmentItem boots =
                new EquipmentItem(
                        "beginner_boots",
                        "초보자용 부츠",
                        ItemType.ARMOR,
                        EquipmentKind.BOOTS,
                        List.of(),
                        null,
                        20);
        final EquipmentItem shield =
                new EquipmentItem(
                        "beginner_shield",
                        "초보자용 방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(),
                        null,
                        20);
        final EquipmentItem bow =
                new EquipmentItem(
                        "beginner_bow",
                        "초보자용 활",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(),
                        null,
                        20);
        final EquipmentItem wand =
                new EquipmentItem(
                        "beginner_wand",
                        "초보자용 완드",
                        ItemType.WEAPON,
                        EquipmentKind.WAND,
                        List.of(),
                        null,
                        20);

        assertThat(sword.icon()).isEqualTo("🗡️");
        assertThat(helmet.icon()).isEqualTo("🪖");
        assertThat(body.icon()).isEqualTo("🥋");
        assertThat(gloves.icon()).isEqualTo("🧤");
        assertThat(boots.icon()).isEqualTo("👢");
        assertThat(shield.icon()).isEqualTo("🛡️");
        assertThat(bow.icon()).isEqualTo("🏹");
        assertThat(wand.icon()).isEqualTo("🔮");
    }
}
