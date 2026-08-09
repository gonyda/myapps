package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 스킬 목록 팝업의 전체 뷰 모델.
 *
 * <p>현재 활성 탭과 해당 탭에 맞는 스킬 행 목록을 포함한다.
 *
 * @param activeTab 현재 활성 탭 (null 또는 "melee"/"archery"/"magic"/"common")
 * @param rows      필터링된 스킬 행 목록
 */
public record SkillListView(
        String activeTab,
        List<SkillRowView> rows
) {
}
