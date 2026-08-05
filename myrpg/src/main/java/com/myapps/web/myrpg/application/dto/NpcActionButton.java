package com.myapps.web.myrpg.application.dto;

/**
 * NPC 행동 버튼을 나타내는 뷰 모델 레코드.
 *
 * <p>NPC와 대화 중일 때 화면 하단에 표시되는 행동 버튼의 라벨을 담는다.
 *
 * @param label 버튼에 표시할 라벨 텍스트
 */
public record NpcActionButton(
        String label
) {
}
