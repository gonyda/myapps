package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 몬스터 처치 시 드랍 계산 결과를 나타내는 불변 레코드.
 *
 * <p>골드(필수)와 아이템 목록(0개 이상)을 포함한다.
 * 실제 골드 가산 및 인벤토리 적재는 6순위(전투 완료 후)에서
 * 이 결과를 소비하여 처리하며, 본 레코드는 순수 계산 결과만 담는다.
 *
 * @param gold  드랍된 골드 금액 (0 이상)
 * @param items 드랍된 아이템 목록 (빈 목록 가능)
 */
public record DropResult(long gold, List<DroppedItem> items) {

    /**
     * 드랍 없음을 나타내는 빈 결과 상수.
     */
    public static final DropResult EMPTY = new DropResult(0L, List.of());
}
