package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link Monster#buttonLabel()} 포맷을 검증하는 프로퍼티 테스트.
 *
 * <p>NORMAL 타입(배지 빈 문자열)은 이름만 반환하고, BOSS 타입(배지 "👑")은 이름 뒤에 공백과 배지를 붙여 반환함을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 2: 몬스터 버튼 라벨 포맷
 *
 * <p><b>Validates: Requirements 4.6, 10.4</b>
 */
class MonsterButtonLabelPropertyTest {

    private static final List<String> DEFAULT_LINES = List.of("소리", "행동1", "행동2");

    /**
     * NORMAL 타입 몬스터의 buttonLabel()은 이름과 정확히 같음을 검증한다.
     *
     * @param monster NORMAL 타입의 임의 몬스터
     */
    @Property(tries = 100)
    void should_returnNameOnly_when_normalType(@ForAll("normalMonsters") final Monster monster) {
        assertThat(monster.buttonLabel()).isEqualTo(monster.name());
    }

    /**
     * BOSS 타입 몬스터의 buttonLabel()은 이름 + 공백 + 배지("👑")임을 검증한다.
     *
     * @param monster BOSS 타입의 임의 몬스터
     */
    @Property(tries = 100)
    void should_returnNameWithBadge_when_bossType(@ForAll("bossMonsters") final Monster monster) {
        final String expected = monster.name() + " " + MonsterType.BOSS.badge();
        assertThat(monster.buttonLabel()).isEqualTo(expected);
    }

    /**
     * NORMAL 타입의 임의 몬스터를 생성하는 Arbitrary 제공자.
     *
     * @return NORMAL 타입 Monster를 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<Monster> normalMonsters() {
        return monsterWith(MonsterType.NORMAL);
    }

    /**
     * BOSS 타입의 임의 몬스터를 생성하는 Arbitrary 제공자.
     *
     * @return BOSS 타입 Monster를 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<Monster> bossMonsters() {
        return monsterWith(MonsterType.BOSS);
    }

    private Arbitrary<Monster> monsterWith(final MonsterType type) {
        final Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        final Arbitrary<String> names =
                Arbitraries.strings()
                        .ofMinLength(1)
                        .ofMaxLength(10)
                        .filter(name -> !name.isBlank());

        return Combinators.combine(ids, names)
                .as(
                        (id, name) ->
                                new Monster(
                                        id,
                                        name,
                                        type,
                                        1,
                                        25,
                                        4,
                                        1,
                                        10,
                                        15L,
                                        new GoldDrop(3, 10),
                                        List.of(),
                                        DEFAULT_LINES));
    }
}
