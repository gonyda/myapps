package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.Npc;

/**
 * 현재 대사 대상(NPC 또는 몬스터)을 묶는 뷰 모델 레코드.
 *
 * <p>{@code buildPlayScreen} 파라미터 폭증을 막기 위해 대사 대상(NPC 또는 몬스터)과 선택된 대사를 하나의 레코드로 묶는다. NPC 대사와 몬스터
 * 대사는 동시에 활성되지 않는다(둘 중 하나만 non-null).
 *
 * @param npc NPC 대사 대상 (몬스터 대상이면 null)
 * @param monster 몬스터 대사 대상 (NPC 대상이면 null)
 * @param dialogue 선택된 대사 텍스트 (대상 없으면 null)
 */
public record TalkTarget(Npc npc, Monster monster, String dialogue) {

    /** 대사 대상 없음을 나타내는 빈 인스턴스. */
    public static final TalkTarget EMPTY = new TalkTarget(null, null, null);

    /**
     * NPC를 대사 대상으로 지정한다.
     *
     * @param npc 대사 대상 NPC
     * @param dialogue 선택된 대사 텍스트
     * @return NPC 대사 대상을 담은 TalkTarget
     */
    public static TalkTarget ofNpc(final Npc npc, final String dialogue) {
        return new TalkTarget(npc, null, dialogue);
    }

    /**
     * 몬스터를 대사 대상으로 지정한다.
     *
     * @param monster 대사 대상 몬스터
     * @param dialogue 선택된 대사 텍스트
     * @return 몬스터 대사 대상을 담은 TalkTarget
     */
    public static TalkTarget ofMonster(final Monster monster, final String dialogue) {
        return new TalkTarget(null, monster, dialogue);
    }
}
