package com.myapps.web.myrpg.interfaces.dto;

/**
 * 스킬북 장착 폼 DTO.
 *
 * <p>대상 무기, 장착할 스킬, 덮어쓸 슬롯 인덱스를 전달받는다.
 */
public record AttachSkillForm(Long weaponId, Long skillId, Integer overwriteSlot) {
}
