package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;

import org.mockito.ArgumentCaptor;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 기본 지급 결과 프로퍼티 테스트.
 *
 * <p>신규 캐릭터 시드({@code seedDefault()})에 대해:
 * <ul>
 *   <li>INVENTORY에 초보자 장비 10종 + 포션 1스택(수량 5)이 생성되고</li>
 *   <li>한손검·방패·갑옷만 {@code equipped=true}(양손검 false)이며</li>
 *   <li>모든 지급 장비의 {@code currentDurability == maxDurability(20)}이고</li>
 *   <li>{@code equippedBonus}의 STAT 합이 STR+5·DEF+10이다</li>
 * </ul>
 *
 * <p>Feature: 006-gold-item-inventory, Property 18: 기본 지급 결과
 *
 * <p><b>Validates: Requirements 18.2, 18.3, 18.4, 18.5</b>
 */
class SeedDefaultItemsPropertyTest {

    private static final String POTION_ID = "hp_potion_30";
    private static final String ONE_HAND_SWORD_ID = "beginner_one_hand_sword";
    private static final String TWO_HAND_SWORD_ID = "beginner_two_hand_sword";
    private static final String SHIELD_ID = "beginner_shield";
    private static final String ARMOR_ID = "beginner_armor";
    private static final int MAX_DURABILITY = 20;
    private static final int POTION_QUANTITY = 5;

    // Feature: 006-gold-item-inventory, Property 18: 기본 지급 결과

    /**
     * seedDefault()가 정확히 11개 아이템(장비 10 + 포션 1스택)을 저장함을 검증한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_saveExactlyElevenItems_when_seedDefault(@ForAll("dummyInt") final int dummy) {
        final List<OwnedItem> savedItems = executeSeedAndCapture();

        assertThat(savedItems).hasSize(11);
    }

    /**
     * seedDefault()가 한손검·방패·갑옷 3종만 장착(equipped=true)으로 저장함을 검증한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_equipThreeItems_when_seedDefault(@ForAll("dummyInt") final int dummy) {
        final List<OwnedItem> savedItems = executeSeedAndCapture();

        final List<OwnedItem> equippedItems = savedItems.stream()
                .filter(OwnedItem::isEquipped)
                .toList();

        assertThat(equippedItems).hasSize(3);
        assertThat(equippedItems.stream().map(OwnedItem::getItemId).toList())
                .containsExactlyInAnyOrder(ONE_HAND_SWORD_ID, SHIELD_ID, ARMOR_ID);
    }

    /**
     * seedDefault()가 양손검을 미장착(equipped=false) 상태로 저장함을 검증한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_notEquipTwoHandSword_when_seedDefault(@ForAll("dummyInt") final int dummy) {
        final List<OwnedItem> savedItems = executeSeedAndCapture();

        final OwnedItem twoHandSword = savedItems.stream()
                .filter(item -> TWO_HAND_SWORD_ID.equals(item.getItemId()))
                .findFirst()
                .orElseThrow();

        assertThat(twoHandSword.isEquipped()).isFalse();
    }

    /**
     * seedDefault()가 포션을 수량 5로 저장함을 검증한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_savePotionWithQuantityFive_when_seedDefault(@ForAll("dummyInt") final int dummy) {
        final List<OwnedItem> savedItems = executeSeedAndCapture();

        final OwnedItem potion = savedItems.stream()
                .filter(item -> POTION_ID.equals(item.getItemId()))
                .findFirst()
                .orElseThrow();

        assertThat(potion.getQuantity()).isEqualTo(POTION_QUANTITY);
        assertThat(potion.isEquipped()).isFalse();
    }

    /**
     * seedDefault()가 모든 장비의 현재 내구도를 20(최대)으로 초기화함을 검증한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_initializeDurabilityToMax_when_seedDefault(@ForAll("dummyInt") final int dummy) {
        final List<OwnedItem> savedItems = executeSeedAndCapture();

        final List<OwnedItem> equipmentItems = savedItems.stream()
                .filter(item -> !POTION_ID.equals(item.getItemId()))
                .toList();

        assertThat(equipmentItems).hasSize(10);
        for (final OwnedItem equipment : equipmentItems) {
            assertThat(equipment.getCurrentDurability()).isEqualTo(MAX_DURABILITY);
        }
    }

    /**
     * 기본 장착 장비의 보너스 합산이 STR+5, DEF+10임을 검증한다.
     *
     * <p>한손검(STR+5) + 방패(DEF+5) + 갑옷(DEF+5)이 장착되어
     * equippedBonus의 STAT 보너스가 STR+5, DEF+10이어야 한다.
     *
     * @param dummy 더미 파라미터 (jqwik 프로퍼티 실행 보장)
     */
    @Property(tries = 100)
    void should_yieldStatBonusStrFiveDefTen_when_equippedBonusAfterSeed(
            @ForAll("dummyInt") final int dummy) {

        final List<OwnedItem> savedItems = executeSeedAndCapture();
        final List<OwnedItem> equippedItems = savedItems.stream()
                .filter(OwnedItem::isEquipped)
                .toList();

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(equippedItems);

        setupCatalogForBonusTest(mockCatalog);

        final InventoryService bonusService = new InventoryService(
                mockRepo, mockCatalog, mockProgressRepo, statProgression);

        final EquippedBonusResult result = bonusService.equippedBonus();

        assertThat(result.statBonus().str()).isEqualTo(5);
        assertThat(result.statBonus().defense()).isEqualTo(10);
        assertThat(result.statBonus().dex()).isZero();
        assertThat(result.statBonus().intelligence()).isZero();
        assertThat(result.statBonus().critical()).isZero();
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 더미 정수 Arbitrary 제공자.
     *
     * @return 0~100 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> dummyInt() {
        return Arbitraries.integers().between(0, 100);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * seedDefault()를 실행하고 저장된 OwnedItem 목록을 캡처하여 반환한다.
     *
     * @return 저장된 OwnedItem 리스트
     */
    private List<OwnedItem> executeSeedAndCapture() {
        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final List<OwnedItem> captured = new ArrayList<>();
        when(mockRepo.save(any(OwnedItem.class))).thenAnswer(invocation -> {
            final OwnedItem item = invocation.getArgument(0);
            captured.add(item);
            return item;
        });

        final InventoryService service = new InventoryService(
                mockRepo, mockCatalog, mockProgressRepo, statProgression);
        service.seedDefault();

        return captured;
    }

    /**
     * equippedBonus 테스트를 위해 카탈로그 서비스에 기본 장비 데이터를 설정한다.
     *
     * @param mockCatalog 모의 카탈로그 서비스
     */
    private void setupCatalogForBonusTest(final ItemCatalogService mockCatalog) {
        final EquipmentItem oneHandSword = new EquipmentItem(
                ONE_HAND_SWORD_ID, "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD,
                List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        final EquipmentItem shield = new EquipmentItem(
                SHIELD_ID, "초보자용 방패", ItemType.ARMOR,
                EquipmentKind.SHIELD,
                List.of(new EquipBonus(BonusTarget.DEF, 5)),
                null, MAX_DURABILITY);

        final EquipmentItem armor = new EquipmentItem(
                ARMOR_ID, "초보자용 갑옷", ItemType.ARMOR,
                EquipmentKind.ARMOR_BODY,
                List.of(new EquipBonus(BonusTarget.DEF, 5)),
                null, MAX_DURABILITY);

        when(mockCatalog.byId(ONE_HAND_SWORD_ID)).thenReturn(Optional.of(oneHandSword));
        when(mockCatalog.byId(SHIELD_ID)).thenReturn(Optional.of(shield));
        when(mockCatalog.byId(ARMOR_ID)).thenReturn(Optional.of(armor));
    }
}
