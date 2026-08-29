package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.SkillEffectRowView;
import java.util.List;

/**
 * 스킬 승급 모달의 뷰 모델.
 *
 * <p>현재 랭크와 다음 랭크의 수치, 다형적 효과 목록, 사용 횟수 진행상황, AP 비용 및 보유량, 승급 가능 여부, 최고 랭크 여부, 스킬 유형 식별자/라벨을 포함한다.
 *
 * @param id 스킬 카탈로그 ID
 * @param label 스킬 표시명
 * @param description 스킬 설명 문자열
 * @param currentRankLabel 현재 랭크 라벨
 * @param nextRankLabel 다음 랭크 라벨 (MASTER일 경우 null)
 * @param primaryStatLabel 주요 스탯 라벨 (예: "보너스 데미지", "피해 경감")
 * @param currentValue 현재 랭크 주요 수치 (딜: 배율% / 디펜스: 경감%)
 * @param nextValue 다음 랭크 주요 수치 (MASTER면 0)
 * @param currentCounterValue 현재 랭크 반격 배율% (딜스킬 및 순수방어 스킬은 null)
 * @param nextCounterValue 다음 랭크 반격 배율% (딜스킬 및 순수방어 스킬은 null)
 * @param resourceKindLabel 자원 종류 라벨 ("스태미나" 또는 "MP")
 * @param resourceCost 현재 랭크 자원 소모량
 * @param nextResourceCost 다음 랭크 자원 소모량 (MASTER면 null)
 * @param currentCritBonus 현재 랭크 크리티컬 보너스 (0.1% 단위, 미보유 시 null)
 * @param nextCritBonus 다음 랭크 크리티컬 보너스 (0.1% 단위, MASTER 또는 미보유 시 null)
 * @param rankupBonusText 승급 시 얻는 영구 패시브 스탯 문자열 (없으면 null)
 * @param usageCurrent 현재 사용 횟수
 * @param usageRequired 승급 요구 사용 횟수 (MASTER면 0)
 * @param apCost 승급 AP 비용 (MASTER면 0)
 * @param apOwned 현재 보유 AP
 * @param rankable 승급 가능 여부
 * @param maxed 최고 랭크(MASTER) 여부
 * @param effectRows 스킬별 다형적 효과 상세 행 목록
 * @param passive 패시브 스킬 여부
 * @param hasUsageRequirement 사용 횟수 수련 조건 존재 여부
 * @param typeName 스킬 유형 식별자 (예: "NORMAL", "HEAVY", "CC", "BUFF", "DEBUFF", "DEFENSE", "ULTIMATE",
 *     "RECOVERY", "PASSIVE", "DOT")
 * @param typeLabel 스킬 유형 한글 라벨 (예: "일반", "강", "제어", "버프", "디버프", "방어", "궁극기", "회복", "패시브", "지속피해")
 */
public record SkillRankUpView(
        String id,
        String label,
        String description,
        String currentRankLabel,
        String nextRankLabel,
        String primaryStatLabel,
        int currentValue,
        int nextValue,
        Integer currentCounterValue,
        Integer nextCounterValue,
        String resourceKindLabel,
        int resourceCost,
        Integer nextResourceCost,
        Integer currentCritBonus,
        Integer nextCritBonus,
        String rankupBonusText,
        int usageCurrent,
        int usageRequired,
        int apCost,
        int apOwned,
        boolean rankable,
        boolean maxed,
        List<SkillEffectRowView> effectRows,
        boolean passive,
        boolean hasUsageRequirement,
        String typeName,
        String typeLabel) {

    /** 25인자 하위호환 생성자. */
    public SkillRankUpView(
            final String id,
            final String label,
            final String description,
            final String currentRankLabel,
            final String nextRankLabel,
            final String primaryStatLabel,
            final int currentValue,
            final int nextValue,
            final Integer currentCounterValue,
            final Integer nextCounterValue,
            final String resourceKindLabel,
            final int resourceCost,
            final Integer nextResourceCost,
            final Integer currentCritBonus,
            final Integer nextCritBonus,
            final String rankupBonusText,
            final int usageCurrent,
            final int usageRequired,
            final int apCost,
            final int apOwned,
            final boolean rankable,
            final boolean maxed,
            final List<SkillEffectRowView> effectRows,
            final boolean passive,
            final boolean hasUsageRequirement) {
        this(
                id,
                label,
                description,
                currentRankLabel,
                nextRankLabel,
                primaryStatLabel,
                currentValue,
                nextValue,
                currentCounterValue,
                nextCounterValue,
                resourceKindLabel,
                resourceCost,
                nextResourceCost,
                currentCritBonus,
                nextCritBonus,
                rankupBonusText,
                usageCurrent,
                usageRequired,
                apCost,
                apOwned,
                rankable,
                maxed,
                effectRows,
                passive,
                hasUsageRequirement,
                passive ? "PASSIVE" : "NORMAL",
                passive ? "패시브" : "일반");
    }
}
