package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillEffectRowView;
import com.myapps.web.myrpg.domain.model.SkillRank;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SkillDomainModelCoverageTest {

    private SkillCatalogService skillCatalogService;

    @BeforeEach
    void setUp() {
        skillCatalogService = new SkillCatalogService(new ObjectMapper());
        skillCatalogService.init();
    }

    @Test
    void test_allSkills_effectRowsAt_for_every_rank() {
        final List<Skill> allSkills = skillCatalogService.all();
        assertThat(allSkills).isNotEmpty();

        for (final Skill skill : allSkills) {
            assertThat(skill.id()).isNotBlank();
            assertThat(skill.label()).isNotBlank();
            assertThat(skill.talent()).isNotNull();
            assertThat(skill.type()).isNotNull();

            for (final SkillRank rank : SkillRank.values()) {
                final SkillRank nextRank = rank.next().orElse(null);
                final List<SkillEffectRowView> rows = skill.effectRowsAt(rank, nextRank);
                assertThat(rows).isNotNull();
            }
        }
    }
}
