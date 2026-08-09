package com.myapps.web.myrpg.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Stats;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스킬 시스템 컨텍스트 로드 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트를 기동하여 스킬 관련 핵심 빈
 * ({@link SkillCatalogService}, {@link SkillService})이 정상 로딩되는지,
 * 정보 팝업의 스킬 보너스 경로가 정상 작동하는지,
 * 스킬 팝업 뷰 조립이 정상인지 검증한다.
 *
 * <p>Validates: Requirements 1.1, 8.5
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SkillContextLoadSmokeTest {

    private static final int EXPECTED_SKILL_COUNT = 7;

    private final SkillCatalogService skillCatalogService;
    private final SkillService skillService;
    private final CharacterService characterService;

    SkillContextLoadSmokeTest(final SkillCatalogService skillCatalogService,
                              final SkillService skillService,
                              final CharacterService characterService) {
        this.skillCatalogService = skillCatalogService;
        this.skillService = skillService;
        this.characterService = characterService;
    }

    /**
     * 애플리케이션 컨텍스트가 정상 기동되고 스킬 빈이 로딩되는지 검증한다.
     */
    @Test
    void should_loadApplicationContext_withSkillBeans() {
        assertThat(skillCatalogService).isNotNull();
        assertThat(skillService).isNotNull();
    }

    /**
     * SkillCatalogService가 기동 시 7종 스킬을 로드하는지 검증한다.
     */
    @Test
    void should_loadSevenSkills_onStartup() {
        assertThat(skillCatalogService.all()).hasSize(EXPECTED_SKILL_COUNT);
    }

    /**
     * 신규 캐릭터(windmill F 시드)의 스킬 보너스가 Stats.ZERO인지 검증한다.
     *
     * <p>windmill F(order 0) = 보너스 0이므로 정보 팝업의 스킬 보너스 경로가
     * Stats.ZERO를 반환해야 한다(Requirements 8.5).
     */
    @Test
    void should_returnZeroBonus_forFreshCharacter() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final Stats bonus = skillService.rankupBonus(progress.getId());

        assertThat(bonus).isEqualTo(Stats.ZERO);
    }

    /**
     * 신규 캐릭터의 스킬 목록 팝업(전체 탭)이 정상 렌더되는지 검증한다.
     *
     * <p>windmill 1개가 표시되어야 하며 예외 없이 뷰가 조립된다.
     */
    @Test
    void should_buildSkillListView_forFreshCharacter() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final var listView = skillService.buildListView(progress.getId(), "all");

        assertThat(listView).isNotNull();
        assertThat(listView.rows()).hasSize(1);
        assertThat(listView.rows().getFirst().id()).isEqualTo("windmill");
    }

    /**
     * 신규 캐릭터의 승급 모달 뷰가 정상 조립되는지 검증한다.
     */
    @Test
    void should_buildRankUpView_forFreshCharacterWindmill() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final var rankUpView = skillService.buildRankUpView(progress.getId(), "windmill");

        assertThat(rankUpView).isNotNull();
        assertThat(rankUpView.id()).isEqualTo("windmill");
        assertThat(rankUpView.currentRankLabel()).isEqualTo("F");
        assertThat(rankUpView.nextRankLabel()).isEqualTo("E");
        assertThat(rankUpView.maxed()).isFalse();
    }
}
