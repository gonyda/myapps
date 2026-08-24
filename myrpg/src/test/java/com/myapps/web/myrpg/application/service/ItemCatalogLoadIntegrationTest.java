package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.Item;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 실제 {@code data/item.json} 로딩 통합 테스트.
 *
 * <p>Spring Boot 컨텍스트 전체를 기동하여 {@link ItemCatalogService}가 클래스패스 리소스를 정상 로드하고, 11종 아이템·유일 id·장비
 * maxDurability 완비를 검증한다.
 *
 * <p>Validates: Requirements 5.1, 5.9
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ItemCatalogLoadIntegrationTest {

    private static final int TOTAL_ITEM_COUNT = 57;
    private static final Set<String> KNOWN_ITEM_IDS =
            Set.of(
                    "hp_potion_30",
                    "mp_potion_30",
                    "stamina_potion_30",
                    "beginner_one_hand_sword",
                    "beginner_two_hand_sword",
                    "beginner_shield",
                    "beginner_armor",
                    "beginner_bow",
                    "beginner_wand",
                    "beginner_staff",
                    "beginner_helmet",
                    "beginner_gloves",
                    "beginner_boots",
                    "short_sword",
                    "long_sword",
                    "wooden_blade",
                    "dagger",
                    "short_bow",
                    "fire_wand",
                    "ice_wand",
                    "lightning_wand",
                    "round_shield",
                    "cuirassier_helm",
                    "light_leather_mail",
                    "studded_cuirassier",
                    "leather_gloves",
                    "cores_thief_gloves",
                    "lorica_gloves",
                    "leather_shoes",
                    "hunter_boots",
                    "combat_shoes",
                    "thief_shoes",
                    "broadsword",
                    "bastard_sword",
                    "battle_sword",
                    "long_bow",
                    "composite_bow",
                    "crossbow",
                    "phoenix_fire_wand",
                    "crown_ice_wand",
                    "crystal_lightning_wand",
                    "trinity_staff",
                    "kite_shield",
                    "ring_mail_helm",
                    "cross_full_helm",
                    "wing_half_helm",
                    "bone_helm",
                    "spiked_helm",
                    "drandos_leather_mail",
                    "three_belt_leather_mail",
                    "melka_chain_mail",
                    "surcoat_chain_mail",
                    "counter_gauntlet",
                    "tork_hunter_gloves",
                    "ulna_protector_gloves",
                    "long_greaves",
                    "leather_protector");

    private final ItemCatalogService itemCatalogService;

    ItemCatalogLoadIntegrationTest(final ItemCatalogService itemCatalogService) {
        this.itemCatalogService = itemCatalogService;
    }

    /** 전체 아이템 수가 11개인지 검증한다. */
    @Test
    void should_loadAllItems_when_applicationStarts() {
        final List<Item> allItems = itemCatalogService.all();

        assertThat(allItems).hasSize(TOTAL_ITEM_COUNT);
    }

    /** 모든 아이템 id가 유일한지 검증한다. */
    @Test
    void should_haveUniqueIds_forAllItems() {
        final List<Item> allItems = itemCatalogService.all();
        final Set<String> ids = new HashSet<>();

        for (final Item item : allItems) {
            assertThat(ids.add(item.id())).as("중복 id가 존재합니다: " + item.id()).isTrue();
        }
    }

    /** 모든 장비 아이템에 maxDurability가 양수로 설정되어 있는지 검증한다. */
    @Test
    void should_haveMaxDurabilityForAllEquipment() {
        final List<Item> allItems = itemCatalogService.all();

        final List<EquipmentItem> equipmentItems =
                allItems.stream()
                        .filter(item -> item.type().isEquipment())
                        .map(EquipmentItem.class::cast)
                        .toList();

        assertThat(equipmentItems).isNotEmpty();

        for (final EquipmentItem equipment : equipmentItems) {
            assertThat(equipment.maxDurability())
                    .as("장비 '%s'의 maxDurability는 양수여야 합니다", equipment.id())
                    .isPositive();
        }
    }

    /** 알려진 아이템 id가 모두 로드되었는지 검증한다. */
    @Test
    void should_containAllKnownItemIds_when_loaded() {
        final List<Item> allItems = itemCatalogService.all();
        final Set<String> loadedIds = new HashSet<>();

        for (final Item item : allItems) {
            loadedIds.add(item.id());
        }

        assertThat(loadedIds).containsAll(KNOWN_ITEM_IDS);
    }

    /** byId로 알려진 아이템을 조회할 수 있는지 검증한다. */
    @Test
    void should_findItemById_when_knownIdUsed() {
        for (final String itemId : KNOWN_ITEM_IDS) {
            assertThat(itemCatalogService.byId(itemId))
                    .as("아이템 '%s'가 byId로 조회되어야 합니다", itemId)
                    .isPresent();
        }
    }

    /** 존재하지 않는 id로 조회하면 빈 Optional을 반환하는지 검증한다. */
    @Test
    void should_returnEmpty_when_unknownIdUsed() {
        assertThat(itemCatalogService.byId("nonexistent_item")).isEmpty();
    }
}
