package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

/**
 * 상점 판매 시 장착 중 장비 보호 및 1개 단위 처리 프로퍼티 테스트.
 *
 * <p>장착 중인 장비 판매는 항상 {@link EquipConflictException}으로 거부되고 골드·수량·장착 상태가 불변임을 검증한다. 미장착 아이템은 1개 단위로
 * 판매되어 수량이 정확히 1 감소하고 골드가 판매가만큼 증가하며, 수량이 0이 되면 저장소에서 행이 삭제됨을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 4: 상점 판매 시 장착 중 장비 보호 및 1개 단위 처리
 *
 * <p><b>Validates: Requirements 1.8, 4.1, 4.2, 4.4, 5.2, 5.3, 5.4, 5.5, 15.2</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 4: 상점 판매 시 장착 중 장비 보호 및 1개 단위 처리
class ShopServiceSellEquippedProtectionPropertyTest {

    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;
    private static final String POTION_ID = "hp_potion_30";
    private static final long SELL_VALUE = 25L;

    /**
     * 장착 중인 장비 판매 시도는 항상 거부되고 골드·수량·장착 상태가 불변임을 검증한다.
     *
     * @param gold 보유 골드 (0~10000)
     * @param quantity 보유 수량 (1~10)
     */
    @Property(tries = 100)
    void should_rejectSell_whenEquipped(
            @ForAll @LongRange(min = 0, max = 10_000) final long gold,
            @ForAll @IntRange(min = 1, max = 10) final int quantity) {
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItem equippedItem = createOwnedItem(1L, POTION_ID, quantity, true);

        when(repository.findById(1L)).thenReturn(Optional.of(equippedItem));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "포션", 30, 50)));

        final ShopService service = newService(catalog, repository);
        final CharacterProgress progress = createProgress(gold);

        assertThatThrownBy(() -> service.sell(progress, 1L))
                .isInstanceOf(EquipConflictException.class);

        assertThat(progress.getGold()).isEqualTo(gold);
        assertThat(equippedItem.getQuantity()).isEqualTo(quantity);
        assertThat(equippedItem.isEquipped()).isTrue();
        verify(repository, never()).delete(equippedItem);
    }

    /**
     * 미장착 1개 이상 스택 판매 시 수량이 정확히 1 감소하고 골드가 판매가만큼 증가함을 검증한다.
     *
     * @param tuple (보유 골드, 수량) 쌍 (수량 2~10)
     */
    @Property(tries = 100)
    void should_decreaseQuantityByOne_andGainGold_when_unequippedStack(
            @ForAll("goldAndQuantity") final Tuple.Tuple2<Long, Integer> tuple) {
        final long gold = tuple.get1();
        final int quantity = tuple.get2();

        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItem potionStack = createOwnedItem(2L, POTION_ID, quantity, false);

        when(repository.findById(2L)).thenReturn(Optional.of(potionStack));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "포션", 30, 50)));

        final ShopService service = newService(catalog, repository);
        final CharacterProgress progress = createProgress(gold);

        service.sell(progress, 2L);

        assertThat(potionStack.getQuantity()).isEqualTo(quantity - 1);
        assertThat(progress.getGold()).isEqualTo(gold + SELL_VALUE);
        verify(repository, never()).delete(potionStack);
    }

    /**
     * 수량 1 아이템 판매 시 수량이 0이 되고 저장소에서 행이 삭제되며 골드가 지급됨을 검증한다.
     *
     * @param gold 보유 골드 (0~10000)
     */
    @Property(tries = 100)
    void should_deleteRow_when_quantityReachesZero(
            @ForAll @LongRange(min = 0, max = 10_000) final long gold) {
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItem singlePotion = createOwnedItem(3L, POTION_ID, 1, false);

        when(repository.findById(3L)).thenReturn(Optional.of(singlePotion));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "포션", 30, 50)));

        final ShopService service = newService(catalog, repository);
        final CharacterProgress progress = createProgress(gold);

        service.sell(progress, 3L);

        assertThat(singlePotion.getQuantity()).isZero();
        assertThat(progress.getGold()).isEqualTo(gold + SELL_VALUE);
        verify(repository).delete(singlePotion);
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * (보유 골드, 수량) 쌍을 생성한다. 수량은 2~10으로 보장한다.
     *
     * @return (gold, quantity) Tuple Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Integer>> goldAndQuantity() {
        return Arbitraries.longs()
                .between(0L, 10_000L)
                .flatMap(
                        gold ->
                                Arbitraries.integers()
                                        .between(2, 10)
                                        .map(quantity -> Tuple.of(gold, quantity)));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * 지정된 속성의 OwnedItem을 생성한다.
     *
     * @param id 엔티티 ID
     * @param itemId 아이템 카탈로그 ID
     * @param quantity 보유 수량
     * @param equipped 장착 여부
     * @return OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItem(
            final long id, final String itemId, final int quantity, final boolean equipped) {
        final OwnedItem ownedItem =
                new OwnedItem(itemId, quantity, StorageKind.INVENTORY, equipped, 0.0);
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ownedItem, id);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem ID 설정 실패", exception);
        }
        return ownedItem;
    }

    /**
     * 모의 의존성으로 ShopService를 생성한다.
     *
     * @param catalog 모의 ItemCatalogService
     * @param repository 모의 OwnedItemRepository
     * @return ShopService 인스턴스
     */
    private ShopService newService(
            final ItemCatalogService catalog, final OwnedItemRepository repository) {
        return new ShopService(
                catalog,
                mock(NpcService.class),
                repository,
                mock(InventoryService.class),
                mock(CharacterService.class),
                fixedActionLog());
    }

    /**
     * 지정 골드를 가진 CharacterProgress를 생성한다.
     *
     * @param gold 초기 골드
     * @return CharacterProgress 인스턴스
     */
    private CharacterProgress createProgress(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", 0, gold);
    }

    private ActionLog fixedActionLog() {
        return new ActionLog(
                Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }
}
