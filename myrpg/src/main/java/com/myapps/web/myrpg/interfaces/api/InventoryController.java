package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 인벤토리 팝업 목록·포션 사용·장비 착용/해제를 처리하는 컨트롤러.
 *
 * <p>모든 응답은 Thymeleaf fragment로 반환되며, 클라이언트(myrpg.js)가 DOM 교체로 소비한다.
 *
 * <p>엔드포인트 개요:
 *
 * <ul>
 *   <li>{@code GET /inventory} — 인벤토리 목록 팝업 (기본 획득순)
 *   <li>{@code POST /inventory/use} — 포션 사용 후 갱신된 인벤토리 fragment
 *   <li>{@code POST /inventory/equip} — 장비 착용 후 갱신된 인벤토리 fragment
 *   <li>{@code POST /inventory/unequip} — 장비 해제 후 갱신된 인벤토리 fragment
 * </ul>
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private static final String FRAGMENT_INVENTORY_CONTENT =
            "fragments/inventory-popup :: inventory-content";
    private static final String LOG_TYPE_ITEM = "item";

    private final InventoryService inventoryService;
    private final CharacterService characterService;
    private final ActionLog actionLog;
    private final com.myapps.web.myrpg.support.GameMessageService gameMessageService;

    /**
     * InventoryController를 생성한다.
     *
     * @param inventoryService 인벤토리 서비스
     * @param characterService 캐릭터 진행상황 서비스
     * @param actionLog 세션 보관 행동 로그
     * @param gameMessageService 인게임 메시지 리졸버 서비스
     */
    @org.springframework.beans.factory.annotation.Autowired
    public InventoryController(
            final InventoryService inventoryService,
            final CharacterService characterService,
            final ActionLog actionLog,
            @org.springframework.lang.Nullable
                    final com.myapps.web.myrpg.support.GameMessageService gameMessageService) {
        this.inventoryService = inventoryService;
        this.characterService = characterService;
        this.actionLog = actionLog;
        this.gameMessageService =
                gameMessageService != null
                        ? gameMessageService
                        : new com.myapps.web.myrpg.support.GameMessageService(null);
    }

    /** 이전 호환용 생성자. */
    public InventoryController(
            final InventoryService inventoryService,
            final CharacterService characterService,
            final ActionLog actionLog) {
        this(inventoryService, characterService, actionLog, null);
    }

    /**
     * 인벤토리 팝업 fragment를 반환한다.
     *
     * <p>보유 골드와 인벤토리 아이템 목록을 획득순(id 오름차순)으로 조립하여 반환한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 인벤토리 팝업 fragment 뷰 이름
     */
    @GetMapping
    public String inventory(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final InventoryView view = resolveInventoryView(progress);
        model.addAttribute("inventory", view);
        return FRAGMENT_INVENTORY_CONTENT;
    }

    public String inventory(final Model model) {
        return inventory(null, model);
    }

    /**
     * 포션을 사용하고 갱신된 인벤토리 팝업 fragment를 반환한다.
     *
     * <p>포션 사용 후 HP를 회복하고 수량을 1 감소시킨다. 수량이 0이 되면 해당 행을 제거한다.
     *
     * @param ownedItemId 사용할 포션의 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 인벤토리 팝업 fragment 뷰 이름
     */
    @PostMapping("/use")
    public String usePotion(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        final PotionItem potion = inventoryService.usePotion(ownedItemId);
        final String potionMsg = gameMessageService.get("log.potion.use", potion.name());
        actionLog.add(potionMsg, LOG_TYPE_ITEM);

        final CharacterProgress progress = resolveCurrentCharacter(session);
        final InventoryView view = resolveInventoryView(progress);
        model.addAttribute("inventory", view);
        return FRAGMENT_INVENTORY_CONTENT;
    }

    public String usePotion(final long ownedItemId, final Model model) {
        return usePotion(ownedItemId, null, model);
    }

    /**
     * 장비를 착용하고 갱신된 인벤토리 팝업 fragment를 반환한다.
     *
     * <p>착용 규칙(슬롯 점유 기반 충돌 검사)을 적용한다. 같은 역할 장비가 있으면 스왑하고, 슬롯 충돌 시 {@link
     * com.myapps.web.myrpg.application.exception.EquipConflictException}이 발생한다.
     *
     * @param ownedItemId 착용할 장비의 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 인벤토리 팝업 fragment 뷰 이름
     */
    @PostMapping("/equip")
    public String equip(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.equip(ownedItemId);

        final CharacterProgress progress = resolveCurrentCharacter(session);
        final InventoryView view = resolveInventoryView(progress);
        model.addAttribute("inventory", view);
        return FRAGMENT_INVENTORY_CONTENT;
    }

    public String equip(final long ownedItemId, final Model model) {
        return equip(ownedItemId, null, model);
    }

    /**
     * 장비를 해제하고 갱신된 인벤토리 팝업 fragment를 반환한다.
     *
     * @param ownedItemId 해제할 장비의 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 인벤토리 팝업 fragment 뷰 이름
     */
    @PostMapping("/unequip")
    public String unequip(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.unequip(ownedItemId);

        final CharacterProgress progress = resolveCurrentCharacter(session);
        final InventoryView view = resolveInventoryView(progress);
        model.addAttribute("inventory", view);
        return FRAGMENT_INVENTORY_CONTENT;
    }

    public String unequip(final long ownedItemId, final Model model) {
        return unequip(ownedItemId, null, model);
    }

    private InventoryView resolveInventoryView(final CharacterProgress progress) {
        if (progress != null && progress.getId() != null) {
            final InventoryView view =
                    inventoryService.buildInventoryView(progress.getId(), progress.getGold());
            if (view != null) {
                return view;
            }
        }
        return inventoryService.buildInventoryView(progress != null ? progress.getGold() : 0);
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
