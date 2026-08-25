package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.EquipmentSlotView;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.VitalMax;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link EquipmentController}의 웹 슬라이스 테스트. */
@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {

    private static final String FRAGMENT_EQUIPMENT_CONTENT =
            "fragments/equipment-popup :: equipment-content";
    private static final String FRAGMENT_CANDIDATES =
            "fragments/equipment-popup :: equippable-candidates";
    private static final long OWNED_ITEM_ID = 10L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private InventoryService inventoryService;

    @MockitoBean private CharacterService characterService;

    /** GET /equipment 요청 시 장비 팝업 fragment가 반환되는지 검증한다. */
    @Test
    void should_returnEquipmentPopupFragment_when_equipmentRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final EquipmentView equipmentView = dummyEquipmentView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildEquipmentView(progress.getId())).thenReturn(equipmentView);

        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_EQUIPMENT_CONTENT))
                .andExpect(model().attributeExists("equipment"));
    }

    /** POST /equipment/equip 요청 시 스마트 장착을 실행하고 갱신된 장비 fragment가 반환되는지 검증한다. */
    @Test
    void should_smartEquipAndReturnRefreshedFragment_when_equipRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final EquipmentView equipmentView = dummyEquipmentView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildEquipmentView(progress.getId())).thenReturn(equipmentView);

        mockMvc.perform(
                        post("/equipment/equip")
                                .param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_EQUIPMENT_CONTENT))
                .andExpect(model().attributeExists("equipment"));

        verify(inventoryService).smartEquip(OWNED_ITEM_ID);
    }

    /** POST /equipment/unequip 요청 시 장비를 해제하고 갱신된 장비 fragment가 반환되는지 검증한다. */
    @Test
    void should_unequipAndReturnRefreshedFragment_when_unequipRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final EquipmentView equipmentView = dummyEquipmentView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.buildEquipmentView(progress.getId())).thenReturn(equipmentView);

        mockMvc.perform(
                        post("/equipment/unequip")
                                .param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_EQUIPMENT_CONTENT))
                .andExpect(model().attributeExists("equipment"));

        verify(inventoryService).unequip(OWNED_ITEM_ID);
    }

    /** GET /equipment/equippable 요청 시 슬롯 착용 가능 후보 목록이 반환되는지 검증한다. */
    @Test
    void should_returnCandidatesFragment_when_equippableRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final List<OwnedItemView> candidates =
                List.of(
                        new OwnedItemView(
                                101L,
                                "가죽투구",
                                "투구",
                                ItemType.ARMOR,
                                1,
                                false,
                                false,
                                true,
                                20.0,
                                20,
                                List.of("투구 (방어구)")));

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(inventoryService.findEquippableForSlot(progress.getId(), "HEAD"))
                .thenReturn(candidates);

        mockMvc.perform(get("/equipment/equippable").param("slot", "HEAD"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_CANDIDATES))
                .andExpect(model().attributeExists("candidates"))
                .andExpect(model().attribute("targetSlot", "HEAD"));
    }

    private EquipmentView dummyEquipmentView() {
        final List<EquipmentSlotView> slots =
                List.of(
                        EquipmentSlotView.locked("ACC1", "악세사리 1", "💍"),
                        EquipmentSlotView.empty("HEAD", "머리", "🪖"),
                        EquipmentSlotView.locked("ACC2", "악세사리 2", "💍"),
                        EquipmentSlotView.empty("MAIN_HAND", "주무기", "🗡️"),
                        EquipmentSlotView.empty("BODY", "갑옷", "🥋"),
                        EquipmentSlotView.empty("OFF_HAND", "보조손", "🛡️"),
                        EquipmentSlotView.empty("HANDS", "손", "🧤"),
                        EquipmentSlotView.empty("FEET", "발", "👢"),
                        EquipmentSlotView.locked("ROBE", "로브", "🧥"));
        final EquippedBonusResult bonus =
                new EquippedBonusResult(Stats.ZERO, new VitalMax(0, 0, 0));
        return new EquipmentView(slots, bonus, 0, 100, "맨손");
    }
}
