package com.myapps.web.myrpg.interfaces.dto;

import java.util.List;

import com.myapps.web.myrpg.application.service.BattleSession;

/**
 * 전투 화면에 전달할 뷰 모델.
 *
 * <p>전투 세션 상태와 사용 가능한 스킬 및 포션 정보를 표현한다.
 */
public record BattleViewModel(BattleSession battleSession,
                              List<SkillInfo> playerSkills,
                              List<PotionInfo> playerPotions) {

    /**
     * 사용 가능한 스킬 정보를 나타내는 레코드.
     *
     * @param skillId        스킬 ID
     * @param name           스킬 이름
     * @param mpCost         MP 소모량
     * @param damageMultiplier 데미지 배율
     */
    public record SkillInfo(long skillId, String name, int mpCost,
                            double damageMultiplier) {
    }

    /**
     * 사용 가능한 포션 정보를 나타내는 레코드.
     *
     * @param itemId       아이템 ID
     * @param name         포션 이름
     * @param effectAmount 회복량
     * @param quantity     보유 수량
     * @param isHp         HP 포션 여부 (false면 MP 포션)
     */
    public record PotionInfo(long itemId, String name, int effectAmount,
                             int quantity, boolean isHp) {
    }
}
