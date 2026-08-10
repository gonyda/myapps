package com.myapps.web.myrpg.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 몬스터 서비스 5종의 Spring 빈 로딩 및 컨텍스트 기동을 검증하는 스모크 테스트.
 *
 * <p>MonsterService, MonsterDialogueService, MonsterAiService,
 * MonsterRewardService, MonsterEncounterService가 정상적으로 빈으로 등록되고
 * 카탈로그 로드까지 완료되는지 확인합니다.
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MonsterContextLoadSmokeTest {

    private final MonsterService monsterService;
    private final MonsterDialogueService monsterDialogueService;
    private final MonsterAiService monsterAiService;
    private final MonsterRewardService monsterRewardService;
    private final MonsterEncounterService monsterEncounterService;

    MonsterContextLoadSmokeTest(final MonsterService monsterService,
                                final MonsterDialogueService monsterDialogueService,
                                final MonsterAiService monsterAiService,
                                final MonsterRewardService monsterRewardService,
                                final MonsterEncounterService monsterEncounterService) {
        this.monsterService = monsterService;
        this.monsterDialogueService = monsterDialogueService;
        this.monsterAiService = monsterAiService;
        this.monsterRewardService = monsterRewardService;
        this.monsterEncounterService = monsterEncounterService;
    }

    /**
     * 몬스터 관련 모든 서비스 빈이 정상적으로 로딩되는지 검증한다.
     */
    @Test
    void should_loadAllMonsterServiceBeans_when_contextStarts() {
        assertThat(monsterService).isNotNull();
        assertThat(monsterDialogueService).isNotNull();
        assertThat(monsterAiService).isNotNull();
        assertThat(monsterRewardService).isNotNull();
        assertThat(monsterEncounterService).isNotNull();
    }

    /**
     * 컨텍스트 기동 후 몬스터 카탈로그가 비어 있지 않은지 검증한다.
     */
    @Test
    void should_haveLoadedMonsterCatalog_when_contextStarts() {
        assertThat(monsterService.all()).isNotEmpty();
    }
}
