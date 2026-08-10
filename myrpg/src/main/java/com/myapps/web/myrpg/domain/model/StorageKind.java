package com.myapps.web.myrpg.domain.model;

/**
 * 아이템 보관 위치를 정의하는 열거형.
 *
 * <p>소지품(인벤토리)과 은행에 분류한다.
 */
public enum StorageKind {

    /** 캐릭터 소지품 (인벤토리). */
    INVENTORY,

    /** 은행 보관함. */
    BANK
}
