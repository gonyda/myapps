package com.myapps.web.myrpg.application.dto;

/**
 * 스킬 승급 모달의 뷰 모델.
 *
 * <p>현재 랭크와 다음 랭크의 수치, 사용/막타 진행상황, AP 비용 및 보유량,
 * 승급 가능 여부, 최고 랭크 여부를 포함한다.
 *
 * @param id                   스킬 카탈로그 ID
 * @param label                스킬 표시명
 * @param currentRankLabel     현재 랭크 라벨
 * @param nextRankLabel        다음 랭크 라벨 (MASTER일 경우 null)
 * @param primaryStatLabel     주요 스탯 라벨 (예: "보너스 데미지", "피해 경감")
 * @param currentValue         현재 랭크 주요 수치 (딜: 배율% / 디펜스: 경감%)
 * @param nextValue            다음 랭크 주요 수치 (MASTER면 0)
 * @param currentCounterValue  현재 랭크 반격 배율% (딜스킬은 null)
 * @param nextCounterValue     다음 랭크 반격 배율% (딜스킬은 null)
 * @param resourceKindLabel    자원 종류 라벨 ("스태미나" 또는 "MP")
 * @param resourceCost         자원 소모량
 * @param usageCurrent         현재 사용 횟수
 * @param usageRequired        승급 요구 사용 횟수 (MASTER면 0)
 * @param killCurrent          현재 막타 처치 수
 * @param killRequired         승급 요구 막타 처치 수 (MASTER면 0)
 * @param apCost               승급 AP 비용 (MASTER면 0)
 * @param apOwned              현재 보유 AP
 * @param rankable             승급 가능 여부
 * @param maxed                최고 랭크(MASTER) 여부
 */
public record SkillRankUpView(
        String id,
        String label,
        String currentRankLabel,
        String nextRankLabel,
        String primaryStatLabel,
        int currentValue,
        int nextValue,
        Integer currentCounterValue,
        Integer nextCounterValue,
        String resourceKindLabel,
        int resourceCost,
        int usageCurrent,
        int usageRequired,
        int killCurrent,
        int killRequired,
        int apCost,
        int apOwned,
        boolean rankable,
        boolean maxed
) {
}
