package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.spring.JqwikSpringSupport;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OwnedItem 엔티티의 영속 라운드트립 프로퍼티 테스트.
 *
 * <p>저장 후 조회 시 itemId·quantity·storage·equipped·currentDurability가
 * 모두 보존되는지 검증하며, 리포지토리 쿼리 메서드의 정확성도 확인한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 17: 영속 라운드트립
 *
 * <p><b>Validates: Requirements 7.1, 7.6</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OwnedItemPersistencePropertyTest {

    private static final int ITEM_ID_MIN_LENGTH = 3;
    private static final int ITEM_ID_MAX_LENGTH = 30;
    private static final int QUANTITY_MIN = 1;
    private static final int QUANTITY_MAX = 99;
    private static final double DURABILITY_MIN = 0.0;
    private static final double DURABILITY_MAX = 100.0;

    private final TestEntityManager entityManager;
    private final OwnedItemRepository repository;

    OwnedItemPersistencePropertyTest(final TestEntityManager entityManager,
                                     final OwnedItemRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    // Feature: 006-gold-item-inventory, Property 17: 영속 라운드트립

    /**
     * 임의의 유효한 OwnedItem을 저장 후 findById로 조회하면
     * itemId·quantity·storage·equipped·currentDurability가 모두 보존되는지 검증한다.
     *
     * @param itemId            임의 아이템 카탈로그 ID
     * @param quantity          보유 수량
     * @param storage           저장 위치
     * @param equipped          장착 여부
     * @param currentDurability 현재 내구도
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_savedAndFoundById(
            @ForAll("itemIds") final String itemId,
            @ForAll("quantities") final int quantity,
            @ForAll("storages") final StorageKind storage,
            @ForAll("equippedFlags") final boolean equipped,
            @ForAll("durabilities") final double currentDurability) {

        final boolean effectiveEquipped = resolveEquipped(storage, equipped);

        final OwnedItem item = new OwnedItem(itemId, quantity, storage, effectiveEquipped, currentDurability);

        entityManager.persistAndFlush(item);
        final Long savedId = item.getId();
        entityManager.clear();

        final Optional<OwnedItem> found = repository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getItemId()).isEqualTo(itemId);
        assertThat(found.get().getQuantity()).isEqualTo(quantity);
        assertThat(found.get().getStorage()).isEqualTo(storage);
        assertThat(found.get().isEquipped()).isEqualTo(effectiveEquipped);
        assertThat(found.get().getCurrentDurability()).isEqualTo(currentDurability);
    }

    /**
     * 지정 저장 위치에 여러 아이템을 저장 후 findByStorageOrderById로 조회하면
     * 해당 저장소 아이템만 ID 오름차순으로 반환되는지 검증한다.
     *
     * @param itemId1   첫 번째 아이템 ID
     * @param itemId2   두 번째 아이템 ID
     * @param quantity  보유 수량
     * @param durability 현재 내구도
     */
    @Property(tries = 100)
    void should_returnCorrectItems_when_findByStorageOrderById(
            @ForAll("itemIds") final String itemId1,
            @ForAll("itemIds") final String itemId2,
            @ForAll("quantities") final int quantity,
            @ForAll("durabilities") final double durability) {

        final OwnedItem inventoryItem1 = new OwnedItem(itemId1, quantity, StorageKind.INVENTORY, false, durability);
        final OwnedItem inventoryItem2 = new OwnedItem(itemId2, quantity, StorageKind.INVENTORY, false, durability);
        final OwnedItem bankItem = new OwnedItem(itemId1, quantity, StorageKind.BANK, false, durability);

        entityManager.persistAndFlush(inventoryItem1);
        entityManager.persistAndFlush(inventoryItem2);
        entityManager.persistAndFlush(bankItem);
        entityManager.clear();

        final List<OwnedItem> inventoryItems = repository.findByStorageOrderById(StorageKind.INVENTORY);

        assertThat(inventoryItems).hasSize(2);
        assertThat(inventoryItems).allMatch(i -> i.getStorage() == StorageKind.INVENTORY);
        assertThat(inventoryItems.get(0).getId()).isLessThan(inventoryItems.get(1).getId());
    }

    /**
     * 지정 저장 위치와 itemId로 조회 시 정확한 아이템이 반환되는지 검증한다.
     *
     * @param itemId    대상 아이템 ID
     * @param quantity  보유 수량
     * @param durability 현재 내구도
     */
    @Property(tries = 100)
    void should_returnMatchingItem_when_findByStorageAndItemId(
            @ForAll("itemIds") final String itemId,
            @ForAll("quantities") final int quantity,
            @ForAll("durabilities") final double durability) {

        final OwnedItem inventoryItem = new OwnedItem(itemId, quantity, StorageKind.INVENTORY, false, durability);
        entityManager.persistAndFlush(inventoryItem);
        entityManager.clear();

        final Optional<OwnedItem> found = repository.findByStorageAndItemId(StorageKind.INVENTORY, itemId);

        assertThat(found).isPresent();
        assertThat(found.get().getItemId()).isEqualTo(itemId);
        assertThat(found.get().getStorage()).isEqualTo(StorageKind.INVENTORY);
        assertThat(found.get().getQuantity()).isEqualTo(quantity);
    }

    /**
     * 지정 저장 위치의 항목 수가 정확히 반환되는지 검증한다.
     *
     * @param itemId1   첫 번째 아이템 ID
     * @param itemId2   두 번째 아이템 ID
     * @param quantity  보유 수량
     * @param durability 현재 내구도
     */
    @Property(tries = 100)
    void should_returnCorrectCount_when_countByStorage(
            @ForAll("itemIds") final String itemId1,
            @ForAll("itemIds") final String itemId2,
            @ForAll("quantities") final int quantity,
            @ForAll("durabilities") final double durability) {

        final OwnedItem bankItem1 = new OwnedItem(itemId1, quantity, StorageKind.BANK, false, durability);
        final OwnedItem bankItem2 = new OwnedItem(itemId2, quantity, StorageKind.BANK, false, durability);
        final OwnedItem inventoryItem = new OwnedItem(itemId1, quantity, StorageKind.INVENTORY, false, durability);

        entityManager.persistAndFlush(bankItem1);
        entityManager.persistAndFlush(bankItem2);
        entityManager.persistAndFlush(inventoryItem);
        entityManager.clear();

        final long bankCount = repository.countByStorage(StorageKind.BANK);
        final long inventoryCount = repository.countByStorage(StorageKind.INVENTORY);

        assertThat(bankCount).isEqualTo(2L);
        assertThat(inventoryCount).isEqualTo(1L);
    }

    /**
     * 지정 저장 위치에서 장착 중인 장비만 반환되는지 검증한다.
     *
     * @param itemId1   장착 중 아이템 ID
     * @param itemId2   미장착 아이템 ID
     * @param quantity  보유 수량
     * @param durability 현재 내구도
     */
    @Property(tries = 100)
    void should_returnOnlyEquipped_when_findByStorageAndEquippedTrue(
            @ForAll("itemIds") final String itemId1,
            @ForAll("itemIds") final String itemId2,
            @ForAll("quantities") final int quantity,
            @ForAll("durabilities") final double durability) {

        final OwnedItem equippedItem = new OwnedItem(itemId1, quantity, StorageKind.INVENTORY, true, durability);
        final OwnedItem unequippedItem = new OwnedItem(itemId2, quantity, StorageKind.INVENTORY, false, durability);
        final OwnedItem bankItem = new OwnedItem(itemId1, quantity, StorageKind.BANK, false, durability);

        entityManager.persistAndFlush(equippedItem);
        entityManager.persistAndFlush(unequippedItem);
        entityManager.persistAndFlush(bankItem);
        entityManager.clear();

        final List<OwnedItem> equipped = repository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);

        assertThat(equipped).hasSize(1);
        assertThat(equipped.get(0).getItemId()).isEqualTo(itemId1);
        assertThat(equipped.get(0).isEquipped()).isTrue();
        assertThat(equipped.get(0).getStorage()).isEqualTo(StorageKind.INVENTORY);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * 비즈니스 규칙에 따라 장착 여부를 결정한다.
     * BANK 저장소의 아이템은 장착할 수 없으므로 항상 false를 반환한다.
     *
     * @param storage  저장 위치
     * @param equipped 원래 장착 의도
     * @return 실효 장착 여부
     */
    private boolean resolveEquipped(final StorageKind storage, final boolean equipped) {
        if (storage == StorageKind.BANK) {
            return false;
        }
        return equipped;
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 아이템 카탈로그 ID Arbitrary를 제공한다 (3~30자, 알파벳 소문자 + 언더스코어).
     *
     * @return 아이템 ID Arbitrary
     */
    @Provide
    Arbitrary<String> itemIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('_')
                .ofMinLength(ITEM_ID_MIN_LENGTH)
                .ofMaxLength(ITEM_ID_MAX_LENGTH);
    }

    /**
     * 보유 수량 Arbitrary를 제공한다 (1~99).
     *
     * @return 보유 수량 Arbitrary
     */
    @Provide
    Arbitrary<Integer> quantities() {
        return Arbitraries.integers().between(QUANTITY_MIN, QUANTITY_MAX);
    }

    /**
     * 저장 위치 Arbitrary를 제공한다 (INVENTORY 또는 BANK).
     *
     * @return 저장 위치 Arbitrary
     */
    @Provide
    Arbitrary<StorageKind> storages() {
        return Arbitraries.of(StorageKind.values());
    }

    /**
     * 장착 여부 Arbitrary를 제공한다 (true 또는 false).
     *
     * @return 장착 여부 Arbitrary
     */
    @Provide
    Arbitrary<Boolean> equippedFlags() {
        return Arbitraries.of(true, false);
    }

    /**
     * 현재 내구도 Arbitrary를 제공한다 (0.0~100.0, 소수 첫째 자리까지).
     *
     * @return 현재 내구도 Arbitrary
     */
    @Provide
    Arbitrary<Double> durabilities() {
        return Arbitraries.integers().between(0, 1000)
                .map(i -> i / 10.0);
    }
}
