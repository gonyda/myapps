package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 스킬 목록 팝업의 전체 뷰 모델.
 *
 * <p>현재 활성 탭과 해당 탭에 맞는 스킬 행 목록, 그리고 상단 10개 핫바 슬롯 목록을 포함한다.
 *
 * @param activeTab 현재 활성 탭 (null 또는 "all"/"melee"/"archery"/"magic"/"common")
 * @param rows 필터링된 스킬 행 목록
 * @param slots 상단 10개 핫바 슬롯 목록 (0~9번)
 */
public record SkillListView(
        String activeTab, List<SkillRowView> rows, List<BattleSkillButton> slots) {

    /** 하위호환 생성자 (slots = 빈 리스트). */
    public SkillListView(final String activeTab, final List<SkillRowView> rows) {
        this(activeTab, rows, List.of());
    }
}
