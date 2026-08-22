package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ItemDrop;
import java.util.List;

/**
 * 던전 보스 처치 시 지급되는 최종 클리어 보상 설정을 나타내는 불변 레코드.
 *
 * @param exp 지급 경험치 (0 이상)
 * @param gold 지급 골드 (0 이상)
 * @param itemDrops 지급 대상 확률형 아이템 드랍 목록
 */
public record DungeonRewardSpec(int exp, int gold, List<ItemDrop> itemDrops) {

    /** 방어적 복사를 적용한 생성자. */
    public DungeonRewardSpec {
        itemDrops = itemDrops != null ? List.copyOf(itemDrops) : List.of();
    }
}
