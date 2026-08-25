package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.application.dto.SkillRowView;
import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link SkillController}의 웹 슬라이스 테스트.
 *
 * <p>스킬 목록 팝업(탭 필터 포함), 승급 모달 조회, 랭크업 성공/실패(AP 부족) 엔드포인트를 검증한다.
 */
@WebMvcTest(SkillController.class)
class SkillControllerTest {

    private static final String FRAGMENT_SKILL_LIST = "fragments/skill-popup :: skill-list";
    private static final String FRAGMENT_RANKUP_MODAL = "fragments/skill-popup :: rankup-modal";
    private static final String SKILL_ID = "windmill";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillService skillService;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private com.myapps.web.myrpg.domain.model.ActionLog actionLog;

    /** GET /skills 요청 시 전체 스킬 목록 fragment가 반환되는지 검증한다. */
    @Test
    void should_returnSkillListFragment_when_listRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final SkillListView listView = dummyListView(null);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.buildListView(progress.getId(), null)).thenReturn(listView);

        mockMvc.perform(get("/skills"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SKILL_LIST))
                .andExpect(model().attributeExists("skillList"));
    }

    /** GET /skills?tab=melee 요청 시 근접전투 탭 필터가 적용된 목록이 반환되는지 검증한다. */
    @Test
    void should_returnFilteredList_when_tabSpecified() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final SkillListView listView = dummyListView("melee");

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.buildListView(progress.getId(), "melee")).thenReturn(listView);

        mockMvc.perform(get("/skills").param("tab", "melee"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SKILL_LIST))
                .andExpect(model().attributeExists("skillList"));
    }

    /** GET /skills/{id}/rankup-modal 요청 시 승급 모달 fragment가 반환되는지 검증한다. */
    @Test
    void should_returnRankUpModal_when_modalRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final SkillRankUpView rankUpView = dummyRankUpView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.buildRankUpView(progress.getId(), SKILL_ID)).thenReturn(rankUpView);

        mockMvc.perform(get("/skills/{id}/rankup-modal", SKILL_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_RANKUP_MODAL))
                .andExpect(model().attributeExists("rankUp"));
    }

    /** POST /skills/{id}/rankup 성공 시 갱신된 승급 모달이 반환되고 캐릭터 진행상황이 저장되는지 검증한다. */
    @Test
    void should_returnUpdatedModal_when_rankUpSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final SkillRankUpView rankUpView = dummyRankUpView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.rankUp(progress, SKILL_ID)).thenReturn(true);
        when(skillService.buildRankUpView(progress.getId(), SKILL_ID)).thenReturn(rankUpView);

        mockMvc.perform(post("/skills/{id}/rankup", SKILL_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_RANKUP_MODAL))
                .andExpect(model().attributeExists("rankUp"));

        verify(characterService).saveTurn(progress);
        verify(actionLog).add("✨ [윈드밀] F랭크로 승급되었습니다!", "growth");
    }

    /**
     * POST /skills/{id}/rankup 시 AP 부족이면 {@link InsufficientAbilityPointsException}이 {@code
     * GlobalExceptionHandler}에서 처리되어 에러 뷰가 반환되는지 검증한다.
     */
    @Test
    void should_returnErrorView_when_rankUpFailsDueToInsufficientAp() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.rankUp(progress, SKILL_ID))
                .thenThrow(new InsufficientAbilityPointsException("AP 부족: 필요 1, 보유 0"));

        mockMvc.perform(post("/skills/{id}/rankup", SKILL_ID))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /** POST /skills/{id}/use 성공 시 FieldSkillResult JSON이 반환되는지 검증한다. */
    @Test
    void should_returnSuccessResult_when_useSkillSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final com.myapps.web.myrpg.application.dto.FieldSkillResult result =
                com.myapps.web.myrpg.application.dto.FieldSkillResult.success(
                        "힐링 발동! 생명력을 20 회복했습니다.", 80, 100, 30, 50, 20);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.useFieldSkill(progress.getId(), "healing")).thenReturn(result);

        mockMvc.perform(post("/skills/{id}/use", "healing"))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                                        "$.success")
                                .value(true))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                                        "$.healedAmount")
                                .value(20))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                                        "$.hpCurrent")
                                .value(80));
    }

    /** POST /skills/{id}/use 실패(마나 부족) 시 실패 FieldSkillResult JSON이 반환되는지 검증한다. */
    @Test
    void should_returnFailureResult_when_useSkillFailsDueToLowMp() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final com.myapps.web.myrpg.application.dto.FieldSkillResult result =
                com.myapps.web.myrpg.application.dto.FieldSkillResult.failure(
                        "마나가 부족합니다.", 50, 100, 2, 50);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(skillService.useFieldSkill(progress.getId(), "healing")).thenReturn(result);

        mockMvc.perform(post("/skills/{id}/use", "healing"))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                                        "$.success")
                                .value(false))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                                        "$.message")
                                .value("마나가 부족합니다."));
    }

    private SkillListView dummyListView(final String activeTab) {
        final SkillRowView row = new SkillRowView(SKILL_ID, "윈드밀", "근접전투", "F", 0, false, false);
        return new SkillListView(activeTab, List.of(row));
    }

    private SkillRankUpView dummyRankUpView() {
        return new SkillRankUpView(
                SKILL_ID,
                "윈드밀",
                "무기를 크게 휘둘러 주변의 적을 회전 베기로 공격한다. 여러 적에게 동시에 피해를 줄 수 있어 다수와의 전투에서 유용하다.",
                "F",
                "E",
                "보너스 데미지",
                100,
                150,
                null,
                null,
                "스태미나",
                10,
                10,
                null,
                null,
                "STR +1",
                0,
                5,
                0,
                1,
                1,
                0,
                false,
                false);
    }
}
