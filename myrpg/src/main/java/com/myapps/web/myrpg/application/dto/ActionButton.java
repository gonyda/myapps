package com.myapps.web.myrpg.application.dto;

/**
 * NPC·몬스터 행동 버튼을 나타내는 뷰 모델 레코드.
 *
 * <p>NPC 또는 몬스터와 상호작용 시 화면 하단에 표시되는 행동 버튼의 라벨을 담는다.
 *
 * @param label 버튼에 표시할 라벨 텍스트
 */
public record ActionButton(
        String label
) {
}
