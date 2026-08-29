package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myrpg.application.dto.WoodcutResult;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.GatheringService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GatheringController.class)
class GatheringControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GatheringService gatheringService;

    @MockitoBean private CharacterService characterService;

    @Test
    @DisplayName("POST /gathering/woodcut 요청 시 채집이 실행되고 WoodcutResult JSON이 200으로 반환된다")
    void should_returnWoodcutResultJson_when_woodcutRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final WoodcutResult expectedResult =
                new WoodcutResult(true, "🪵 단단한 장작을 1개 획득했습니다!", "firewood", 95, 100);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(gatheringService.gatherWood(any(CharacterProgress.class))).thenReturn(expectedResult);

        mockMvc.perform(post("/gathering/woodcut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("🪵 단단한 장작을 1개 획득했습니다!"))
                .andExpect(jsonPath("$.itemId").value("firewood"))
                .andExpect(jsonPath("$.currentStamina").value(95))
                .andExpect(jsonPath("$.maxStamina").value(100));

        verify(gatheringService).gatherWood(progress);
    }
}
