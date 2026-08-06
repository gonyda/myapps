package com.myapps.web.myrpg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.StatProgression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 캐릭터 진행 컨텍스트 로드 및 정보 팝업 렌더링 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트 기동이 성공하고,
 * 신규 빈({@link StatProgression}, {@link ProgressionService})이 정상 로딩되며,
 * 정보 팝업 렌더링 경로({@code GET /})가 정상 동작하는지 검증한다.
 * 고정 게임 데이터를 저장하지 않고 캐릭터 진행상황만 저장하는 원칙(001)이
 * 유지됨을 컨텍스트 로드 자체로 증명한다.
 *
 * <p>Validates: Requirements 11.3
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:progression-smoke-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ProgressionContextLoadSmokeTest {

    private final StatProgression statProgression;
    private final ProgressionService progressionService;
    private final MockMvc mockMvc;

    ProgressionContextLoadSmokeTest(final StatProgression statProgression,
                                    final ProgressionService progressionService,
                                    final MockMvc mockMvc) {
        this.statProgression = statProgression;
        this.progressionService = progressionService;
        this.mockMvc = mockMvc;
    }

    /**
     * StatProgression 빈이 컨텍스트에 정상 등록되었는지 검증한다.
     *
     * <p>순수 정책 빈이 {@code DomainServiceConfiguration}을 통해
     * 정상적으로 로딩되었음을 확인한다.
     */
    @Test
    void should_loadStatProgressionBean_when_contextStarts() {
        assertThat(statProgression).isNotNull();
    }

    /**
     * ProgressionService 빈이 컨텍스트에 정상 등록되었는지 검증한다.
     *
     * <p>{@code @Service} 어노테이션으로 등록된 빈이
     * 의존성(ExperiencePolicy, StatProgression, Clock)과 함께
     * 정상 로딩되었음을 확인한다.
     */
    @Test
    void should_loadProgressionServiceBean_when_contextStarts() {
        assertThat(progressionService).isNotNull();
    }

    /**
     * GET / 요청이 200 OK를 반환하고 정보 팝업 마커를 포함하는지 검증한다.
     *
     * <p>플레이 화면 렌더링 경로가 정상 동작하며,
     * 정보 팝업의 상/중/하 구역이 포함된 HTML을 반환하는지 확인한다.
     */
    @Test
    void should_renderInfoPopupContent_when_getRoot() throws Exception {
        final String content = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("infoOverlay");
        assertThat(content).contains("infoContent");
        assertThat(content).contains("info-top");
        assertThat(content).contains("info-middle");
        assertThat(content).contains("info-bottom");
        assertThat(content).contains("rebirth-btn");
    }
}
