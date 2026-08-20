package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Stats;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 스킬 시스템 컨텍스트 로드 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트를 기동하여 스킬 관련 핵심 빈 ({@link SkillCatalogService}, {@link SkillService})이 정상
 * 로딩되는지, 정보 팝업의 스킬 보너스 경로가 정상 작동하는지, 스킬 팝업 뷰 조립이 정상인지 검증한다.
 *
 * <p>Validates: Requirements 1.1, 8.5
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:skill-smoke-test;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=create"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SkillContextLoadSmokeTest {

    private static final int EXPECTED_SKILL_COUNT = 11;
    private static final List<String> DEFAULT_SEED_SKILL_IDS =
            List.of("slash", "aimed_shot", "mana_bolt", "defense");

    private final SkillCatalogService skillCatalogService;
    private final SkillService skillService;
    private final CharacterService characterService;

    SkillContextLoadSmokeTest(
            final SkillCatalogService skillCatalogService,
            final SkillService skillService,
            final CharacterService characterService) {
        this.skillCatalogService = skillCatalogService;
        this.skillService = skillService;
        this.characterService = characterService;
    }

    /** 애플리케이션 컨텍스트가 정상 기동되고 스킬 빈이 로딩되는지 검증한다. */
    @Test
    void should_loadApplicationContext_withSkillBeans() {
        assertThat(skillCatalogService).isNotNull();
        assertThat(skillService).isNotNull();
    }

    /** SkillCatalogService가 기동 시 11종 스킬을 로드하는지 검증한다. */
    @Test
    void should_loadElevenSkills_onStartup() {
        assertThat(skillCatalogService.all()).hasSize(EXPECTED_SKILL_COUNT);
    }

    /**
     * 신규 캐릭터(기본 스킬 4종 F 시드)의 스킬 보너스가 Stats.ZERO인지 검증한다.
     *
     * <p>시드 스킬은 모두 F(order 0)이라 보너스가 0이므로, 정보 팝업의 스킬 보너스 경로가 Stats.ZERO를 반환해야 한다(Requirements 8.5).
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
     * <p>기본 스킬 4종이 표시되어야 하며 예외 없이 뷰가 조립된다.
     */
    @Test
    void should_buildSkillListView_forFreshCharacter() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final SkillListView listView = skillService.buildListView(progress.getId(), "all");

        assertThat(listView).isNotNull();
        assertThat(listView.rows()).hasSize(DEFAULT_SEED_SKILL_IDS.size());
        assertThat(listView.rows())
                .extracting(row -> row.id())
                .containsExactlyInAnyOrderElementsOf(DEFAULT_SEED_SKILL_IDS);
    }

    /**
     * 신규 캐릭터의 승급 모달 뷰가 정상 조립되는지 검증한다.
     *
     * <p>기본 시드 스킬 {@code slash}(베기)에 대해 F→E 승급 뷰가 조립된다.
     */
    @Test
    void should_buildRankUpView_forFreshCharacterSlash() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final SkillRankUpView rankUpView = skillService.buildRankUpView(progress.getId(), "slash");

        assertThat(rankUpView).isNotNull();
        assertThat(rankUpView.id()).isEqualTo("slash");
        assertThat(rankUpView.currentRankLabel()).isEqualTo("F");
        assertThat(rankUpView.nextRankLabel()).isEqualTo("E");
        assertThat(rankUpView.maxed()).isFalse();
    }
}
