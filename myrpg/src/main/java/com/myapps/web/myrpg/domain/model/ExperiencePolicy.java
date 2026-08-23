package com.myapps.web.myrpg.domain.model;

/**
 * 경험치 정책을 정의하는 도메인 클래스.
 *
 * <p>다음 레벨까지 필요한 경험치를 산출한다. 경험치 곡선은 1차+2차 복합 다항 함수({@code 50 × L + 15 × L²})로, 초반에는 레벨업이 비교적 빠르고
 * 고레벨로 갈수록 점진적으로 난이도가 상승한다.
 *
 * <h2>확장 지점 안내</h2>
 *
 * <p>경험치 곡선을 변경하려면 이 클래스의 {@link #requiredForNext(int)} 메서드를 재정의하거나, 별도의 정책 구현체를 주입하는 방식으로 교체한다.
 */
public class ExperiencePolicy {

    private static final long LINEAR_COEFFICIENT = 50L;
    private static final long QUADRATIC_COEFFICIENT = 15L;

    /**
     * 지정된 레벨에서 다음 레벨까지 필요한 경험치를 산출한다.
     *
     * <p>곡선 정책: {@code 50 × level + 15 × level²}. EXP 게이지의 최대값은 이 메서드의 반환값으로 결정된다.
     *
     * @param level 현재 레벨 (1 이상)
     * @return 다음 레벨까지 필요한 총 경험치
     */
    public long requiredForNext(final int level) {
        return (LINEAR_COEFFICIENT * level) + (QUADRATIC_COEFFICIENT * (long) level * level);
    }
}
