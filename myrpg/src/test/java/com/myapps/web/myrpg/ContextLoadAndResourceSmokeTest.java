package com.myapps.web.myrpg;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.domain.model.MapGraph;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 컨텍스트 로드 및 리소스 로딩 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트 기동이 성공하고, {@link MapService}가 클래스패스 리소스 ({@code map.json})를 정상 로딩했는지
 * 검증합니다.
 *
 * <p>Validates: Requirements 3.5, 10.3
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ContextLoadAndResourceSmokeTest {

    private final MapService mapService;

    ContextLoadAndResourceSmokeTest(final MapService mapService) {
        this.mapService = mapService;
    }

    /** MapService 빈이 로드되고, 맵 그래프가 유효한 노드·시작 노드·던전을 포함하는지 검증한다. */
    @Test
    void should_haveValidMapGraph_when_mapServiceLoaded() {
        final MapGraph graph = mapService.graph();

        assertThat(graph).isNotNull();
        assertThat(graph.nodes()).isNotEmpty();
        assertThat(graph.startNodeId()).isEqualTo("tir-chonaill");
        assertThat(graph.byId(graph.startNodeId())).isPresent();
        assertThat(graph.dungeons()).isNotEmpty();
    }
}
