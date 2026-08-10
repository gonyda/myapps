package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ItemType;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link InventoryController}의 웹 슬라이스 테스트.
 *
 * <p>인벤토리 목록 조회, 포션 사용, 장비 착용/해제,
 * 착용 충돌 시 예외 처리를 검증한다.
 */
@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    private static final String FRAGMENT_INVENTORY_CONTENT = "fragments/inventory-popup :: inventory-content";
    private static final long OWNED_ITEM_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private CharacterService characterService;

    @MockitoBean
    private ActionLog actionLog;

    /**
     * GET /inventory 요청 시 인벤토리 팝업 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnInventoryPopupFragment_when_inventoryRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final InventoryView inventoryView = dummyInventoryView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildInventoryView(progress.getGold())).thenReturn(inventoryView);

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_INVENTORY_CONTENT))
                .andExpect(model().attributeExists("inventory"));
    }

    /**
     * POST /inventory/use 요청 시 포션을 사용하고 갱신된 인벤토리 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_potionUsed() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final InventoryView inventoryView = dummyInventoryView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildInventoryView(progress.getGold())).thenReturn(inventoryView);

        mockMvc.perform(post("/inventory/use").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_INVENTORY_CONTENT))
                .andExpect(model().attributeExists("inventory"));

        verify(inventoryService).usePotion(OWNED_ITEM_ID);
    }

    /**
     * POST /inventory/equip 요청 시 장비를 착용하고 갱신된 인벤토리 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_itemEquipped() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final InventoryView inventoryView = dummyInventoryView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildInventoryView(progress.getGold())).thenReturn(inventoryView);

        mockMvc.perform(post("/inventory/equip").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_INVENTORY_CONTENT))
                .andExpect(model().attributeExists("inventory"));

        verify(inventoryService).equip(OWNED_ITEM_ID);
    }

    /**
     * POST /inventory/unequip 요청 시 장비를 해제하고 갱신된 인벤토리 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_itemUnequipped() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final InventoryView inventoryView = dummyInventoryView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildInventoryView(progress.getGold())).thenReturn(inventoryView);

        mockMvc.perform(post("/inventory/unequip").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_INVENTORY_CONTENT))
                .andExpect(model().attributeExists("inventory"));

        verify(inventoryService).unequip(OWNED_ITEM_ID);
    }

    /**
     * POST /inventory/equip 시 착용 슬롯 충돌이 발생하면 {@link EquipConflictException}이
     * {@code GlobalExceptionHandler}에서 처리되어 에러 뷰가 반환되는지 검증한다.
     */
    @Test
    void should_returnErrorView_when_equipConflictOccurs() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        doThrow(new EquipConflictException("착용 할 수 없습니다."))
                .when(inventoryService).equip(OWNED_ITEM_ID);

        mockMvc.perform(post("/inventory/equip").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    private InventoryView dummyInventoryView() {
        final OwnedItemView potionView = new OwnedItemView(
                1L, "생명력 50 포션", "포션", ItemType.POTION,
                5, false, true, false,
                null, null,
                List.of("생명력을 50 회복한다."));

        final OwnedItemView weaponView = new OwnedItemView(
                2L, "초보자 한손검", "무기", ItemType.WEAPON,
                1, true, false, true,
                20.0, 20,
                List.of("한손검 (무기)", "힘 +5", "내구도: 20/20"));

        return new InventoryView(0L, List.of(potionView, weaponView));
    }
}
