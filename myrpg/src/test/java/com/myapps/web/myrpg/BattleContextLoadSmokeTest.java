package com.myapps.web.myrpg;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 전투 시스템 컨텍스트 로드 스모크 테스트.
 *
 * <p>전체 애플리케이션 컨텍스트가 전투 관련 빈({@link BattleService}, {@link BattleResolver}, {@link
 * BattleStateRepository})을 포함하여 정상 기동되는지 검증한다.
 *
 * <p>Validates: Requirements 18.1, 24.5
 */
@SpringBootTest
class BattleContextLoadSmokeTest {

    @Autowired private BattleService battleService;

    @Autowired private BattleResolver battleResolver;

    @Autowired private BattleStateRepository battleStateRepository;

    /** 전투 시스템 핵심 빈이 컨텍스트에 정상 등록되는지 검증한다. */
    @Test
    void should_loadBattleBeans_when_contextStarts() {
        assertThat(battleService).isNotNull();
        assertThat(battleResolver).isNotNull();
        assertThat(battleStateRepository).isNotNull();
    }
}
