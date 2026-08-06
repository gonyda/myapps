package com.myapps.web.myrpg.domain.model;

/**
 * 경험치 정책을 정의하는 도메인 클래스.
 *
 * <p>다음 레벨까지 필요한 경험치를 산출한다.
 * 경험치 곡선은 2차 함수({@code 100 × L²})로, 레벨이 높아질수록
 * 다음 레벨까지 필요한 경험치가 급격히 증가한다.
 *
 * <h2>확장 지점 안내</h2>
 * <p>경험치 곡선을 변경하려면 이 클래스의 {@link #requiredForNext(int)} 메서드를 재정의하거나,
 * 별도의 정책 구현체를 주입하는 방식으로 교체한다.
 */
public class ExperiencePolicy {

    private static final long BASE_MULTIPLIER = 100L;

    /**
     * 지정된 레벨에서 다음 레벨까지 필요한 경험치를 산출한다.
     *
     * <p>곡선 정책: {@code 100 × level²}.
     * EXP 게이지의 최대값은 이 메서드의 반환값으로 결정된다.
     *
     * @param level 현재 레벨 (1 이상)
     * @return 다음 레벨까지 필요한 총 경험치
     */
    public long requiredForNext(final int level) {
        return BASE_MULTIPLIER * level * level;
    }
}
