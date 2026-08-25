package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 장비 팝업 3x3 슬롯 조회·장비 착용/해제를 처리하는 컨트롤러.
 *
 * <p>모든 응답은 Thymeleaf fragment로 반환되며, 클라이언트(myrpg.js)가 DOM 교체로 소비한다.
 */
@Controller
@RequestMapping("/equipment")
public class EquipmentController {

    private static final String FRAGMENT_EQUIPMENT_CONTENT =
            "fragments/equipment-popup :: equipment-content";
    private static final String FRAGMENT_CANDIDATES =
            "fragments/equipment-popup :: equippable-candidates";
    private static final String LOG_TYPE_ITEM = "item";

    private final InventoryService inventoryService;
    private final CharacterService characterService;
    private final ActionLog actionLog;

    /**
     * EquipmentController를 생성한다.
     *
     * @param inventoryService 인벤토리 및 장비 서비스
     * @param characterService 캐릭터 서비스
     * @param actionLog 세션 행동 로그
     */
    public EquipmentController(
            final InventoryService inventoryService,
            final CharacterService characterService,
            final ActionLog actionLog) {
        this.inventoryService = inventoryService;
        this.characterService = characterService;
        this.actionLog = actionLog;
    }

    /**
     * 장비 팝업 컨텐츠 fragment를 반환한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 장비 팝업 fragment 뷰 이름
     */
    @GetMapping
    public String equipment(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final EquipmentView view =
                inventoryService.buildEquipmentView(progress != null ? progress.getId() : null);
        model.addAttribute("equipment", view);
        return FRAGMENT_EQUIPMENT_CONTENT;
    }

    public String equipment(final Model model) {
        return equipment(null, model);
    }

    /**
     * 장비를 착용(스마트 스왑)하고 갱신된 장비 팝업 fragment를 반환한다.
     *
     * @param ownedItemId 착용할 장비의 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 장비 팝업 fragment 뷰 이름
     */
    @PostMapping("/equip")
    public String equip(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.smartEquip(ownedItemId);
        actionLog.add("장비를 착용했습니다", LOG_TYPE_ITEM);

        final CharacterProgress progress = resolveCurrentCharacter(session);
        final EquipmentView view =
                inventoryService.buildEquipmentView(progress != null ? progress.getId() : null);
        model.addAttribute("equipment", view);
        return FRAGMENT_EQUIPMENT_CONTENT;
    }

    /**
     * 장비를 해제하고 갱신된 장비 팝업 fragment를 반환한다.
     *
     * @param ownedItemId 해제할 장비의 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 장비 팝업 fragment 뷰 이름
     */
    @PostMapping("/unequip")
    public String unequip(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.unequip(ownedItemId);
        actionLog.add("장비를 해제했습니다", LOG_TYPE_ITEM);

        final CharacterProgress progress = resolveCurrentCharacter(session);
        final EquipmentView view =
                inventoryService.buildEquipmentView(progress != null ? progress.getId() : null);
        model.addAttribute("equipment", view);
        return FRAGMENT_EQUIPMENT_CONTENT;
    }

    /**
     * 특정 슬롯에 착용 가능한 인벤토리 아이템 후보 목록을 반환한다.
     *
     * @param slot 슬롯 코드 (예: "HEAD", "BODY", "MAIN_HAND", "OFF_HAND", "HANDS", "FEET")
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 착용 가능 후보 목록 fragment 뷰 이름
     */
    @GetMapping("/equippable")
    public String equippable(
            @RequestParam final String slot, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final List<OwnedItemView> candidates =
                inventoryService.findEquippableForSlot(
                        progress != null ? progress.getId() : null, slot);
        model.addAttribute("candidates", candidates);
        model.addAttribute("targetSlot", slot);
        return FRAGMENT_CANDIDATES;
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
