package com.myapps.web.myrpg.interfaces.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myapps.web.myrpg.application.dto.ShopView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;

/**
 * 상점 팝업·구매·판매를 처리하는 컨트롤러.
 *
 * <p>모든 응답은 Thymeleaf fragment 스왑 형태로 반환되며,
 * 클라이언트(myrpg.js)가 DOM 교체로 소비한다.
 *
 * <p>엔드포인트 개요:
 * <ul>
 *   <li>{@code GET /shop} — 상점 팝업 fragment (상점물건 위 / 소지품 아래 / 골드 하단)</li>
 *   <li>{@code POST /shop/buy} — 아이템 1개 구매 후 갱신된 상점 fragment</li>
 *   <li>{@code POST /shop/sell} — 아이템 1개 판매 후 갱신된 상점 fragment</li>
 * </ul>
 */
@Controller
@RequestMapping("/shop")
public class ShopController {

    private static final String FRAGMENT_SHOP_POPUP = "fragments/shop-popup :: shop-content";

    private final ShopService shopService;
    private final CharacterService characterService;

    /**
     * ShopController를 생성한다.
     *
     * @param shopService       상점 구매/판매/뷰 조립 서비스
     * @param characterService  캐릭터 진행상황 서비스
     */
    public ShopController(final ShopService shopService,
                          final CharacterService characterService) {
        this.shopService = shopService;
        this.characterService = characterService;
    }

    /**
     * 상점 팝업 fragment를 반환한다.
     *
     * <p>대화 중인 NPC의 판매 목록과 인벤토리 판매 대상 목록을 조립하여
     * 상점물건 위 / 소지품 아래 / 골드 하단 구성의 팝업을 렌더한다.
     *
     * @param npcId 대화 중인 NPC ID (없으면 빈 구매 목록)
     * @param model Spring MVC 모델
     * @return 상점 팝업 fragment 뷰 이름
     */
    @GetMapping
    public String shop(@RequestParam(required = false) final String npcId, final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final ShopView view = shopService.buildShopView(npcId, progress.getGold());
        model.addAttribute("shop", view);
        return FRAGMENT_SHOP_POPUP;
    }

    /**
     * 아이템 1개를 구매하고 갱신된 상점 팝업 fragment를 반환한다.
     *
     * <p>NPC 판매 목록 포함 및 buyPrice 존재를 검증한 뒤 골드를 차감하고
     * 인벤토리에 아이템을 추가한다. 골드 부족 시 {@code InsufficientGoldException},
     * 인벤토리 초과 시 {@code InventoryFullException}이 발생하여
     * {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param npcId  대화 중인 NPC ID
     * @param itemId 구매할 아이템 카탈로그 ID
     * @param model  Spring MVC 모델
     * @return 상점 팝업 fragment 뷰 이름
     */
    @PostMapping("/buy")
    public String buy(@RequestParam final String npcId,
                      @RequestParam final String itemId,
                      final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        shopService.buy(progress, npcId, itemId);
        characterService.saveTurn(progress);

        final ShopView view = shopService.buildShopView(npcId, progress.getGold());
        model.addAttribute("shop", view);
        return FRAGMENT_SHOP_POPUP;
    }

    /**
     * 인벤토리 아이템을 1개 판매하고 갱신된 상점 팝업 fragment를 반환한다.
     *
     * <p>장착 중인 장비는 {@code EquipConflictException}으로 거부되어
     * {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param npcId       대화 중인 NPC ID (없으면 빈 구매 목록)
     * @param ownedItemId 판매할 보유 아이템 PK
     * @param model       Spring MVC 모델
     * @return 상점 팝업 fragment 뷰 이름
     */
    @PostMapping("/sell")
    public String sell(@RequestParam(required = false) final String npcId,
                       @RequestParam final long ownedItemId,
                       final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        shopService.sell(progress, ownedItemId);
        characterService.saveTurn(progress);

        final ShopView view = shopService.buildShopView(npcId, progress.getGold());
        model.addAttribute("shop", view);
        return FRAGMENT_SHOP_POPUP;
    }
}