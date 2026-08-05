package com.myapps.web.myrpg.domain.repository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.spring.JqwikSpringSupport;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.Vital;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CharacterProgress 영속 라운드트립 프로퍼티 테스트.
 *
 * <p>{@code @DataJpaTest} 슬라이스와 jqwik {@code @Property}를 결합하여,
 * 임의의 유효한 CharacterProgress를 저장 후 조회하면 모든 필드가 보존되는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 12: 진행상황 영속 라운드트립
 *
 * <p><b>Validates: Requirements 3.1</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CharacterProgressRepositoryTest {

    private static final int NICKNAME_MIN_LENGTH = 1;
    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 100;
    private static final int ACCUMULATED_LEVEL_MAX = 1000;
    private static final long EXPERIENCE_MAX = 100_000L;
    private static final int STAT_MIN = 1;
    private static final int STAT_MAX = 100;
    private static final int CRITICAL_DEFENSE_MAX = 50;
    private static final int VITAL_MAX_UPPER = 1000;
    private static final int NODE_ID_MIN_LENGTH = 1;
    private static final int NODE_ID_MAX_LENGTH = 30;

    private final TestEntityManager entityManager;

    CharacterProgressRepositoryTest(final TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 임의의 유효한 CharacterProgress를 저장→조회 시 모든 필드가 보존되는지 검증한다.
     *
     * @param progress 임의 생성된 CharacterProgress 인스턴스
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_savedAndRetrieved(
            @ForAll("validCharacterProgress") final CharacterProgress progress) {
        // Given: 엔티티 저장 후 영속성 컨텍스트 초기화
        entityManager.persistAndFlush(progress);
        final Long savedId = progress.getId();
        entityManager.clear();

        // When: ID로 재조회
        final CharacterProgress found = entityManager.find(CharacterProgress.class, savedId);

        // Then: 모든 필드 보존 검증
        assertThat(found).isNotNull();
        assertThat(found.getNickname()).isEqualTo(progress.getNickname());
        assertThat(found.getCurrentLevel()).isEqualTo(progress.getCurrentLevel());
        assertThat(found.getAccumulatedLevel()).isEqualTo(progress.getAccumulatedLevel());
        assertThat(found.getExperience()).isEqualTo(progress.getExperience());

        // Stats (Embeddable) 보존
        assertThat(found.getStats().str()).isEqualTo(progress.getStats().str());
        assertThat(found.getStats().dex()).isEqualTo(progress.getStats().dex());
        assertThat(found.getStats().intelligence()).isEqualTo(progress.getStats().intelligence());
        assertThat(found.getStats().critical()).isEqualTo(progress.getStats().critical());
        assertThat(found.getStats().defense()).isEqualTo(progress.getStats().defense());

        // HP (Vital Embeddable) 보존
        assertThat(found.getHp().current()).isEqualTo(progress.getHp().current());
        assertThat(found.getHp().max()).isEqualTo(progress.getHp().max());

        // MP (Vital Embeddable) 보존
        assertThat(found.getMp().current()).isEqualTo(progress.getMp().current());
        assertThat(found.getMp().max()).isEqualTo(progress.getMp().max());

        // Stamina (Vital Embeddable) 보존
        assertThat(found.getStamina().current()).isEqualTo(progress.getStamina().current());
        assertThat(found.getStamina().max()).isEqualTo(progress.getStamina().max());

        // currentNodeId 보존
        assertThat(found.getCurrentNodeId()).isEqualTo(progress.getCurrentNodeId());
    }

    /**
     * 유효한 CharacterProgress를 생성하는 Arbitrary 제공자.
     *
     * <p>jqwik Combinators는 최대 8개 파라미터만 지원하므로,
     * Vital 3종을 먼저 결합한 뒤 나머지와 합성한다.
     *
     * @return 임의의 유효한 CharacterProgress Arbitrary
     */
    @Provide
    Arbitrary<CharacterProgress> validCharacterProgress() {
        final Arbitrary<String> nicknames = Arbitraries.strings()
                .alpha()
                .ofMinLength(NICKNAME_MIN_LENGTH)
                .ofMaxLength(NICKNAME_MAX_LENGTH);

        final Arbitrary<Integer> currentLevels = Arbitraries.integers()
                .between(LEVEL_MIN, LEVEL_MAX);

        final Arbitrary<Integer> accumulatedLevels = Arbitraries.integers()
                .between(LEVEL_MIN, ACCUMULATED_LEVEL_MAX);

        final Arbitrary<Long> experiences = Arbitraries.longs()
                .between(0L, EXPERIENCE_MAX);

        final Arbitrary<Stats> stats = buildStatsArbitrary();
        final Arbitrary<VitalTriple> vitals = buildVitalTripleArbitrary();

        final Arbitrary<String> nodeIds = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('-')
                .ofMinLength(NODE_ID_MIN_LENGTH)
                .ofMaxLength(NODE_ID_MAX_LENGTH)
                .filter(s -> !s.isBlank());

        return Combinators.combine(
                nicknames, currentLevels, accumulatedLevels, experiences,
                stats, vitals, nodeIds
        ).as((nickname, level, accumulated, exp, st, vt, nodeId) ->
                new CharacterProgress(nickname, level, accumulated, exp, st, vt.hp(), vt.mp(), vt.stamina(), nodeId)
        );
    }

    private Arbitrary<Stats> buildStatsArbitrary() {
        final Arbitrary<Integer> mainStats = Arbitraries.integers()
                .between(STAT_MIN, STAT_MAX);
        final Arbitrary<Integer> secondaryStats = Arbitraries.integers()
                .between(STAT_MIN, CRITICAL_DEFENSE_MAX);

        return Combinators.combine(mainStats, mainStats, mainStats, secondaryStats, secondaryStats)
                .as(Stats::new);
    }

    private Arbitrary<Vital> buildVitalArbitrary() {
        return Arbitraries.integers()
                .between(LEVEL_MIN, VITAL_MAX_UPPER)
                .flatMap(max -> Arbitraries.integers()
                        .between(0, max)
                        .map(current -> new Vital(current, max)));
    }

    private Arbitrary<VitalTriple> buildVitalTripleArbitrary() {
        return Combinators.combine(buildVitalArbitrary(), buildVitalArbitrary(), buildVitalArbitrary())
                .as(VitalTriple::new);
    }

    /**
     * HP/MP/Stamina 세 Vital을 묶는 내부 레코드.
     *
     * <p>jqwik Combinators 파라미터 수 제한(최대 8개) 우회 목적.
     */
    private record VitalTriple(Vital hp, Vital mp, Vital stamina) {
    }
}
