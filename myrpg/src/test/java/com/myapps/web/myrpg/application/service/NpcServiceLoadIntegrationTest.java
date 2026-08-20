package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Npc;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 실제 {@code data/npc.json} 로딩 통합 테스트.
 *
 * <p>Spring Boot 컨텍스트 전체를 기동하여 {@link NpcService}가 클래스패스 리소스를 정상 로드하는지 검증합니다.
 *
 * <p>Validates: Requirements 1.3
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NpcServiceLoadIntegrationTest {

    private static final int TOTAL_NPC_COUNT = 10;
    private static final int TIR_CHONAILL_NPC_COUNT = 5;
    private static final int DUNBARTON_NPC_COUNT = 5;

    private final NpcService npcService;

    NpcServiceLoadIntegrationTest(final NpcService npcService) {
        this.npcService = npcService;
    }

    /** 전체 NPC 수가 10명인지 검증한다. */
    @Test
    void should_loadTenNpcs_when_applicationStarts() {
        final List<Npc> allNpcs = npcService.all();

        assertThat(allNpcs).hasSize(TOTAL_NPC_COUNT);
    }

    /** 티르코네일 노드에 5명의 NPC가 배치되어 있는지 검증한다. */
    @Test
    void should_haveFiveNpcsInTirChonaill_when_filteredByNode() {
        final List<Npc> tirChonaill = npcService.byNode("tir-chonaill");

        assertThat(tirChonaill).hasSize(TIR_CHONAILL_NPC_COUNT);
    }

    /** 던바튼 노드에 5명의 NPC가 배치되어 있는지 검증한다. */
    @Test
    void should_haveFiveNpcsInDunbarton_when_filteredByNode() {
        final List<Npc> dunbarton = npcService.byNode("dunbarton");

        assertThat(dunbarton).hasSize(DUNBARTON_NPC_COUNT);
    }

    /** 모든 NPC의 type이 유효한 {@code NpcType} enum 값으로 분류되었는지 검증한다. */
    @Test
    void should_haveValidNpcType_forAllNpcs() {
        final List<Npc> allNpcs = npcService.all();

        assertThat(allNpcs).allSatisfy(npc -> assertThat(npc.type()).isNotNull());
    }
}
