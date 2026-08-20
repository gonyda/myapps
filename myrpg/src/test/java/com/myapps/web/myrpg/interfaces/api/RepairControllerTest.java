package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.RepairItemView;
import com.myapps.web.myrpg.application.dto.RepairView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link RepairController}의 웹 슬라이스 테스트.
 *
 * <p>수리 팝업 렌더, 수리 성공/실패 시드 분기, 모델 검증을 수행한다.
 */
@WebMvcTest(RepairController.class)
class RepairControllerTest {

    private static final String FRAGMENT_REPAIR_POPUP = "fragments/repair-popup :: repair-content";
    private static final long OWNED_ITEM_ID = 1L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private ShopService shopService;

    @MockitoBean private InventoryService inventoryService;

    @MockitoBean private ItemCatalogService itemCatalogService;

    @MockitoBean private OwnedItemRepository ownedItemRepository;

    @MockitoBean private ActionLog actionLog;

    @MockitoBean private Random random;

    /** GET /repair 요청 시 수리 팝업 fragment가 200으로 반환되는지 검증한다. */
    @Test
    void should_returnRepairPopupFragment_when_repairRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final RepairView repairView = dummyRepairView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(dummyOwnedItem()));
        when(itemCatalogService.byId(anyString())).thenReturn(Optional.of(dummyEquipment()));
        when(shopService.sellValueOf(any(OwnedItem.class))).thenReturn(50L);
        when(inventoryService.describe(any(), any(OwnedItem.class)))
                .thenReturn(List.of("한손검 (무기)", "힘 +5", "내구도: 15/20"));

        mockMvc.perform(get("/repair"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_REPAIR_POPUP))
                .andExpect(model().attributeExists("repair"));
    }

    /** POST /repair?ownedItemId=1 수리 성공(95% 판정) 시 갱신된 수리 fragment가 반환되는지 검증한다. */
    @Test
    void should_returnRefreshedFragment_when_repairSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final OwnedItem owned = dummyOwnedItem();
        final RepairView repairView = dummyRepairView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(owned));
        when(itemCatalogService.byId(anyString())).thenReturn(Optional.of(dummyEquipment()));
        when(shopService.sellValueOf(any(OwnedItem.class))).thenReturn(50L);
        when(random.nextInt(100)).thenReturn(0); // 0 < 95 → 성공
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(owned));
        when(inventoryService.describe(any(), any(OwnedItem.class)))
                .thenReturn(List.of("한손검 (무기)", "힘 +5", "내구도: 16/20"));

        mockMvc.perform(post("/repair").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_REPAIR_POPUP))
                .andExpect(model().attributeExists("repair"));

        verify(characterService).saveTurn(progress);
    }

    /** POST /repair?ownedItemId=1 수리 실패(5% 판정) 시에도 갱신된 수리 fragment가 반환되는지 검증한다. */
    @Test
    void should_returnRefreshedFragment_when_repairFails() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final OwnedItem owned = dummyOwnedItem();
        final RepairView repairView = dummyRepairView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(owned));
        when(itemCatalogService.byId(anyString())).thenReturn(Optional.of(dummyEquipment()));
        when(shopService.sellValueOf(any(OwnedItem.class))).thenReturn(50L);
        when(random.nextInt(100)).thenReturn(99); // 99 >= 95 → 실패
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(owned));
        when(inventoryService.describe(any(), any(OwnedItem.class)))
                .thenReturn(List.of("한손검 (무기)", "힘 +5", "내구도: 15/20"));

        mockMvc.perform(post("/repair").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_REPAIR_POPUP))
                .andExpect(model().attributeExists("repair"));

        verify(characterService).saveTurn(progress);
    }

    private OwnedItem dummyOwnedItem() {
        final OwnedItem owned =
                new OwnedItem("beginner_one_hand_sword", 1, StorageKind.INVENTORY, false, 15.0);
        IdTestHelper.setId(owned, OWNED_ITEM_ID);
        return owned;
    }

    private EquipmentItem dummyEquipment() {
        return new EquipmentItem(
                "beginner_one_hand_sword",
                "초보자 한손검",
                ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD,
                List.of(),
                null,
                20);
    }

    private RepairView dummyRepairView() {
        final RepairItemView item =
                new RepairItemView(
                        OWNED_ITEM_ID,
                        "초보자 한손검",
                        "무기",
                        15,
                        20,
                        50L,
                        false,
                        List.of("한손검 (무기)", "힘 +5", "내구도: 15/20"));
        return new RepairView(List.of(item), 1_000L);
    }
}
