package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.FieldSkillResult;
import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 스킬 목록 팝업·승급 모달 엔드포인트를 제공하는 컨트롤러.
 *
 * <p>모든 응답은 Thymeleaf fragment 스왑 형태로 반환되며, 클라이언트(myrpg.js)가 htmx 또는 직접 DOM 교체로 소비한다.
 *
 * <p>엔드포인트 개요:
 *
 * <ul>
 *   <li>{@code GET /skills} — 스킬 목록 팝업(전체 또는 탭 필터)
 *   <li>{@code GET /skills/{id}/rankup-modal} — 승급 모달
 *   <li>{@code POST /skills/{id}/rankup} — 랭크업 실행 → 갱신된 모달
 * </ul>
 */
@Controller
@RequestMapping("/skills")
public class SkillController {

    private static final String FRAGMENT_SKILL_LIST = "fragments/skill-popup :: skill-list";
    private static final String FRAGMENT_RANKUP_MODAL = "fragments/skill-popup :: rankup-modal";

    private final SkillService skillService;
    private final CharacterService characterService;

    /**
     * SkillController를 생성한다.
     *
     * @param skillService 스킬 시스템 서비스
     * @param characterService 캐릭터 진행상황 서비스
     */
    public SkillController(
            final SkillService skillService, final CharacterService characterService) {
        this.skillService = skillService;
        this.characterService = characterService;
    }

    /**
     * 스킬 목록 팝업 fragment를 반환한다.
     *
     * <p>탭 파라미터가 지정되면 해당 재능 분류의 스킬만 표시하고, 미지정이면 전체 스킬을 표시한다.
     *
     * @param tab 탭 필터 ("melee"/"archery"/"magic"/"common", 미지정 시 전체)
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 스킬 목록 fragment 뷰 이름
     */
    @GetMapping
    public String list(
            @RequestParam(name = "tab", required = false) final String tab,
            final HttpSession session,
            final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final SkillListView listView = skillService.buildListView(progress.getId(), tab);
        model.addAttribute("skillList", listView);
        return FRAGMENT_SKILL_LIST;
    }

    public String list(final String tab, final Model model) {
        return list(tab, null, model);
    }

    /**
     * 스킬 승급 모달 fragment를 반환한다.
     *
     * <p>현재 랭크, 다음 랭크 수치, 사용/막타 진행상황, AP 비용 등을 포함한다.
     *
     * @param id 스킬 카탈로그 ID
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 승급 모달 fragment 뷰 이름
     */
    @GetMapping("/{id}/rankup-modal")
    public String rankUpModal(
            @PathVariable final String id, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final SkillRankUpView rankUpView = skillService.buildRankUpView(progress.getId(), id);
        model.addAttribute("rankUp", rankUpView);
        return FRAGMENT_RANKUP_MODAL;
    }

    public String rankUpModal(final String id, final Model model) {
        return rankUpModal(id, null, model);
    }

    /**
     * 스킬 랭크업을 실행하고 갱신된 승급 모달 fragment를 반환한다.
     *
     * <p>승급 성공 시 캐릭터 진행상황을 저장하고 새 랭크 기준의 모달을 반환한다. AP 부족 시 {@code
     * InsufficientAbilityPointsException}이 발생하여 {@code GlobalExceptionHandler}에서 처리된다. 조건
     * 미충족/MASTER 시에는 상태 불변으로 현재 모달을 다시 반환한다.
     *
     * @param id 승급 대상 스킬 ID
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 승급 모달 fragment 뷰 이름
     */
    @PostMapping("/{id}/rankup")
    public String rankUp(
            @PathVariable final String id, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final boolean success = skillService.rankUp(progress, id);
        if (success) {
            characterService.saveTurn(progress);
        }
        final SkillRankUpView rankUpView = skillService.buildRankUpView(progress.getId(), id);
        model.addAttribute("rankUp", rankUpView);
        return FRAGMENT_RANKUP_MODAL;
    }

    public String rankUp(final String id, final Model model) {
        return rankUp(id, null, model);
    }

    /**
     * 필드 스킬(힐링 등)을 사용한다.
     *
     * <p>성공/실패 결과 DTO({@link FieldSkillResult})를 JSON 형태로 반환한다. 클라이언트(myrpg.js)가 상단바 및 스킬 목록을 실시간
     * 갱신한다.
     *
     * @param id 사용할 스킬 카탈로그 ID
     * @param session HTTP 세션
     * @return 필드 스킬 사용 결과 JSON
     */
    @PostMapping("/{id}/use")
    @ResponseBody
    public ResponseEntity<FieldSkillResult> useSkill(
            @PathVariable final String id, final HttpSession session) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final FieldSkillResult result = skillService.useFieldSkill(progress.getId(), id);
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<FieldSkillResult> useSkill(final String id) {
        return useSkill(id, null);
    }

    private CharacterProgress resolveCurrentCharacter(final HttpSession session) {
        if (session != null) {
            final Object sessionUser = session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
            if (sessionUser instanceof UserSession userSession
                    && userSession.characterId() != null) {
                return characterService.loadByCharacterId(userSession.characterId());
            }
        }
        return characterService.loadOrCreateDefault();
    }
}
