package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SkillTalent}의 자원 종류 파생({@code resourceKind()})을 검증하는 프로퍼티 테스트.
 *
 * <p>MAGIC은 {@link ResourceKind#MP}를, 그 외(MELEE/ARCHERY/COMMON)는
 * {@link ResourceKind#STAMINA}를 반환하는지 검증한다.
 *
 * <p><b>Validates: Requirements 9.1</b>
 */
class SkillTalentResourceKindPropertyTest {

    // Feature: 005-skill-system, Property 5: 자원 종류 파생

    /**
     * MAGIC은 MP를, 그 외 재능은 STAMINA를 반환하는지 검증한다.
     *
     * @param talent 임의의 SkillTalent 상수
     */
    @Property(tries = 100)
    void should_deriveCorrectResourceKind_when_anySkillTalent(@ForAll("talents") final SkillTalent talent) {
        if (talent == SkillTalent.MAGIC) {
            assertThat(talent.resourceKind()).isEqualTo(ResourceKind.MP);
        } else {
            assertThat(talent.resourceKind()).isEqualTo(ResourceKind.STAMINA);
        }
    }

    /**
     * 모든 비-MAGIC 재능은 STAMINA를 반환하는지 검증한다.
     *
     * @param talent 비-MAGIC SkillTalent 상수
     */
    @Property(tries = 100)
    void should_returnStamina_when_nonMagicTalent(@ForAll("nonMagicTalents") final SkillTalent talent) {
        assertThat(talent.resourceKind()).isEqualTo(ResourceKind.STAMINA);
    }

    /**
     * SkillTalent 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 전체 SkillTalent 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> talents() {
        return Arbitraries.of(SkillTalent.values());
    }

    /**
     * MAGIC을 제외한 재능만 생성하는 Arbitrary 제공자.
     *
     * @return MELEE/ARCHERY/COMMON 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> nonMagicTalents() {
        return Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.COMMON);
    }
}
