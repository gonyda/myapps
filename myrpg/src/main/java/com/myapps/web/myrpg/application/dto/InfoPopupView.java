package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 정보 팝업(상/중/하 3구역) 전체 뷰 모델 레코드.
 *
 * <p>상단: 닉네임, 현재 레벨, 누적 레벨, 재능 라벨, 보유 AP, 재능 효과 요약, HP·MP·Stamina 게이지.
 * 중앙: 스탯 목록({@link StatLine}).
 * 하단: 환생 가능 여부, 환생 경과 텍스트.
 *
 * @param nickname             닉네임
 * @param currentLevel         현재 레벨
 * @param accumulatedLevel     누적 레벨
 * @param talentLabel          재능 한글 라벨 (예: "근접전투")
 * @param abilityPoints        보유 어빌리티 포인트
 * @param talentEffectSummary  재능 효과 요약 문자열 (예: "근접 데미지 +10%, STR +2/Lv, HP +5/Lv")
 * @param hp                   HP 게이지 뷰 모델
 * @param mp                   MP 게이지 뷰 모델
 * @param stamina              Stamina 게이지 뷰 모델
 * @param stats                스탯 라인 목록 (STR, DEX, INT, CRIT, DEF 순서)
 * @param rebirthAvailable     환생 가능 여부
 * @param rebirthElapsedText   환생 경과 텍스트 (예: "환생 후 3시간 15분 경과" 또는 "환생 기록 없음")
 */
public record InfoPopupView(
        String nickname,
        int currentLevel,
        int accumulatedLevel,
        String talentLabel,
        int abilityPoints,
        String talentEffectSummary,
        GaugeView hp,
        GaugeView mp,
        GaugeView stamina,
        List<StatLine> stats,
        boolean rebirthAvailable,
        String rebirthElapsedText
) {
}
