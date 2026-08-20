package com.myapps.web.myrpg.application.dto;

import java.time.Duration;

/**
 * 환생 시도 결과를 나타내는 sealed 인터페이스.
 *
 * <p>환생 성공({@link Reborn}) 또는 쿨다운으로 인한 거부({@link CooldownActive})를 표현한다.
 */
public sealed interface RebirthResult permits RebirthResult.Reborn, RebirthResult.CooldownActive {

    /**
     * 환생 성공 결과.
     *
     * <p>캐릭터가 레벨 1로 초기화되고 누적 레벨이 증가한다.
     */
    record Reborn() implements RebirthResult {}

    /**
     * 쿨다운 활성 상태로 인한 환생 거부 결과.
     *
     * <p>환생 가능 시점까지 남은 시간을 포함한다.
     *
     * @param remaining 환생 가능까지 남은 쿨다운 시간
     */
    record CooldownActive(Duration remaining) implements RebirthResult {}
}
