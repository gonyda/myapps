package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link MonsterType} 매핑 완전성을 검증하는 프로퍼티 테스트.
 *
 * <p>모든 {@link MonsterType} 상수에 대해 label/actionLabels가 비어 있지 않고, badge가 타입별 규약을 준수하며, fromType 왕복
 * 변환이 정확하고, 미지/null 코드는 빈 Optional을 반환함을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 1: 몬스터 타입 완전성
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
 */
class MonsterTypeCompletenessPropertyTest {

    /**
     * 모든 MonsterType 상수의 label()은 비어 있지 않은 문자열이고, actionLabels()는 비어 있지 않은 목록임을 검증한다.
     *
     * @param monsterType 임의의 MonsterType 상수
     */
    @Property(tries = 100)
    void should_haveLabelAndActionLabels_when_anyMonsterType(
            @ForAll("monsterTypes") final MonsterType monsterType) {
        final String label = monsterType.label();
        assertThat(label).isNotNull().isNotEmpty();

        final List<String> actionLabels = monsterType.actionLabels();
        assertThat(actionLabels).isNotNull().isNotEmpty();
    }

    /**
     * NORMAL 타입의 badge()는 빈 문자열이고, BOSS 타입의 badge()는 "👑"임을 검증한다.
     *
     * @param monsterType 임의의 MonsterType 상수
     */
    @Property(tries = 100)
    void should_returnCorrectBadge_when_anyMonsterType(
            @ForAll("monsterTypes") final MonsterType monsterType) {
        if (monsterType == MonsterType.NORMAL) {
            assertThat(monsterType.badge()).isEqualTo("");
        } else if (monsterType == MonsterType.BOSS) {
            assertThat(monsterType.badge()).isEqualTo("\uD83D\uDC51");
        }
    }

    /**
     * 모든 MonsterType 상수에 대해 fromType(typeString())은 자기 자신을 반환함을 검증한다.
     *
     * @param monsterType 임의의 MonsterType 상수
     */
    @Property(tries = 100)
    void should_returnSelf_when_fromTypeWithOwnTypeString(
            @ForAll("monsterTypes") final MonsterType monsterType) {
        final Optional<MonsterType> result = MonsterType.fromType(monsterType.typeString());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(monsterType);
    }

    /**
     * 미지 문자열에 대해 fromType은 빈 Optional을 반환함을 검증한다.
     *
     * @param unknownType 유효하지 않은 임의의 문자열
     */
    @Property(tries = 100)
    void should_returnEmpty_when_fromTypeWithUnknownString(
            @ForAll("unknownTypes") final String unknownType) {
        final Optional<MonsterType> result = MonsterType.fromType(unknownType);

        assertThat(result).isEmpty();
    }

    /** null 입력에 대해 fromType은 빈 Optional을 반환함을 검증한다. */
    @Property(tries = 100)
    void should_returnEmpty_when_fromTypeWithNull() {
        final Optional<MonsterType> result = MonsterType.fromType(null);

        assertThat(result).isEmpty();
    }

    /**
     * MonsterType 상수를 균등하게 선택하는 Arbitrary 제공자.
     *
     * @return 전체 MonsterType 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<MonsterType> monsterTypes() {
        return Arbitraries.of(MonsterType.values());
    }

    /**
     * 유효한 MonsterType 코드와 겹치지 않는 임의의 문자열을 생성하는 Arbitrary 제공자.
     *
     * @return 미지 타입 문자열을 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<String> unknownTypes() {
        final Set<String> validTypes = Set.of("normal", "boss");
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(type -> !validTypes.contains(type));
    }
}
