package com.myapps.web.myrpg.application.dto;

/**
 * 정보 팝업의 스탯 한 줄을 나타내는 뷰 모델 레코드.
 *
 * <p>라벨(예: "STR"), 본체 값(예: "23"), 스킬 보너스(예: "+0")을
 * 문자열로 보관하여 템플릿에서 직접 렌더링한다.
 *
 * @param label 스탯 라벨 (예: "STR", "DEX", "INT", "CRIT", "DEF")
 * @param value 본체 값 문자열
 * @param bonus 스킬 보너스 문자열 (예: "+0", "+0.0%")
 */
public record StatLine(
        String label,
        String value,
        String bonus
) {
}
