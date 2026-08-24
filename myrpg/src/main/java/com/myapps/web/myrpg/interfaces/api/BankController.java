package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.BankService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.Bank;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 은행 팝업·골드 입출금·아이템 맡기기/찾기를 처리하는 컨트롤러.
 *
 * <p>모든 응답은 Thymeleaf fragment 스왑 형태로 반환되며, 클라이언트(myrpg.js)가 DOM 교체로 소비한다.
 *
 * <p>엔드포인트 개요:
 *
 * <ul>
 *   <li>{@code GET /bank} — 은행 팝업 fragment (좌 은행 / 우 소지품 목록 + 골드 + 입출금)
 *   <li>{@code POST /bank/deposit} — 골드 입금 후 갱신된 은행 fragment
 *   <li>{@code POST /bank/withdraw} — 골드 출금 후 갱신된 은행 fragment
 *   <li>{@code POST /bank/item/deposit} — 아이템 맡기기 후 갱신된 은행 fragment
 *   <li>{@code POST /bank/item/withdraw} — 아이템 찾기 후 갱신된 은행 fragment
 * </ul>
 */
@Controller
@RequestMapping("/bank")
public class BankController {

    private static final String FRAGMENT_BANK_POPUP = "fragments/bank-popup :: bank-content";

    private final BankService bankService;
    private final InventoryService inventoryService;
    private final CharacterService characterService;

    /**
     * BankController를 생성한다.
     *
     * @param bankService 은행 금고 관리 서비스
     * @param inventoryService 인벤토리 서비스 (뷰 조립·아이템 이동)
     * @param characterService 캐릭터 진행상황 서비스
     */
    public BankController(
            final BankService bankService,
            final InventoryService inventoryService,
            final CharacterService characterService) {
        this.bankService = bankService;
        this.inventoryService = inventoryService;
        this.characterService = characterService;
    }

    /**
     * 은행 팝업 fragment를 반환한다.
     *
     * <p>은행 보관 골드·보유 골드와 은행/소지품 두 아이템 목록을 조립하여 좌(은행)/우(소지품) 리스트 + 골드 2칸 + 입출금 구성의 팝업을 렌더한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 은행 팝업 fragment 뷰 이름
     */
    @GetMapping
    public String bank(final HttpSession session, final Model model) {
        final BankView view = buildBankView(session);
        model.addAttribute("bank", view);
        return FRAGMENT_BANK_POPUP;
    }

    public String bank(final Model model) {
        return bank(null, model);
    }

    /**
     * 골드 입금을 수행하고 갱신된 은행 팝업 fragment를 반환한다.
     *
     * <p>단일 트랜잭션 내에서 소지금을 차감한 뒤 은행 골드를 증가시킨다. 소지금 부족 시 {@code InsufficientGoldException}이 발생하여
     * {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param amount 입금할 금액 (최소 1, 상한 없음, 수수료 없음)
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 은행 팝업 fragment 뷰 이름
     */
    @PostMapping("/deposit")
    public String deposit(
            @RequestParam final long amount, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        bankService.deposit(progress, amount);
        characterService.saveTurn(progress);

        final BankView view = buildBankView(session);
        model.addAttribute("bank", view);
        return FRAGMENT_BANK_POPUP;
    }

    public String deposit(final long amount, final Model model) {
        return deposit(amount, null, model);
    }

    /**
     * 골드 출금을 수행하고 갱신된 은행 팝업 fragment를 반환한다.
     *
     * <p>단일 트랜잭션 내에서 은행 골드를 차감한 뒤 소지금을 증가시킨다. 은행 잔액 부족 시 {@code InsufficientGoldException}이 발생하여
     * {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param amount 출금할 금액 (최소 1, 상한 없음, 수수료 없음)
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 은행 팝업 fragment 뷰 이름
     */
    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam final long amount, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        bankService.withdraw(progress, amount);
        characterService.saveTurn(progress);

        final BankView view = buildBankView(session);
        model.addAttribute("bank", view);
        return FRAGMENT_BANK_POPUP;
    }

    public String withdraw(final long amount, final Model model) {
        return withdraw(amount, null, model);
    }

    /**
     * 아이템을 인벤토리에서 은행으로 맡기고 갱신된 은행 팝업 fragment를 반환한다.
     *
     * <p>장착 중 장비는 {@code EquipConflictException}으로, 은행 용량 초과는 {@code InventoryFullException}으로 거부되어
     * {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param ownedItemId 맡길 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 은행 팝업 fragment 뷰 이름
     */
    @PostMapping("/item/deposit")
    public String depositItem(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.moveToBank(ownedItemId);

        final BankView view = buildBankView(session);
        model.addAttribute("bank", view);
        return FRAGMENT_BANK_POPUP;
    }

    public String depositItem(final long ownedItemId, final Model model) {
        return depositItem(ownedItemId, null, model);
    }

    /**
     * 아이템을 은행에서 인벤토리로 찾고 갱신된 은행 팝업 fragment를 반환한다.
     *
     * <p>인벤토리 용량 초과 시 {@code InventoryFullException}으로 거부되어 {@code GlobalExceptionHandler}에서 처리된다.
     *
     * @param ownedItemId 찾을 보유 아이템 PK
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 은행 팝업 fragment 뷰 이름
     */
    @PostMapping("/item/withdraw")
    public String withdrawItem(
            @RequestParam final long ownedItemId, final HttpSession session, final Model model) {
        inventoryService.moveToInventory(ownedItemId);

        final BankView view = buildBankView(session);
        model.addAttribute("bank", view);
        return FRAGMENT_BANK_POPUP;
    }

    public String withdrawItem(final long ownedItemId, final Model model) {
        return withdrawItem(ownedItemId, null, model);
    }

    /**
     * 최신 은행 뷰를 조립한다.
     *
     * @param session HTTP 세션
     * @return 은행 팝업 뷰 모델
     */
    private BankView buildBankView(final HttpSession session) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final Bank bank = bankService.loadOrCreateDefault();
        if (progress != null && progress.getId() != null) {
            final BankView view =
                    inventoryService.buildBankView(
                            progress.getId(), progress.getGold(), bank.getGold());
            if (view != null) {
                return view;
            }
        }
        return inventoryService.buildBankView(
                progress != null ? progress.getGold() : 0, bank != null ? bank.getGold() : 0);
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
