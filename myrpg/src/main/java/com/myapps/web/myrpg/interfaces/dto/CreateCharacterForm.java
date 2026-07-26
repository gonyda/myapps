package com.myapps.web.myrpg.interfaces.dto;

/**
 * 캐릭터 생성 폼 DTO.
 *
 * <p>생성할 캐릭터의 이름을 전달받는다.
 */
public record CreateCharacterForm(String name) {
}
