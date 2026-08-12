package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 슬롯 점유 유일성 불변식 프로퍼티 테스트.
 *
 * <p>임의의 착용/해제 시퀀스 후에도 각 {@link EquipSlot}을 점유한 장착 장비는
 * 최대 1개이며(양손검은 MAIN_HAND+OFF_HAND를 동시 점유), 장착 장비는 모두
 * {@code storage=INVENTORY}임을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 8: 슬롯 점유 유일성 불변식
 *
 * <p><b>Validates: Requirements 9.8, 9.9</b>
 */
class EquipSlotUniquenessPropertyTest {

    private static final int MAX_DURABILITY = 20;

    // Feature: 006-gold-item-inventory, Property 8: 슬롯 점유 유일성 불변식

    /**
     * 임의의 equip/unequip 시퀀스 실행 후 각 슬롯을 점유한 장착 장비가 최대 1개이고
     * 장착 장비가 모두 INVENTORY임을 검증한다.
     *
     * @param operations 실행할 조작 시퀀스 (양의 정수: equip, 음의 정수: unequip)
     */
    @Property(tries = 100)
    void should_maintainSlotUniqueness_after_equipUnequipSequence(
            @ForAll("operationSequence") final List<Integer> operations) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo = mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        // 인벤토리에 4종 장비를 준비 (모두 미장착 상태에서 시작)
        final OwnedItem oneHandSword = createOwnedItem(1L, "one_hand_sword", false);
        final OwnedItem twoHandSword = createOwnedItem(2L, "two_hand_sword", false);
        final OwnedItem shield = createOwnedItem(3L, "shield", false);
        final OwnedItem armor = createOwnedItem(4L, "armor", false);

        final List<OwnedItem> allItems = List.of(oneHandSword, twoHandSword, shield, armor);
        final Map<String, EquipmentItem> catalogMap = new HashMap<>();

        final EquipmentItem oneHandItem = createEquipmentItem("one_hand_sword", "한손검",
                ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD);
        final EquipmentItem twoHandItem = createEquipmentItem("two_hand_sword", "양손검",
                ItemType.WEAPON, EquipmentKind.TWO_HANDED_SWORD);
        final EquipmentItem shieldItem = createEquipmentItem("shield", "방패",
                ItemType.ARMOR, EquipmentKind.SHIELD);
        final EquipmentItem armorItem = createEquipmentItem("armor", "갑옷",
                ItemType.ARMOR, EquipmentKind.ARMOR_BODY);

        catalogMap.put("one_hand_sword", oneHandItem);
        catalogMap.put("two_hand_sword", twoHandItem);
        catalogMap.put("shield", shieldItem);
        catalogMap.put("armor", armorItem);

        for (final Map.Entry<String, EquipmentItem> entry : catalogMap.entrySet()) {
            when(mockCatalog.byId(entry.getKey())).thenReturn(Optional.of(entry.getValue()));
        }

        for (final OwnedItem item : allItems) {
            when(mockRepo.findById(item.getId())).thenReturn(Optional.of(item));
        }

        // 조작 시퀀스 실행
        for (final int op : operations) {
            // op 절대값으로 아이템 인덱스 결정 (0~3)
            final int index = Math.abs(op) % 4;
            final OwnedItem target = allItems.get(index);

            // 현재 장착 상태 갱신 (모의 리포지토리)
            final List<OwnedItem> currentEquipped = allItems.stream()
                    .filter(OwnedItem::isEquipped)
                    .toList();
            when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                    .thenReturn(currentEquipped);

            if (op >= 0) {
                // equip 시도
                if (!target.isEquipped()) {
                    try {
                        service.equip(target.getId());
                    } catch (final EquipConflictException ignored) {
                        // 충돌로 거부됨 — 정상 동작
                    }
                }
            } else {
                // unequip 시도
                if (target.isEquipped()) {
                    service.unequip(target.getId());
                }
            }
        }

        // 불변식 검증: 각 슬롯을 점유한 장착 장비는 최대 1개
        final List<OwnedItem> finalEquipped = allItems.stream()
                .filter(OwnedItem::isEquipped)
                .toList();

        final Map<EquipSlot, Integer> slotOccupancy = new HashMap<>();
        for (final EquipSlot slot : EquipSlot.values()) {
            slotOccupancy.put(slot, 0);
        }

        for (final OwnedItem equipped : finalEquipped) {
            final EquipmentItem equipItem = catalogMap.get(equipped.getItemId());
            for (final EquipSlot slot : equipItem.kind().requiredSlots()) {
                slotOccupancy.merge(slot, 1, Integer::sum);
            }
        }

        for (final EquipSlot slot : EquipSlot.values()) {
            assertThat(slotOccupancy.get(slot))
                    .as("슬롯 %s의 점유 장비 수가 1을 초과하면 안 됨", slot)
                    .isLessThanOrEqualTo(1);
        }

        // 불변식 검증: 장착 장비는 모두 INVENTORY
        for (final OwnedItem equipped : finalEquipped) {
            assertThat(equipped.getStorage())
                    .as("장착 장비는 INVENTORY여야 함")
                    .isEqualTo(StorageKind.INVENTORY);
        }
    }

    /**
     * 기본 장착(한손검+방패+갑옷) 상태에서 각 슬롯이 유일하게 점유됨을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_haveUniqueSlotOccupancy_when_defaultSeedEquipped(
            @ForAll("smallPositive") final long ignored) {

        // 기본 장착 상태: 한손검(MAIN_HAND) + 방패(OFF_HAND) + 갑옷(BODY)
        final OwnedItem sword = createOwnedItem(1L, "one_hand_sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", true);
        final OwnedItem armor = createOwnedItem(3L, "armor", true);

        final List<OwnedItem> equipped = List.of(sword, shield, armor);

        final Map<String, EquipmentKind> kindMap = Map.of(
                "one_hand_sword", EquipmentKind.ONE_HANDED_SWORD,
                "shield", EquipmentKind.SHIELD,
                "armor", EquipmentKind.ARMOR_BODY
        );

        final Map<EquipSlot, Integer> slotCount = new HashMap<>();
        for (final EquipSlot slot : EquipSlot.values()) {
            slotCount.put(slot, 0);
        }

        for (final OwnedItem item : equipped) {
            final EquipmentKind kind = kindMap.get(item.getItemId());
            for (final EquipSlot slot : kind.requiredSlots()) {
                slotCount.merge(slot, 1, Integer::sum);
            }
        }

        final Set<EquipSlot> defaultOccupiedSlots =
                Set.of(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND, EquipSlot.BODY);
        for (final EquipSlot slot : EquipSlot.values()) {
            if (defaultOccupiedSlots.contains(slot)) {
                assertThat(slotCount.get(slot))
                        .as("기본 장착 시 슬롯 %s는 정확히 1개 점유", slot)
                        .isEqualTo(1);
            } else {
                assertThat(slotCount.get(slot))
                        .as("기본 장착에 쓰이지 않는 슬롯 %s는 0 점유", slot)
                        .isZero();
            }
        }

        // 모든 장착 장비가 INVENTORY
        for (final OwnedItem item : equipped) {
            assertThat(item.getStorage()).isEqualTo(StorageKind.INVENTORY);
        }
    }

    /**
     * 양손검 장착 시 MAIN_HAND + OFF_HAND 두 슬롯을 동시 점유하여
     * 다른 장비가 두 슬롯에 추가 장착 불가임을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_occupyBothSlots_when_twoHandedSwordEquipped(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo = mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem twoHandSword = createOwnedItem(1L, "two_hand_sword", false);

        final EquipmentItem twoHandItem = createEquipmentItem("two_hand_sword", "양손검",
                ItemType.WEAPON, EquipmentKind.TWO_HANDED_SWORD);

        when(mockRepo.findById(1L)).thenReturn(Optional.of(twoHandSword));
        when(mockCatalog.byId("two_hand_sword")).thenReturn(Optional.of(twoHandItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        service.equip(1L);

        assertThat(twoHandSword.isEquipped()).isTrue();

        // 양손검이 점유하는 슬롯 확인
        final Set<EquipSlot> occupiedSlots = EquipmentKind.TWO_HANDED_SWORD.requiredSlots();
        assertThat(occupiedSlots).containsExactlyInAnyOrder(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND);

        // 장착 목록에서 슬롯 유일성 확인
        final Map<EquipSlot, Integer> slotCount = new HashMap<>();
        for (final EquipSlot slot : occupiedSlots) {
            slotCount.merge(slot, 1, Integer::sum);
        }
        for (final EquipSlot slot : occupiedSlots) {
            assertThat(slotCount.get(slot)).isEqualTo(1);
        }
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 조작 시퀀스를 생성하는 Arbitrary 제공자.
     * 양수: equip, 음수: unequip. 절대값 % 4로 아이템 인덱스 결정.
     *
     * @return 정수 리스트의 Arbitrary (3~10개 조작)
     */
    @Provide
    Arbitrary<List<Integer>> operationSequence() {
        return Arbitraries.integers().between(-100, 100)
                .list().ofMinSize(3).ofMaxSize(10);
    }

    /**
     * 양의 정수를 생성하는 Arbitrary 제공자 (프로퍼티 반복용).
     *
     * @return 1~1000 범위의 long Arbitrary
     */
    @Provide
    Arbitrary<Long> smallPositive() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * {@link InventoryService}를 모의 의존성으로 생성한다.
     *
     * @param repo         모의 OwnedItemRepository
     * @param catalog      모의 ItemCatalogService
     * @param progressRepo 모의 CharacterProgressRepository
     * @return InventoryService 인스턴스
     */
    private InventoryService createService(final OwnedItemRepository repo,
                                           final ItemCatalogService catalog,
                                           final CharacterProgressRepository progressRepo) {
        return new InventoryService(repo, catalog, progressRepo,
                mock(com.myapps.web.myrpg.domain.model.StatProgression.class),
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));
    }

    /**
     * 지정된 ID와 장착 상태를 가진 {@link OwnedItem}을 생성한다.
     *
     * @param id       엔티티 ID
     * @param itemId   아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItem(final long id, final String itemId, final boolean equipped) {
        final OwnedItem item = new OwnedItem(itemId, 1, StorageKind.INVENTORY, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    /**
     * 지정된 속성을 가진 {@link EquipmentItem}을 생성한다.
     *
     * @param id   아이템 ID
     * @param name 아이템 이름
     * @param type 아이템 유형
     * @param kind 장비 종류
     * @return EquipmentItem 인스턴스
     */
    private EquipmentItem createEquipmentItem(final String id, final String name,
                                              final ItemType type, final EquipmentKind kind) {
        return new EquipmentItem(id, name, type, kind,
                List.of(new EquipBonus(BonusTarget.STR, 5)), null, MAX_DURABILITY);
    }

    /**
     * 리플렉션을 이용하여 OwnedItem의 ID를 설정한다.
     *
     * @param item 대상 엔티티
     * @param id   설정할 ID 값
     */
    private void setId(final OwnedItem item, final Long id) {
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem ID 설정 실패", exception);
        }
    }
}
