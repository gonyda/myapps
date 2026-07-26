package com.myapps.web.myrpg.interfaces.dto;

import com.myapps.web.myrpg.domain.model.vo.DropResult;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;

/**
 * 전투 승리/드랍 결과 화면에 전달할 뷰 모델.
 *
 * <p>처치한 몬스터, 획득 보상, 드랍 아이템, 레벨업 정보를 표현한다.
 */
public record BattleResultViewModel(String monsterName, int expGained, int goldGained,
                                    DropResult dropResult, LevelUpResult levelUpResult,
                                    String dropDescription, boolean isBossKill,
                                    boolean dungeonCleared) {
}
