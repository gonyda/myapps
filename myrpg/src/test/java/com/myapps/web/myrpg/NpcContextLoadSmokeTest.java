package com.myapps.web.myrpg;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.interfaces.api.HealController;
import com.myapps.web.myrpg.interfaces.api.RepairController;
import com.myapps.web.myrpg.interfaces.api.ShopController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * NPC 컨텍스트 로드 및 리소스 로딩 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트 기동이 성공하고, {@link NpcService}가 클래스패스 리소스({@code npc.json})를 정상 로딩했는지 검증합니다.
 * 또한 NPC 상점/수리/치료 관련 컨트롤러 및 서비스 빈이 정상 등록되었는지 스모크 검증합니다.
 *
 * <p>Validates: Requirements 1.6, 2.4, 2.5, 5.1, 15.4
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NpcContextLoadSmokeTest {

    private final NpcService npcService;
    private final ShopService shopService;
    private final ShopController shopController;
    private final RepairController repairController;
    private final HealController healController;

    NpcContextLoadSmokeTest(
            final NpcService npcService,
            final ShopService shopService,
            final ShopController shopController,
            final RepairController repairController,
            final HealController healController) {
        this.npcService = npcService;
        this.shopService = shopService;
        this.shopController = shopController;
        this.repairController = repairController;
        this.healController = healController;
    }

    /**
     * NpcService 빈이 로드되고, NPC 목록이 비어 있지 않은지 검증한다.
     *
     * <p>컨텍스트 기동 시 {@code npc.json}이 정상 파싱되어 최소 1개 이상의 NPC가 로드됨을 확인합니다.
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
     * <p>{@code NpcService}는 순수 클래스패스 리소스 기반으로 동작하며, ID 기반 조회가 정상적으로 수행됨을 확인합니다. 만약 NpcService가 DB
     * Repository에 의존했다면 컨텍스트 로드 자체가 실패합니다.
     */
    @Test
    void should_resolveNpcById_when_npcServiceHasNoDatabaseDependency() {
        final List<Npc> allNpcs = npcService.all();
        final Npc firstNpc = allNpcs.getFirst();

        assertThat(npcService.byId(firstNpc.id())).isPresent();
        assertThat(npcService.byId(firstNpc.id()).get()).isEqualTo(firstNpc);
    }

    /** NPC 상점 아이템이 올바르게 로드되는지 검증한다 (Req 010). */
    @Test
    void should_loadShopItems_when_npcConfiguredWithShopItems() {
        final Npc ferghus = npcService.byId("ferghus").orElseThrow();
        assertThat(ferghus.shopItems())
                .containsExactly(
                        "wooden_blade",
                        "dagger",
                        "short_sword",
                        "short_bow",
                        "round_shield",
                        "cuirassier_helm",
                        "light_leather_mail",
                        "studded_cuirassier",
                        "leather_gloves",
                        "cores_thief_gloves",
                        "lorica_gloves",
                        "leather_shoes",
                        "hunter_boots",
                        "combat_shoes",
                        "thief_shoes");

        final Npc lassar = npcService.byId("lassar").orElseThrow();
        assertThat(lassar.shopItems()).containsExactly("fire_wand", "ice_wand", "lightning_wand");

        final Npc neris = npcService.byId("neris").orElseThrow();
        assertThat(neris.shopItems())
                .containsExactly(
                        "long_sword",
                        "broadsword",
                        "bastard_sword",
                        "battle_sword",
                        "long_bow",
                        "composite_bow",
                        "crossbow",
                        "kite_shield",
                        "ring_mail_helm",
                        "cross_full_helm",
                        "wing_half_helm",
                        "bone_helm",
                        "spiked_helm",
                        "drandos_leather_mail",
                        "three_belt_leather_mail",
                        "melka_chain_mail",
                        "surcoat_chain_mail",
                        "counter_gauntlet",
                        "tork_hunter_gloves",
                        "ulna_protector_gloves",
                        "long_greaves",
                        "leather_protector");

        final Npc stewart = npcService.byId("stewart").orElseThrow();
        assertThat(stewart.shopItems())
                .containsExactly(
                        "phoenix_fire_wand",
                        "crown_ice_wand",
                        "crystal_lightning_wand",
                        "trinity_staff");

        final Npc dilys = npcService.byId("dilys").orElseThrow();
        assertThat(dilys.shopItems())
                .containsExactly("hp_potion_30", "mp_potion_30", "stamina_potion_30");
    }

    /** 상점, 수리, 치료 관련 빈이 정상 로드되는지 검증한다 (Req 010 스모크). */
    @Test
    void should_loadActionBeans_when_contextInitialized() {
        assertThat(shopService).isNotNull();
        assertThat(shopController).isNotNull();
        assertThat(repairController).isNotNull();
        assertThat(healController).isNotNull();
    }
}
