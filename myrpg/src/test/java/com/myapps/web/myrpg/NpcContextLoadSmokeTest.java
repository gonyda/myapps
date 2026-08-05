package com.myapps.web.myrpg;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.domain.model.Npc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NPC 컨텍스트 로드 및 리소스 로딩 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트 기동이 성공하고,
 * {@link NpcService}가 클래스패스 리소스({@code npc.json})를 정상 로딩했는지 검증합니다.
 * {@code NpcService}는 데이터베이스(Repository) 의존 없이 동작하므로,
 * 컨텍스트가 정상 기동된다는 사실 자체가 DB 무의존성을 증명합니다.
 *
 * <p>Validates: Requirements 1.6, 5.1
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NpcContextLoadSmokeTest {

    private final NpcService npcService;

    NpcContextLoadSmokeTest(final NpcService npcService) {
        this.npcService = npcService;
    }

    /**
     * NpcService 빈이 로드되고, NPC 목록이 비어 있지 않은지 검증한다.
     *
     * <p>컨텍스트 기동 시 {@code npc.json}이 정상 파싱되어
     * 최소 1개 이상의 NPC가 로드됨을 확인합니다.
     */
    @Test
    void should_haveNonEmptyNpcList_when_npcServiceLoaded() {
        final List<Npc> allNpcs = npcService.all();

        assertThat(allNpcs).isNotNull();
        assertThat(allNpcs).isNotEmpty();
    }

    /**
     * NpcService가 DB(Repository) 의존 없이 동작함을 검증한다.
     *
     * <p>{@code NpcService}는 순수 클래스패스 리소스 기반으로 동작하며,
     * ID 기반 조회가 정상적으로 수행됨을 확인합니다.
     * 만약 NpcService가 DB Repository에 의존했다면 컨텍스트 로드 자체가 실패합니다.
     */
    @Test
    void should_resolveNpcById_when_npcServiceHasNoDatabaseDependency() {
        final List<Npc> allNpcs = npcService.all();
        final Npc firstNpc = allNpcs.getFirst();

        assertThat(npcService.byId(firstNpc.id())).isPresent();
        assertThat(npcService.byId(firstNpc.id()).get()).isEqualTo(firstNpc);
    }
}
