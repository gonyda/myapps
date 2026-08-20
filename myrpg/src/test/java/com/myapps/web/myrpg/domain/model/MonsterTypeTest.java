package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link MonsterType}의 실제 매핑값이 요구사항과 정확히 일치하는지 검증하는 단위 테스트.
 *
 * <p>2개 타입의 {@code typeString}→{@code label}·{@code badge}·{@code actionLabels} 매핑과 {@code
 * fromType}의 미지 타입 처리를 확인한다.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
 */
class MonsterTypeTest {

    /** normal 타입의 라벨은 "일반"이고, 배지는 빈 문자열이고, 행동 라벨은 ["전투"]임을 검증한다. */
    @Test
    void should_returnCorrectValues_when_normal() {
        final Optional<MonsterType> result = MonsterType.fromType("normal");

        assertThat(result).isPresent();
        final MonsterType monsterType = result.get();
        assertThat(monsterType).isEqualTo(MonsterType.NORMAL);
        assertThat(monsterType.typeString()).isEqualTo("normal");
        assertThat(monsterType.label()).isEqualTo("일반");
        assertThat(monsterType.badge()).isEqualTo("");
        assertThat(monsterType.actionLabels()).isEqualTo(List.of("전투"));
    }

    /** boss 타입의 라벨은 "보스"이고, 배지는 "👑"이고, 행동 라벨은 ["전투"]임을 검증한다. */
    @Test
    void should_returnCorrectValues_when_boss() {
        final Optional<MonsterType> result = MonsterType.fromType("boss");

        assertThat(result).isPresent();
        final MonsterType monsterType = result.get();
        assertThat(monsterType).isEqualTo(MonsterType.BOSS);
        assertThat(monsterType.typeString()).isEqualTo("boss");
        assertThat(monsterType.label()).isEqualTo("보스");
        assertThat(monsterType.badge()).isEqualTo("\uD83D\uDC51");
        assertThat(monsterType.actionLabels()).isEqualTo(List.of("전투"));
    }

    /** 미지 타입 문자열에 대해 fromType은 빈 Optional을 반환함을 검증한다. */
    @Test
    void should_returnEmpty_when_unknownType() {
        assertThat(MonsterType.fromType("unknown")).isEmpty();
        assertThat(MonsterType.fromType("elite")).isEmpty();
        assertThat(MonsterType.fromType("")).isEmpty();
    }

    /** null 입력에 대해 fromType은 빈 Optional을 반환함을 검증한다. */
    @Test
    void should_returnEmpty_when_nullType() {
        assertThat(MonsterType.fromType(null)).isEmpty();
    }
}
