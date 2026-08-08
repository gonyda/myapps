package com.myapps.web.myrpg.domain.model;

/**
 * 재능 보너스가 적용되는 대상의 분류를 정의하는 열거형.
 *
 * <p>스탯 계열({@link #STAT})과 바이탈 계열({@link #VITAL})을 구분하며,
 * 재능 보너스를 스탯 또는 바이탈 최대치 중 어디에 가산할지 분기하는 단일 소스 역할을 한다.
 */
public enum BonusKind {

    /** 스탯 계열 (STR/DEX/INT/Critical). */
    STAT,

    /** 바이탈 계열 (HP/MP/Stamina). */
    VITAL
}
