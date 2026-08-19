package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.dto.ShopBuyItemView;
import com.myapps.web.myrpg.application.dto.ShopSellItemView;
import com.myapps.web.myrpg.application.dto.ShopView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;

/**
 * {@link ShopController}의 웹 슬라이스 테스트.
 *
 * <p>상점 팝업 렌더, 구매·판매 성공 fragment 스왑, 골드 부족 에러 뷰를 검증한다.
 */
@WebMvcTest(ShopController.class)
class ShopControllerTest {

    private static final String FRAGMENT_SHOP_POPUP = "fragments/shop-popup :: shop-content";
    private static final String NPC_ID = "ferghus";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private CharacterService characterService;

    /**
     * GET /shop?npcId=ferghus 요청 시 상점 팝업 fragment가 200으로 반환되는지 검증한다.
     */
    @Test
    void should_returnShopPopupFragment_when_shopRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final ShopView shopView = dummyShopView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(shopService.buildShopView(eq(NPC_ID), eq(progress.getGold()))).thenReturn(shopView);

        mockMvc.perform(get("/shop").param("npcId", NPC_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SHOP_POPUP))
                .andExpect(model().attributeExists("shop"));
    }

    /**
     * POST /shop/buy?npcId=ferghus&itemId=short_sword 성공 시 갱신된 상점 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_buySucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final ShopView shopView = dummyShopView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(shopService.buildShopView(eq(NPC_ID), eq(progress.getGold()))).thenReturn(shopView);

        mockMvc.perform(post("/shop/buy")
                        .param("npcId", NPC_ID)
                        .param("itemId", "short_sword"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SHOP_POPUP))
                .andExpect(model().attributeExists("shop"));

        verify(shopService).buy(progress, NPC_ID, "short_sword");
    }

    /**
     * POST /shop/buy 골드 부족 시 InsufficientGoldException → 400 에러 뷰를 검증한다.
     */
    @Test
    void should_returnErrorView_when_buyInsufficientGold() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        doThrow(new InsufficientGoldException("골드 부족: 소모 요청 300, 보유 0"))
                .when(shopService).buy(any(CharacterProgress.class), anyString(), anyString());

        mockMvc.perform(post("/shop/buy")
                        .param("npcId", NPC_ID)
                        .param("itemId", "short_sword"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    /**
     * POST /shop/sell?npcId=ferghus&ownedItemId=1 성공 시 갱신된 상점 fragment가 반환되는지 검증한다.
     */
    @Test
    void should_returnRefreshedFragment_when_sellSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final ShopView shopView = dummyShopView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(shopService.buildShopView(eq(NPC_ID), eq(progress.getGold()))).thenReturn(shopView);

        mockMvc.perform(post("/shop/sell")
                        .param("npcId", NPC_ID)
                        .param("ownedItemId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SHOP_POPUP))
                .andExpect(model().attributeExists("shop"));

        verify(shopService).sell(progress, 1L);
    }

    /**
     * POST /shop/sell npcId 없이도 정상 동작하는지 검증한다 (npcId optional).
     */
    @Test
    void should_returnRefreshedFragment_when_sellWithoutNpcId() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);
        final ShopView shopView = dummyShopView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(shopService.buildShopView(isNull(), eq(progress.getGold()))).thenReturn(shopView);

        mockMvc.perform(post("/shop/sell").param("ownedItemId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_SHOP_POPUP))
                .andExpect(model().attributeExists("shop"));

        verify(shopService).sell(progress, 1L);
    }

    private ShopView dummyShopView() {
        final ShopBuyItemView buyItem = new ShopBuyItemView(
                "short_sword", "숏소드", "무기", 300L,
                List.of("한손검 (무기)", "힘 +8", "내구도: 15/15"));
        final ShopSellItemView sellItem = new ShopSellItemView(
                1L, "초보자 한손검", "무기", 1, 50L, false,
                List.of("한손검 (무기)", "힘 +5", "내구도: 20/20"));
        return new ShopView(List.of(buyItem), List.of(sellItem), 1_000L, NPC_ID);
    }
}