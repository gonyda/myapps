package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.io.InputStream;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeProperty;
import tools.jackson.databind.ObjectMapper;

/** 메디테이션(Meditation) 턴당 MP 회복의 랭크별 단조 증가성 및 유효성 PBT 속성 테스트. */
class BattleServiceMeditationPropertyTest {

    private static final Long CHARACTER_ID = 1L;

    private CharacterSkillRepository characterSkillRepository;
    private SkillService skillService;

    @BeforeProperty
    void setUp() {
        characterSkillRepository = mock(CharacterSkillRepository.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final SkillCatalogService skillCatalogService = new SkillCatalogService(new ObjectMapper());
        final InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("data/skill.json");
        skillCatalogService.loadFromStream(inputStream);
        skillCatalogService.init();

        skillService =
                new SkillService(
                        characterSkillRepository, characterProgressRepository, skillCatalogService);
    }

    @Provide
    Arbitrary<SkillRank> ranks() {
        return Arbitraries.of(SkillRank.values());
    }

    @Property
    void meditationRegen_shouldBePositive_andMonotonicallyNonDecreasing(
            @net.jqwik.api.ForAll("ranks") final SkillRank rank) {
        // given
        final CharacterSkill charSkill = new CharacterSkill(CHARACTER_ID, "meditation", rank, 0, 0);
        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "meditation"))
                .thenReturn(Optional.of(charSkill));

        // when
        final int regen = skillService.meditationTurnRegen(CHARACTER_ID);

        // then
        assertThat(regen).isPositive();

        // Check monotonicity with next rank if exists
        final Optional<SkillRank> nextOpt = rank.next();
        if (nextOpt.isPresent()) {
            final CharacterSkill nextSkill =
                    new CharacterSkill(CHARACTER_ID, "meditation", nextOpt.get(), 0, 0);
            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "meditation"))
                    .thenReturn(Optional.of(nextSkill));
            final int nextRegen = skillService.meditationTurnRegen(CHARACTER_ID);
            assertThat(nextRegen).isGreaterThanOrEqualTo(regen);
        }
    }
}
