package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 은행 팝업의 전체 뷰 모델.
 *
 * <p>은행 보관 골드·보유 골드와 은행(BANK)·소지품(INVENTORY) 두 아이템 목록을 담으며, 팝업은 좌(은행)/우(소지품) 목록 + 골드 2칸 + 입금/출금으로
 * 구성된다.
 *
 * @param bankGold 은행 보관 골드
 * @param playerGold 캐릭터 보유 골드
 * @param bankItems 은행 보관 아이템 목록 (획득순)
 * @param inventoryItems 소지품 아이템 목록 (획득순)
 */
public record BankView(
        long bankGold,
        long playerGold,
        List<OwnedItemView> bankItems,
        List<OwnedItemView> inventoryItems) {}
