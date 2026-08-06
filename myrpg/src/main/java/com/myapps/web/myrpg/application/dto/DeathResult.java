package com.myapps.web.myrpg.application.dto;

/**
 * 사망 패널티 적용 결과를 나타내는 DTO.
 *
 * <p>사망 시 차감된 경험치량을 포함한다.
 *
 * @param experienceLost 사망으로 인해 차감된 경험치량
 */
public record DeathResult(long experienceLost) {
}
