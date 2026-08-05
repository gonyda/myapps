package com.myapps.web.myrpg.application.dto;

/**
 * 상호작용 대상(몬스터 또는 NPC) 항목을 나타내는 뷰 모델 레코드.
 *
 * @param name 대상 이름
 * @param npc  NPC 여부 (true이면 NPC, false이면 몬스터)
 */
public record InteractionItem(
        String name,
        boolean npc
) {
}
