package com.myapps.web.myrpg.domain.model;

/**
 * 장비 착용 슬롯을 정의하는 열거형.
 *
 * <p>캐릭터가 장비를 장착할 수 있는 물리적 위치를 나타낸다.
 */
public enum EquipSlot {

    /** 주무기 슬롯 (한손검·양손검). */
    MAIN_HAND,

    /** 보조손 슬롯 (방패·양손검 점유). */
    OFF_HAND,

    /** 몸통 슬롯 (갑옷). */
    BODY
}
