package com.myapps.web.myrpg.application.dto;

/**
 * 상호작용 대상(NPC, 몬스터, 던전 입장/퇴장 등) 항목을 나타내는 뷰 모델 레코드.
 *
 * @param id 대상 식별자 (NPC인 경우 npcId, 비-NPC는 null 가능)
 * @param name 대상 표시 이름
 * @param npc NPC 여부 (true이면 NPC, false이면 몬스터/던전 등)
 * @param actionType 상호작용 동작 유형 ("npc", "monster", "dungeon-enter", "dungeon-leave", "dungeon-move")
 * @param targetParam 동작에 필요한 대상 매개변수 (던전 ID, 방 ID 등)
 */
public record InteractionItem(
        String id, String name, boolean npc, String actionType, String targetParam) {

    /**
     * 기존 3-인자 호출부와의 하위 호환을 위한 생성자.
     *
     * @param id 대상 식별자
     * @param name 대상 이름
     * @param npc NPC 여부
     */
    public InteractionItem(final String id, final String name, final boolean npc) {
        this(id, name, npc, npc ? "npc" : "monster", id);
    }
}
