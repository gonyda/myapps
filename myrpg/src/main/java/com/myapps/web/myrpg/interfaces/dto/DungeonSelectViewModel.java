package com.myapps.web.myrpg.interfaces.dto;

import java.util.List;

/**
 * 던전 선택 화면에 전달할 뷰 모델.
 *
 * <p>선택 가능한 던전 목록과 플레이어 레벨을 표현한다.
 */
public record DungeonSelectViewModel(List<DungeonInfo> dungeons, int playerLevel) {

    /**
     * 단일 던전의 표시 정보를 나타내는 레코드.
     *
     * @param id            던전 ID
     * @param name          던전 이름
     * @param difficulty    난이도
     * @param requiredLevel 입장 요구 레벨
     */
    public record DungeonInfo(long id, String name, int difficulty, int requiredLevel) {
    }
}
