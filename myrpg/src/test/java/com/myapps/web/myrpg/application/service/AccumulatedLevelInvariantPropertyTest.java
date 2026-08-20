package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 누적레벨 불변식 프로퍼티 테스트.
 *
 * <p>레벨업과 환생의 임의 시퀀스에서 {@code accumulatedLevel}이 레벨업 1회당 +1, 환생 1회당 +1 증가하며 절대 감소하지 않음을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 4: 누적레벨 불변식
 *
 * <p><b>Validates: Requirements 1.2, 1.4, 8.2</b>
 */
class AccumulatedLevelInvariantPropertyTest {

    private static final int INITIAL_ACCUMULATED_LEVEL = 1;

    /**
     * 레벨업·환생 임의 시퀀스에서 누적레벨이 레벨업당 +1, 환생당 +1 증가하며 감소하지 않음을 검증한다.
     *
     * <p>불변식: {@code accumulatedLevel == initialAccumulated + totalLevelsGained + totalRebirths}
     *
     * @param actions 실행할 액션 시퀀스 (경험치 획득량 또는 환생)
     */
    @Property(tries = 100)
    void should_maintainAccumulatedLevelInvariant_when_levelUpAndRebirthSequence(
            @ForAll("actionSequence") final List<Action> actions) {
        // Given: 고정 클럭과 실제 정책을 사용하는 ProgressionService
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2099-01-01T00:00:00Z"), ZoneId.of("UTC"));
        final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
        final StatProgression statProgression = new StatProgression();
        final ProgressionService service =
                new ProgressionService(experiencePolicy, statProgression, fixedClock);

        // Given: 신규 캐릭터 (Lv1, 누적 1, EXP 0)
        final CharacterProgress progress = CharacterProgress.createDefault();
        final int initialAccumulated = progress.getAccumulatedLevel();
        assertThat(initialAccumulated).isEqualTo(INITIAL_ACCUMULATED_LEVEL);

        int totalLevelsGained = 0;
        int totalRebirths = 0;
        int previousAccumulated = initialAccumulated;

        // When: 액션 시퀀스 실행
        for (final Action action : actions) {
            switch (action) {
                case Action.GainExp gainExp -> {
                    final LevelUpResult result = service.gainExperience(progress, gainExp.amount());
                    totalLevelsGained += result.levelsGained();
                }
                case Action.Rebirth ignored -> {
                    // 환생 쿨다운을 우회하기 위해 lastRebirthAt을 null로 설정
                    progress.setLastRebirthAt(null);
                    final RebirthResult result = service.rebirth(progress);
                    if (result instanceof RebirthResult.Reborn) {
                        totalRebirths++;
                    }
                }
            }

            // Then: 누적레벨은 절대 감소하지 않는다
            final int currentAccumulated = progress.getAccumulatedLevel();
            assertThat(currentAccumulated)
                    .as("누적레벨은 감소하지 않아야 한다")
                    .isGreaterThanOrEqualTo(previousAccumulated);
            previousAccumulated = currentAccumulated;
        }

        // Then: 최종 불변식 검증
        final int expectedAccumulated = initialAccumulated + totalLevelsGained + totalRebirths;
        assertThat(progress.getAccumulatedLevel())
                .as("누적레벨 = 초기값 + 레벨업 횟수 + 환생 횟수")
                .isEqualTo(expectedAccumulated);
    }

    /**
     * 경험치 획득과 환생 액션의 임의 시퀀스를 생성하는 Provider.
     *
     * @return 액션 시퀀스 Arbitrary
     */
    @Provide
    Arbitrary<List<Action>> actionSequence() {
        final Arbitrary<Action> gainExp =
                Arbitraries.longs().between(1L, 50000L).map(Action.GainExp::new);

        final Arbitrary<Action> rebirth = Arbitraries.just(Action.Rebirth.INSTANCE);

        final Arbitrary<Action> action =
                Arbitraries.frequencyOf(
                        net.jqwik.api.Tuple.of(7, gainExp), net.jqwik.api.Tuple.of(3, rebirth));

        return action.list().ofMinSize(1).ofMaxSize(20);
    }

    /** 테스트에서 사용하는 액션 모델. */
    sealed interface Action permits Action.GainExp, Action.Rebirth {

        /**
         * 경험치 획득 액션.
         *
         * @param amount 획득할 경험치량
         */
        record GainExp(long amount) implements Action {}

        /** 환생 액션. */
        enum Rebirth implements Action {
            /** 환생 액션 싱글턴. */
            INSTANCE
        }
    }
}
