package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import org.junit.jupiter.api.Test;

/**
 * {@link CharacterProgress}의 골드 관련 메서드 경계값 단위 테스트.
 *
 * <p>신규 캐릭터 초기값, 양수 획득, 정확히 보유액 소모, 초과 소모 예외 및 불변 보증, 비양수 금액에 대한 방어를 검증한다.
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5</b>
 */
class CharacterProgressGoldTest {

    /** 신규 캐릭터(createDefault)의 초기 골드가 0임을 검증한다. */
    @Test
    void should_have_zero_gold_when_created() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        assertThat(progress.getGold()).isEqualTo(0L);
    }

    /** 양수 금액으로 gainGold 호출 시 골드가 정확히 증가함을 검증한다. */
    @Test
    void should_gain_gold_when_positive_amount() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        progress.gainGold(500L);

        assertThat(progress.getGold()).isEqualTo(500L);
    }

    /** 보유 골드와 정확히 같은 금액을 소모할 때 골드가 0이 됨을 검증한다. */
    @Test
    void should_spend_gold_when_exactly_balance() {
        final CharacterProgress progress = createProgressWithGold(300L);

        progress.spendGold(300L);

        assertThat(progress.getGold()).isEqualTo(0L);
    }

    /** 보유 골드를 초과하는 금액 소모 시 {@link InsufficientGoldException}이 발생함을 검증한다. */
    @Test
    void should_throw_exception_when_spend_exceeds_balance() {
        final CharacterProgress progress = createProgressWithGold(100L);

        assertThatThrownBy(() -> progress.spendGold(101L))
                .isInstanceOf(InsufficientGoldException.class);
    }

    /** 소모 실패(예외 발생) 후 골드가 변하지 않음을 검증한다. */
    @Test
    void should_not_change_gold_when_spend_fails() {
        final CharacterProgress progress = createProgressWithGold(50L);

        try {
            progress.spendGold(100L);
        } catch (InsufficientGoldException ignored) {
            // 예외 발생이 기대됨
        }

        assertThat(progress.getGold()).isEqualTo(50L);
    }

    /** gainGold에 0 또는 음수를 전달하면 {@link IllegalArgumentException}이 발생함을 검증한다. */
    @Test
    void should_throw_when_gain_zero_or_negative() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        assertThatThrownBy(() -> progress.gainGold(0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> progress.gainGold(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** spendGold에 0 또는 음수를 전달하면 {@link IllegalArgumentException}이 발생함을 검증한다. */
    @Test
    void should_throw_when_spend_zero_or_negative() {
        final CharacterProgress progress = createProgressWithGold(100L);

        assertThatThrownBy(() -> progress.spendGold(0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> progress.spendGold(-5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 지정된 골드 값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param gold 설정할 보유 골드
     * @return 해당 골드를 보유한 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithGold(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", 0, gold);
    }
}
