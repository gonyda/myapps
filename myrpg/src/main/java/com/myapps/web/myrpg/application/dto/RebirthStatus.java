package com.myapps.web.myrpg.application.dto;

import java.time.Duration;

/**
 * 환생 가능 여부 및 쿨다운 상태를 나타내는 DTO.
 *
 * <p>현재 환생 가능 여부, 과거 환생 이력, 경과 시간, 남은 쿨다운 시간을 포함한다.
 *
 * @param available      환생 가능 여부
 * @param everRebirthed  환생한 적이 있는지 여부
 * @param elapsed        마지막 환생으로부터 경과한 시간
 * @param remaining      환생 가능까지 남은 쿨다운 시간
 */
public record RebirthStatus(boolean available, boolean everRebirthed, Duration elapsed, Duration remaining) {
}
