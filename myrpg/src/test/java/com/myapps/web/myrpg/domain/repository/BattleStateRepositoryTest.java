package com.myapps.web.myrpg.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/**
 * 전투 상태 영속 왕복 프로퍼티 테스트.
 *
 * <p>{@code @DataJpaTest} 슬라이스와 jqwik {@code @Property}를 결합하여, 임의의 유효한 {@link BattleState}를 저장 후
 * 조회하면 모든 필드가 보존되는지, {@link BattleStateRepository#findByCharacterIdAndActiveTrue(long)}가 활성 전투만
 * 반환하는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 18: 전투 상태 영속 왕복
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.4, 1.6</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BattleStateRepositoryTest {

    private static final long CHARACTER_ID_MIN = 1L;
    private static final long CHARACTER_ID_MAX = 10_000L;
    private static final int MONSTER_HP_MIN = 1;
    private static final int MONSTER_HP_MAX = 9999;
    private static final int MONSTER_ID_MIN_LENGTH = 3;
    private static final int MONSTER_ID_MAX_LENGTH = 20;
    private static final int TURN_COUNT_UPDATE_MIN = 2;
    private static final int TURN_COUNT_UPDATE_MAX = 50;

    private final BattleStateRepository battleStateRepository;
    private final TestEntityManager entityManager;

    BattleStateRepositoryTest(
            final BattleStateRepository battleStateRepository,
            final TestEntityManager entityManager) {
        this.battleStateRepository = battleStateRepository;
        this.entityManager = entityManager;
    }

    // Feature: 008-battle-system, Property 18: 전투 상태 영속 왕복

    /**
     * 임의의 유효한 BattleState를 저장→재조회 시 모든 필드가 보존되는지 검증한다.
     *
     * @param characterId 캐릭터 ID
     * @param monsterId 몬스터 식별자
     * @param monsterCurrentHp 몬스터 현재 HP
     * @param ambush 기습 여부
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_savedAndRetrieved(
            @ForAll("characterIds") final long characterId,
            @ForAll("monsterIds") final String monsterId,
            @ForAll("monsterHps") final int monsterCurrentHp,
            @ForAll("ambushFlags") final boolean ambush) {

        final BattleState state = new BattleState(characterId, monsterId, monsterCurrentHp, ambush);

        entityManager.persistAndFlush(state);
        final Long savedId = state.getId();
        entityManager.clear();

        final BattleState found = entityManager.find(BattleState.class, savedId);

        assertThat(found).isNotNull();
        assertThat(found.getCharacterId()).isEqualTo(characterId);
        assertThat(found.getMonsterId()).isEqualTo(monsterId);
        assertThat(found.getMonsterCurrentHp()).isEqualTo(monsterCurrentHp);
        assertThat(found.getTurnCount()).isEqualTo(1);
        assertThat(found.isAmbush()).isEqualTo(ambush);
        assertThat(found.isActive()).isTrue();
        assertThat(found.isStandby()).isTrue();
        assertThat(found.getCurrentMonsterIntent()).isNull();
    }

    /**
     * 동일 캐릭터에 활성·비활성 전투가 있을 때 {@code findByCharacterIdAndActiveTrue}가 활성 전투만 반환하는지 검증한다.
     *
     * @param characterId 캐릭터 ID
     * @param monsterId 몬스터 식별자
     * @param monsterCurrentHp 몬스터 현재 HP
     */
    @Property(tries = 100)
    void should_returnOnlyActive_when_activeAndInactiveExist(
            @ForAll("characterIds") final long characterId,
            @ForAll("monsterIds") final String monsterId,
            @ForAll("monsterHps") final int monsterCurrentHp) {

        final BattleState activeState =
                new BattleState(characterId, monsterId, monsterCurrentHp, false);
        entityManager.persistAndFlush(activeState);

        final BattleState inactiveState =
                new BattleState(characterId, monsterId, monsterCurrentHp, true);
        inactiveState.setActive(false);
        entityManager.persistAndFlush(inactiveState);
        entityManager.clear();

        final Optional<BattleState> result =
                battleStateRepository.findByCharacterIdAndActiveTrue(characterId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(activeState.getId());
        assertThat(result.get().isActive()).isTrue();
    }

    /**
     * 비활성 전투만 있을 때 {@code findByCharacterIdAndActiveTrue}가 빈 {@code Optional}을 반환하는지 검증한다.
     *
     * @param characterId 캐릭터 ID
     * @param monsterId 몬스터 식별자
     * @param monsterCurrentHp 몬스터 현재 HP
     */
    @Property(tries = 100)
    void should_returnEmpty_when_noActiveBattle(
            @ForAll("characterIds") final long characterId,
            @ForAll("monsterIds") final String monsterId,
            @ForAll("monsterHps") final int monsterCurrentHp) {

        final BattleState inactiveState =
                new BattleState(characterId, monsterId, monsterCurrentHp, false);
        inactiveState.setActive(false);
        entityManager.persistAndFlush(inactiveState);
        entityManager.clear();

        final Optional<BattleState> result =
                battleStateRepository.findByCharacterIdAndActiveTrue(characterId);

        assertThat(result).isEmpty();
    }

    /**
     * 저장 후 monsterCurrentHp, turnCount, standby, currentMonsterIntent를 수정·재저장하면 재조회 시 갱신된 값이 보존되는지
     * 검증한다.
     *
     * @param characterId 캐릭터 ID
     * @param monsterId 몬스터 식별자
     * @param monsterCurrentHp 초기 몬스터 HP
     * @param updatedHp 갱신할 몬스터 HP
     * @param updatedTurn 갱신할 턴 수
     * @param updatedStandby 갱신할 대치 페이즈 여부
     * @param updatedIntent 갱신할 몬스터 의도
     */
    @Property(tries = 100)
    void should_persistUpdatedValues_when_hpAndTurnCountModified(
            @ForAll("characterIds") final long characterId,
            @ForAll("monsterIds") final String monsterId,
            @ForAll("monsterHps") final int monsterCurrentHp,
            @ForAll("monsterHps") final int updatedHp,
            @ForAll("updatedTurnCounts") final int updatedTurn,
            @ForAll("standbyFlags") final boolean updatedStandby,
            @ForAll("monsterIntents") final SkillType updatedIntent) {

        final BattleState state = new BattleState(characterId, monsterId, monsterCurrentHp, false);
        entityManager.persistAndFlush(state);
        final Long savedId = state.getId();
        entityManager.clear();

        final BattleState loaded = entityManager.find(BattleState.class, savedId);
        loaded.setMonsterCurrentHp(updatedHp);
        loaded.setTurnCount(updatedTurn);
        loaded.setStandby(updatedStandby);
        loaded.setCurrentMonsterIntent(updatedIntent);
        entityManager.persistAndFlush(loaded);
        entityManager.clear();

        final BattleState reloaded = entityManager.find(BattleState.class, savedId);

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getMonsterCurrentHp()).isEqualTo(updatedHp);
        assertThat(reloaded.getTurnCount()).isEqualTo(updatedTurn);
        assertThat(reloaded.getCharacterId()).isEqualTo(characterId);
        assertThat(reloaded.getMonsterId()).isEqualTo(monsterId);
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.isStandby()).isEqualTo(updatedStandby);
        assertThat(reloaded.getCurrentMonsterIntent()).isEqualTo(updatedIntent);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 캐릭터 ID Arbitrary를 제공한다 (1~10,000).
     *
     * @return 캐릭터 ID Arbitrary
     */
    @Provide
    Arbitrary<Long> characterIds() {
        return Arbitraries.longs().between(CHARACTER_ID_MIN, CHARACTER_ID_MAX);
    }

    /**
     * 몬스터 ID Arbitrary를 제공한다 (영소문자+하이픈, 3~20자).
     *
     * @return 몬스터 ID Arbitrary
     */
    @Provide
    Arbitrary<String> monsterIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('-')
                .ofMinLength(MONSTER_ID_MIN_LENGTH)
                .ofMaxLength(MONSTER_ID_MAX_LENGTH)
                .filter(s -> !s.isBlank() && !s.startsWith("-") && !s.endsWith("-"));
    }

    /**
     * 몬스터 HP Arbitrary를 제공한다 (1~9,999).
     *
     * @return 몬스터 HP Arbitrary
     */
    @Provide
    Arbitrary<Integer> monsterHps() {
        return Arbitraries.integers().between(MONSTER_HP_MIN, MONSTER_HP_MAX);
    }

    /**
     * 기습 여부 Arbitrary를 제공한다 (true/false).
     *
     * @return 기습 여부 Arbitrary
     */
    @Provide
    Arbitrary<Boolean> ambushFlags() {
        return Arbitraries.of(true, false);
    }

    /**
     * 대치 상태 Arbitrary를 제공한다 (true/false).
     *
     * @return 대치 상태 Arbitrary
     */
    @Provide
    Arbitrary<Boolean> standbyFlags() {
        return Arbitraries.of(true, false);
    }

    /**
     * 몬스터 의도 Arbitrary를 제공한다 (NORMAL, HEAVY, DEFENSE, null).
     *
     * @return 몬스터 의도 Arbitrary
     */
    @Provide
    Arbitrary<SkillType> monsterIntents() {
        return Arbitraries.of(SkillType.class).injectNull(0.2);
    }

    /**
     * 갱신 턴 수 Arbitrary를 제공한다 (2~50).
     *
     * @return 갱신 턴 수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> updatedTurnCounts() {
        return Arbitraries.integers().between(TURN_COUNT_UPDATE_MIN, TURN_COUNT_UPDATE_MAX);
    }
}
