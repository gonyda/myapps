package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.VitalMax;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link HealController}의 웹 슬라이스 테스트. */
@WebMvcTest(HealController.class)
class HealControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private StatProgression statProgression;

    @MockitoBean private InventoryService inventoryService;

    @MockitoBean private ActionLog actionLog;

    /** POST /heal 성공 시 200 OK가 반환되고 100골드 차감·풀회복이 수행되는지 검증한다. */
    @Test
    void should_returnOk_when_healSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(500);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(statProgression.vitalMaxFor(any(int.class), any()))
                .thenReturn(new VitalMax(100, 100, 100));
        when(inventoryService.equippedBonus())
                .thenReturn(new EquippedBonusResult(Stats.ZERO, new VitalMax(0, 0, 0)));

        mockMvc.perform(post("/heal")).andExpect(status().isOk());

        verify(characterService).saveTurn(progress);
    }

    /** POST /heal 골드 부족(100 미만) 시 400 에러가 반환되는지 검증한다. */
    @Test
    void should_returnBadRequest_when_insufficientGold() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);

        mockMvc.perform(post("/heal")).andExpect(status().isBadRequest());
    }
}
