package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link SkillTalent}의 재능 매칭({@code matchingTalent()})을 검증하는 프로퍼티 테스트.
 *
 * <p>MELEE/ARCHERY/MAGIC은 대응 {@link TalentType}을 반환하고, COMMON은 빈 {@code Optional}을 반환하는지 검증한다.
 *
 * <p><b>Validates: Requirements 2.2, 3.1, 3.2</b>
 */
class SkillTalentMatchingPropertyTest {

    // Feature: 005-skill-system, Property 4: 재능 매칭

    /**
     * 전투 재능(MELEE/ARCHERY/MAGIC)은 대응 TalentType을 반환하고, COMMON은 빈 Optional을 반환하는지 검증한다.
     *
     * @param talent 임의의 SkillTalent 상수
     */
    @Property(tries = 100)
    void should_matchCorrectTalentType_when_anySkillTalent(
            @ForAll("talents") final SkillTalent talent) {
        final Optional<TalentType> matching = talent.matchingTalent();

        switch (talent) {
            case MELEE -> assertThat(matching).isEqualTo(Optional.of(TalentType.MELEE));
            case ARCHERY -> assertThat(matching).isEqualTo(Optional.of(TalentType.ARCHERY));
            case MAGIC -> assertThat(matching).isEqualTo(Optional.of(TalentType.MAGIC));
            case COMMON -> assertThat(matching).isEmpty();
        }
    }

    /**
     * COMMON이 아닌 재능은 matchingTalent()가 반드시 present인지 검증한다.
     *
     * @param talent 임의의 SkillTalent 상수
     */
    @Property(tries = 100)
    void should_havePresentMatchingTalent_when_notCommon(
            @ForAll("combatTalents") final SkillTalent talent) {
        assertThat(talent.matchingTalent()).isPresent();
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
     * COMMON을 제외한 전투 재능만 생성하는 Arbitrary 제공자.
     *
     * @return MELEE/ARCHERY/MAGIC 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> combatTalents() {
        return Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.MAGIC);
    }
}
