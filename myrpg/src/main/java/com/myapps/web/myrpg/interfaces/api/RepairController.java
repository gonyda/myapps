package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.RepairItemView;
import com.myapps.web.myrpg.application.dto.RepairView;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 대장간 내구도 1포인트 수리를 처리하는 컨트롤러.
 *
 * <p>GET /repair로 수리 팝업(내구도가 닳은 장비 목록 + 수리비 + 성공률 95%)을 렌더하고, POST /repair로 1포인트 수리를 실행한 뒤 갱신된 팝업
 * fragment를 반환한다.
 */
@Controller
@RequestMapping("/repair")
public class RepairController {

    private static final String FRAGMENT_REPAIR_POPUP = "fragments/repair-popup :: repair-content";
    private static final String LOG_TYPE_ITEM = "item";
    private static final int REPAIR_SUCCESS_RATE_PERCENT = 95;
    private static final int REPAIR_AMOUNT = 1;

    private final CharacterService characterService;
    private final ShopService shopService;
    private final InventoryService inventoryService;
    private final ItemCatalogService itemCatalogService;
    private final OwnedItemRepository ownedItemRepository;
    private final ActionLog actionLog;
    private final Random random;

    /**
     * RepairController를 생성한다.
     *
     * @param characterService 캐릭터 진행상황 서비스
     * @param shopService 상점 서비스 (판매가 산출 = 수리비)
     * @param inventoryService 인벤토리 서비스 (설명문 생성)
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param ownedItemRepository 보유 아이템 저장소
     * @param actionLog 세션 보관 행동 로그
     * @param random 난수 발생기 (성공률 판정용)
     */
    public RepairController(
            final CharacterService characterService,
            final ShopService shopService,
            final InventoryService inventoryService,
            final ItemCatalogService itemCatalogService,
            final OwnedItemRepository ownedItemRepository,
            final ActionLog actionLog,
            final Random random) {
        this.characterService = characterService;
        this.shopService = shopService;
        this.inventoryService = inventoryService;
        this.itemCatalogService = itemCatalogService;
        this.ownedItemRepository = ownedItemRepository;
        this.actionLog = actionLog;
        this.random = random;
    }

    /**
     * 수리 팝업 fragment를 반환한다.
     *
     * <p>인벤토리에서 내구도가 닳은 장비({@code ceil(currentDurability) < maxDurability})만 필터링하여 수리 목록을 조립한다. 장착
     * 중인 장비도 수리 대상에 포함된다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 수리 팝업 fragment 뷰 이름
     */
    @GetMapping
    public String repairPopup(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final RepairView view = buildRepairView(progress.getId(), progress.getGold());
        model.addAttribute("repair", view);
        return FRAGMENT_REPAIR_POPUP;
    }

    /**
     * 세션 없는 직접 호출을 위한 오버로드.
     *
     * @param model Spring MVC 모델
     * @return 수리 팝업 fragment 뷰 이름
     */
    public String repairPopup(final Model model) {
        return repairPopup(null, model);
    }

    /**
     * 1포인트 수리를 수행하고 갱신된 수리 팝업 fragment를 반환한다.
     *
     * <p>대상 장비의 내구도가 가득 차 있으면 수리 없이 갱신 fragment만 반환한다. 수리비는 해당 장비의 판매가와 동일하며, 시도 시 골드를 차감한다. 95%
     * 확률로 성공하여 내구도가 +1 되고, 실패 시 내구도 변화 없이 골드만 소모된다.
     *
     * @param ownedItemId 수리할 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 수리 팝업 fragment 뷰 이름
     */
    @PostMapping
    public String repair(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final OwnedItem target =
                ownedItemRepository
                        .findById(ownedItemId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "보유 아이템을 찾을 수 없습니다: " + ownedItemId));

        final Item catalogItem =
                itemCatalogService
                        .byId(target.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (catalogItem instanceof EquipmentItem equipItem
                && Math.ceil(target.getCurrentDurability()) < equipItem.maxDurability()) {
            final long repairCost = shopService.sellValueOf(target);
            progress.spendGold(repairCost);

            if (random.nextInt(100) < REPAIR_SUCCESS_RATE_PERCENT) {
                target.repairBy(REPAIR_AMOUNT, equipItem.maxDurability());
                actionLog.add("수리 성공! 내구도 +1", LOG_TYPE_ITEM);
            } else {
                actionLog.add("퍼거스가 손을 삐끗했다… 수리 실패!", LOG_TYPE_ITEM);
            }
            characterService.saveTurn(progress);
        }

        final RepairView view = buildRepairView(progress.getId(), progress.getGold());
        model.addAttribute("repair", view);
        return FRAGMENT_REPAIR_POPUP;
    }

    /**
     * 세션 없는 직접 호출을 위한 오버로드.
     *
     * @param ownedItemId 수리할 보유 아이템 PK
     * @param model Spring MVC 모델
     * @return 수리 팝업 fragment 뷰 이름
     */
    public String repair(final long ownedItemId, final Model model) {
        return repair(ownedItemId, null, model);
    }

    /**
     * 최신 수리 뷰를 조립한다.
     *
     * <p>인벤토리 장비 중 내구도가 닳은 장비만 {@link RepairItemView}로 변환한다.
     *
     * @param characterId 캐릭터 식별자
     * @param currentGold 현재 보유 골드
     * @return 수리 팝업 뷰 모델
     */
    private RepairView buildRepairView(final Long characterId, final long currentGold) {
        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final List<RepairItemView> repairItems = new ArrayList<>();
        for (final OwnedItem owned : inventoryItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(owned.getItemId());
            if (catalogOpt.isEmpty() || !(catalogOpt.get() instanceof EquipmentItem equipItem)) {
                continue;
            }
            if (Math.ceil(owned.getCurrentDurability()) >= equipItem.maxDurability()) {
                continue;
            }
            repairItems.add(
                    new RepairItemView(
                            owned.getId(),
                            equipItem.name(),
                            equipItem.type().label(),
                            (int) Math.ceil(owned.getCurrentDurability()),
                            equipItem.maxDurability(),
                            shopService.sellValueOf(owned),
                            owned.isEquipped(),
                            inventoryService.describe(equipItem, owned)));
        }
        return new RepairView(List.copyOf(repairItems), currentGold);
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
