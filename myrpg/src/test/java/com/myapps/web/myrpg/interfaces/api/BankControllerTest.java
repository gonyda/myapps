package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.application.service.BankService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.Bank;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ItemType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link BankController}의 웹 슬라이스 테스트.
 *
 * <p>은행 팝업 렌더, 골드 입출금 성공·부족 안내,
 * 아이템 맡기기(장착 거부·용량 초과)·찾기를 검증한다.
 */
@WebMvcTest(BankController.class)
class BankControllerTest {

    private static final String FRAGMENT_BANK_POPUP = "fragments/bank-popup :: bank-content";
    private static final long OWNED_ITEM_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankService bankService;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private CharacterService characterService;

    /**
     * GET /bank 요청 시 은행 팝업 fragment가 200으로 반환되는지 검증한다.
     */
    @Test
    void should_returnBankPopupFragment_when_bankRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Bank bank = Bank.createDefault();
        final BankView bankView = dummyBankView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(bankService.loadOrCreateDefault()).thenReturn(bank);
        when(inventoryService.buildBankView(progress.getGold(), bank.getGold())).thenReturn(bankView);

        mockMvc.perform(get("/bank"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BANK_POPUP))
                .andExpect(model().attributeExists("bank"));
    }

    /**
     * POST /bank/deposit?amount=100 성공 시 갱신된 은행 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_depositSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(500);
        final Bank bank = Bank.createDefault();
        final BankView bankView = dummyBankView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(bankService.loadOrCreateDefault()).thenReturn(bank);
        when(inventoryService.buildBankView(progress.getGold(), bank.getGold())).thenReturn(bankView);

        mockMvc.perform(post("/bank/deposit").param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BANK_POPUP))
                .andExpect(model().attributeExists("bank"));

        verify(bankService).deposit(progress, 100L);
    }

    /**
     * POST /bank/deposit?amount=1000 소지금 부족 시 InsufficientGoldException → 400 에러 뷰를 검증한다.
     */
    @Test
    void should_returnErrorView_when_depositExceedsPlayerGold() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        doThrow(new InsufficientGoldException("소지금 부족: 입금 요청 1000, 보유 0"))
                .when(bankService).deposit(any(CharacterProgress.class), eq(1000L));

        mockMvc.perform(post("/bank/deposit").param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /**
     * POST /bank/withdraw?amount=50 성공 시 갱신된 은행 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_withdrawSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Bank bank = Bank.createDefault();
        final BankView bankView = dummyBankView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(bankService.loadOrCreateDefault()).thenReturn(bank);
        when(inventoryService.buildBankView(progress.getGold(), bank.getGold())).thenReturn(bankView);

        mockMvc.perform(post("/bank/withdraw").param("amount", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BANK_POPUP))
                .andExpect(model().attributeExists("bank"));

        verify(bankService).withdraw(progress, 50L);
    }

    /**
     * POST /bank/withdraw?amount=1000 은행 잔액 부족 시 InsufficientGoldException → 400 에러 뷰를 검증한다.
     */
    @Test
    void should_returnErrorView_when_withdrawExceedsBankGold() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        doThrow(new InsufficientGoldException("은행 잔액 부족: 출금 요청 1000, 보관 0"))
                .when(bankService).withdraw(any(CharacterProgress.class), eq(1000L));

        mockMvc.perform(post("/bank/withdraw").param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /**
     * POST /bank/item/deposit?ownedItemId=1 성공 시 갱신된 은행 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_itemDepositSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Bank bank = Bank.createDefault();
        final BankView bankView = dummyBankView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(bankService.loadOrCreateDefault()).thenReturn(bank);
        when(inventoryService.buildBankView(progress.getGold(), bank.getGold())).thenReturn(bankView);

        mockMvc.perform(post("/bank/item/deposit").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BANK_POPUP))
                .andExpect(model().attributeExists("bank"));

        verify(inventoryService).moveToBank(OWNED_ITEM_ID);
    }

    /**
     * POST /bank/item/deposit 장착 중 장비 맡기기 시 EquipConflictException → 400 에러 뷰를 검증한다.
     */
    @Test
    void should_returnErrorView_when_itemDepositEquipped() throws Exception {
        doThrow(new EquipConflictException("장착을 해제한 후 맡길 수 있습니다."))
                .when(inventoryService).moveToBank(OWNED_ITEM_ID);

        mockMvc.perform(post("/bank/item/deposit").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /**
     * POST /bank/item/deposit 은행 용량 초과 시 InventoryFullException → 400 에러 뷰를 검증한다.
     */
    @Test
    void should_returnErrorView_when_itemDepositBankFull() throws Exception {
        doThrow(new InventoryFullException("은행가 가득 찼습니다."))
                .when(inventoryService).moveToBank(OWNED_ITEM_ID);

        mockMvc.perform(post("/bank/item/deposit").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /**
     * POST /bank/item/withdraw?ownedItemId=1 성공 시 갱신된 은행 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_itemWithdrawSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Bank bank = Bank.createDefault();
        final BankView bankView = dummyBankView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(bankService.loadOrCreateDefault()).thenReturn(bank);
        when(inventoryService.buildBankView(progress.getGold(), bank.getGold())).thenReturn(bankView);

        mockMvc.perform(post("/bank/item/withdraw").param("ownedItemId", String.valueOf(OWNED_ITEM_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BANK_POPUP))
                .andExpect(model().attributeExists("bank"));

        verify(inventoryService).moveToInventory(OWNED_ITEM_ID);
    }

    private BankView dummyBankView() {
        final OwnedItemView potionView = new OwnedItemView(
                1L, "생명력 50 포션", "포션", ItemType.POTION,
                5, false, true, false,
                null, null,
                List.of("생명력을 50 회복한다."));

        final OwnedItemView weaponView = new OwnedItemView(
                2L, "초보자 한손검", "무기", ItemType.WEAPON,
                1, true, false, true,
                20.0, 20,
                List.of("한손검 (무기)", "힘 +5", "내구도: 20/20"));

        return new BankView(0L, 0L, List.of(), List.of(potionView, weaponView));
    }
}
