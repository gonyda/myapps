package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.ShopBuyItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * ShopService 핵심 메서드의 단위 테스트.
 *
 * <p>대표 판매가(포션 25, 초보한손검 50, 초보활 110, 숏소드 150, 롱소드 350)를 검증하고, 구매 성공/거부(비매품 NPC·buyPrice 미지정·골드 부족)
 * 및 판매 성공(1개 단위 감소, 0개 행 삭제, 골드 지급)/거부(장착 장비)를 검증한다.
 */
class ShopServiceTest {

    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;
    private static final String FERGHUS_ID = "ferghus";
    private static final String SHORT_SWORD_ID = "short_sword";
    private static final String LONG_SWORD_ID = "long_sword";
    private static final String POTION_ID = "hp_potion_30";
    private static final String BEGINNER_BOW_ID = "beginner_bow";

    // ─── 대표 판매가 검증 ─────────────────────────────────────────────────────

    /** 상점 구매가가 있는 아이템(포션·숏소드·롱소드)과 카탈로그 보너스 계산식인 초보자 장비의 대표 판매가를 검증한다. */
    @Test
    void should_calculateRepresentativeSellValues() {
        final ActionLog actionLog = fixedActionLog();
        final ShopService service =
                serviceWith(
                        mock(ItemCatalogService.class),
                        mock(NpcService.class),
                        mock(OwnedItemRepository.class),
                        mock(InventoryService.class),
                        actionLog);

        final PotionItem potion = new PotionItem(POTION_ID, "생명력 30 포션", 30, 50);
        assertThat(service.calculateSellValue(potion)).isEqualTo(25L);

        final EquipmentItem beginnerSword =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        20);
        assertThat(service.calculateSellValue(beginnerSword)).isEqualTo(50L);

        final EquipmentItem beginnerBow =
                new EquipmentItem(
                        BEGINNER_BOW_ID,
                        "초보자용 활",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(
                                new EquipBonus(BonusTarget.DEX, 10),
                                new EquipBonus(BonusTarget.CRITICAL, 10)),
                        null,
                        20);
        assertThat(service.calculateSellValue(beginnerBow)).isEqualTo(110L);

        final EquipmentItem shortSword =
                new EquipmentItem(
                        SHORT_SWORD_ID,
                        "숏소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                        300,
                        15);
        assertThat(service.calculateSellValue(shortSword)).isEqualTo(150L);

        final EquipmentItem longSword =
                new EquipmentItem(
                        LONG_SWORD_ID,
                        "롱소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 12)),
                        700,
                        15);
        assertThat(service.calculateSellValue(longSword)).isEqualTo(350L);
    }

    // ─── 구매 ──────────────────────────────────────────────────────────────

    @Test
    void should_buyItem_when_npcSellsItemAndGoldEnough() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final NpcService npcService = mock(NpcService.class);
        final ActionLog actionLog = fixedActionLog();

        when(npcService.byId(FERGHUS_ID))
                .thenReturn(
                        Optional.of(
                                new Npc(
                                        FERGHUS_ID,
                                        "퍼거스",
                                        NpcType.BLACKSMITH,
                                        "tir-chonaill",
                                        "호탕한 대장장이",
                                        new NpcLines(List.of("어서 오게."), null),
                                        List.of(SHORT_SWORD_ID))));
        final EquipmentItem shortSword =
                new EquipmentItem(
                        SHORT_SWORD_ID,
                        "숏소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                        300,
                        15);
        when(catalog.byId(SHORT_SWORD_ID)).thenReturn(Optional.of(shortSword));

        final ShopService service =
                serviceWith(
                        catalog,
                        npcService,
                        mock(OwnedItemRepository.class),
                        inventoryService,
                        actionLog);
        final CharacterProgress progress = createProgress(1000L);

        service.buy(progress, FERGHUS_ID, SHORT_SWORD_ID);

        assertThat(progress.getGold()).isEqualTo(700L);
        verify(inventoryService).acquireItem(SHORT_SWORD_ID, 1);
        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals("아이템을 구매했습니다: 숏소드"));
    }

    @Test
    void should_rejectBuy_when_npcDoesNotSellItem() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final NpcService npcService = mock(NpcService.class);

        when(npcService.byId(FERGHUS_ID))
                .thenReturn(
                        Optional.of(
                                new Npc(
                                        FERGHUS_ID,
                                        "퍼거스",
                                        NpcType.BLACKSMITH,
                                        "tir-chonaill",
                                        "호탕한 대장갑",
                                        new NpcLines(List.of("어서 오게."), null),
                                        List.of())));
        when(catalog.byId(SHORT_SWORD_ID))
                .thenReturn(
                        Optional.of(
                                new EquipmentItem(
                                        SHORT_SWORD_ID,
                                        "숏소드",
                                        ItemType.WEAPON,
                                        EquipmentKind.ONE_HANDED_SWORD,
                                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                                        300,
                                        15)));

        final ShopService service =
                serviceWith(
                        catalog,
                        npcService,
                        mock(OwnedItemRepository.class),
                        inventoryService,
                        fixedActionLog());
        final CharacterProgress progress = createProgress(1000L);

        assertThatThrownBy(() -> service.buy(progress, FERGHUS_ID, SHORT_SWORD_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(progress.getGold()).isEqualTo(1000L);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    @Test
    void should_rejectBuy_when_itemHasNoBuyPrice() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final NpcService npcService = mock(NpcService.class);

        when(npcService.byId(FERGHUS_ID))
                .thenReturn(
                        Optional.of(
                                new Npc(
                                        FERGHUS_ID,
                                        "퍼거스",
                                        NpcType.BLACKSMITH,
                                        "tir-chonaill",
                                        "호탕한 대장갑",
                                        new NpcLines(List.of("어서 오게."), null),
                                        List.of("beginner_one_hand_sword"))));
        when(catalog.byId("beginner_one_hand_sword"))
                .thenReturn(
                        Optional.of(
                                new EquipmentItem(
                                        "beginner_one_hand_sword",
                                        "초보자용 한손검",
                                        ItemType.WEAPON,
                                        EquipmentKind.ONE_HANDED_SWORD,
                                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                                        null,
                                        20)));

        final ShopService service =
                serviceWith(
                        catalog,
                        npcService,
                        mock(OwnedItemRepository.class),
                        inventoryService,
                        fixedActionLog());
        final CharacterProgress progress = createProgress(1000L);

        assertThatThrownBy(() -> service.buy(progress, FERGHUS_ID, "beginner_one_hand_sword"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(progress.getGold()).isEqualTo(1000L);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    @Test
    void should_rejectBuy_when_goldInsufficient() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final NpcService npcService = mock(NpcService.class);

        when(npcService.byId(FERGHUS_ID))
                .thenReturn(
                        Optional.of(
                                new Npc(
                                        FERGHUS_ID,
                                        "퍼거스",
                                        NpcType.BLACKSMITH,
                                        "tir-chonaill",
                                        "호탕한 대장갑",
                                        new NpcLines(List.of("어서 오게."), null),
                                        List.of(POTION_ID))));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "생명력 30 포션", 30, 50)));

        final ShopService service =
                serviceWith(
                        catalog,
                        npcService,
                        mock(OwnedItemRepository.class),
                        inventoryService,
                        fixedActionLog());
        final CharacterProgress progress = createProgress(30L);

        assertThatThrownBy(() -> service.buy(progress, FERGHUS_ID, POTION_ID))
                .isInstanceOf(InsufficientGoldException.class);
        assertThat(progress.getGold()).isEqualTo(30L);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    // ─── 판매 ──────────────────────────────────────────────────────────────

    @Test
    void should_sellOneItem_andGainGold_when_unequipped() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ActionLog actionLog = fixedActionLog();

        final OwnedItem potionStack = createOwnedItem(10L, POTION_ID, 2, false);
        when(repository.findById(10L)).thenReturn(Optional.of(potionStack));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "생명력 30 포션", 30, 50)));

        final ShopService service =
                serviceWith(catalog, repository, mock(InventoryService.class), actionLog);
        final CharacterProgress progress = createProgress(100L);

        service.sell(progress, 10L);

        assertThat(progress.getGold()).isEqualTo(125L);
        assertThat(potionStack.getQuantity()).isEqualTo(1);
        verify(repository, never()).delete(potionStack);
        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals("아이템을 판매했습니다: 생명력 30 포션"));
    }

    @Test
    void should_sellLastItem_andDeleteRow_whenQuantityReachesZero() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ShopService service =
                serviceWith(catalog, repository, mock(InventoryService.class), fixedActionLog());

        final OwnedItem potionStack = createOwnedItem(11L, POTION_ID, 1, false);
        when(repository.findById(11L)).thenReturn(Optional.of(potionStack));
        when(catalog.byId(POTION_ID))
                .thenReturn(Optional.of(new PotionItem(POTION_ID, "생명력 30 포션", 30, 50)));

        final CharacterProgress progress = createProgress(0L);

        service.sell(progress, 11L);

        assertThat(progress.getGold()).isEqualTo(25L);
        assertThat(potionStack.getQuantity()).isZero();
        verify(repository).delete(potionStack);
    }

    @Test
    void should_rejectSell_when_equipped() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ActionLog actionLog = fixedActionLog();

        final OwnedItem equippedSword = createOwnedItem(12L, SHORT_SWORD_ID, 1, true);
        when(repository.findById(12L)).thenReturn(Optional.of(equippedSword));
        when(catalog.byId(SHORT_SWORD_ID))
                .thenReturn(
                        Optional.of(
                                new EquipmentItem(
                                        SHORT_SWORD_ID,
                                        "숏소드",
                                        ItemType.WEAPON,
                                        EquipmentKind.ONE_HANDED_SWORD,
                                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                                        300,
                                        15)));

        final ShopService service =
                serviceWith(catalog, repository, mock(InventoryService.class), actionLog);
        final CharacterProgress progress = createProgress(500L);

        assertThatThrownBy(() -> service.sell(progress, 12L))
                .isInstanceOf(EquipConflictException.class);
        assertThat(progress.getGold()).isEqualTo(500L);
        assertThat(equippedSword.getQuantity()).isEqualTo(1);
        verify(repository, never()).delete(equippedSword);
    }

    @Test
    void should_rejectSell_when_assignedToInactiveWeaponSet() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        final ActionLog actionLog = fixedActionLog();

        final OwnedItem inactiveSetSword = createOwnedItem(15L, SHORT_SWORD_ID, 1, false);
        when(repository.findById(15L)).thenReturn(Optional.of(inactiveSetSword));

        final ShopService service =
                serviceWith(catalog, repository, mock(InventoryService.class), actionLog);
        final CharacterProgress progress = createProgress(500L);
        progress.setActiveWeaponSet(2); // 2번 세트 활성
        progress.setWeapon1MainId(15L); // 1번 세트에 15L 등록됨

        assertThatThrownBy(() -> service.sell(progress, 15L))
                .isInstanceOf(EquipConflictException.class)
                .hasMessage("장착을 해제한 후 판매할 수 있습니다.");
        assertThat(progress.getGold()).isEqualTo(500L);
        verify(repository, never()).delete(inactiveSetSword);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private ShopService serviceWith(
            final ItemCatalogService itemCatalogService,
            final NpcService npcService,
            final OwnedItemRepository repository,
            final InventoryService inventoryService,
            final ActionLog actionLog) {
        return new ShopService(
                itemCatalogService,
                npcService,
                repository,
                inventoryService,
                mock(CharacterService.class),
                actionLog);
    }

    @Test
    void should_buildShopBuyList_withEquipmentMaxDurability() {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final NpcService npcService = mock(NpcService.class);
        final ActionLog actionLog = fixedActionLog();

        when(npcService.byId(FERGHUS_ID))
                .thenReturn(
                        Optional.of(
                                new Npc(
                                        FERGHUS_ID,
                                        "퍼거스",
                                        NpcType.BLACKSMITH,
                                        "tir-chonaill",
                                        "호탕한 대장장이",
                                        new NpcLines(List.of("어서 오게."), null),
                                        List.of(SHORT_SWORD_ID))));
        final EquipmentItem shortSword =
                new EquipmentItem(
                        SHORT_SWORD_ID,
                        "숏소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                        300,
                        15);
        when(catalog.byId(SHORT_SWORD_ID)).thenReturn(Optional.of(shortSword));
        when(inventoryService.describe(eq(shortSword), any(OwnedItem.class)))
                .thenAnswer(
                        invocation -> {
                            final OwnedItem owned = invocation.getArgument(1);
                            return List.of(
                                    "한손검",
                                    "체력 +8",
                                    "내구도: "
                                            + (int) owned.getCurrentDurability()
                                            + "/"
                                            + shortSword.maxDurability());
                        });

        final ShopService service =
                serviceWith(
                        catalog,
                        npcService,
                        mock(OwnedItemRepository.class),
                        inventoryService,
                        actionLog);

        final List<ShopBuyItemView> buyList = service.shopBuyList(FERGHUS_ID);

        assertThat(buyList).hasSize(1);
        assertThat(buyList.get(0).name()).isEqualTo("숏소드");
        assertThat(buyList.get(0).detailLines()).contains("내구도: 15/15");
    }

    private ShopService serviceWith(
            final ItemCatalogService itemCatalogService,
            final OwnedItemRepository repository,
            final InventoryService inventoryService,
            final ActionLog actionLog) {
        return new ShopService(
                itemCatalogService,
                mock(NpcService.class),
                repository,
                inventoryService,
                mock(CharacterService.class),
                actionLog);
    }

    private ActionLog fixedActionLog() {
        return new ActionLog(
                Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }

    private CharacterProgress createProgress(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", 0, gold);
    }

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
}
