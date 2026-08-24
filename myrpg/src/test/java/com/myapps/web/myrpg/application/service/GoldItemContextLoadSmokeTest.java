package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 골드·아이템·인벤토리·장비 시스템 컨텍스트 로드 스모크 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트를 기동하여 006 스펙 핵심 빈 ({@link ItemCatalogService}, {@link InventoryService},
 * {@link BankService})이 정상 로딩되는지, 카탈로그 11종 로드, 장비 보너스 경로, 상단바 골드 미표시를 검증한다.
 *
 * <p>Validates: Requirements 5.1, 10.3, 20.5
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GoldItemContextLoadSmokeTest {

    private static final int EXPECTED_ITEM_COUNT = 57;

    private final ItemCatalogService itemCatalogService;
    private final InventoryService inventoryService;
    private final BankService bankService;

    GoldItemContextLoadSmokeTest(
            final ItemCatalogService itemCatalogService,
            final InventoryService inventoryService,
            final BankService bankService) {
        this.itemCatalogService = itemCatalogService;
        this.inventoryService = inventoryService;
        this.bankService = bankService;
    }

    /** 애플리케이션 컨텍스트가 정상 기동되고 골드·아이템 핵심 빈이 로딩되는지 검증한다. */
    @Test
    void should_loadApplicationContext_withGoldItemBeans() {
        assertThat(itemCatalogService).isNotNull();
        assertThat(inventoryService).isNotNull();
        assertThat(bankService).isNotNull();
    }

    /** ItemCatalogService가 기동 시 11종 아이템을 로드하는지 검증한다. */
    @Test
    void should_loadAllItems_onStartup() {
        assertThat(itemCatalogService.all()).hasSize(EXPECTED_ITEM_COUNT);
    }

    /**
     * 정보 팝업 장비 보너스 경로가 정상 작동하는지 검증한다.
     *
     * <p>{@link InventoryService#equippedBonus()}가 유효한 결과를 반환하고 statBonus/vitalBonus가 모두 null이 아닌지
     * 확인한다.
     */
    @Test
    void should_returnValidEquippedBonusResult() {
        final EquippedBonusResult result = inventoryService.equippedBonus();

        assertThat(result).isNotNull();
        assertThat(result.statBonus()).isNotNull();
        assertThat(result.vitalBonus()).isNotNull();
    }

    /**
     * 인벤토리 팝업 뷰 조립이 정상 작동하는지 검증한다.
     *
     * <p>골드 0으로 인벤토리 뷰를 빌드하여 예외 없이 결과가 반환되는지 확인한다.
     */
    @Test
    void should_buildInventoryView_withoutException() {
        final var view = inventoryService.buildInventoryView(0L);

        assertThat(view).isNotNull();
        assertThat(view.gold()).isEqualTo(0L);
        assertThat(view.items()).isNotNull();
    }

    /**
     * 은행 팝업 뷰 조립이 정상 작동하는지 검증한다.
     *
     * <p>골드 0으로 은행 뷰를 빌드하여 예외 없이 결과가 반환되는지 확인한다.
     */
    @Test
    void should_buildBankView_withoutException() {
        final var view = inventoryService.buildBankView(0L, 0L);

        assertThat(view).isNotNull();
        assertThat(view.playerGold()).isEqualTo(0L);
        assertThat(view.bankGold()).isEqualTo(0L);
        assertThat(view.bankItems()).isNotNull();
        assertThat(view.inventoryItems()).isNotNull();
    }

    /**
     * 상단바(top-bar.html)에 골드가 표시되지 않음을 확인한다.
     *
     * <p>설계에 따르면 골드 표시는 인벤토리·은행 팝업에서만 수행되며 상단바는 변경되지 않는다(Requirements 20.5). 이 테스트는 top-bar 관련 뷰
     * 모델에 골드 필드가 존재하지 않음을 구조적으로 검증한다.
     *
     * <p>TopBarView가 gold 필드를 갖지 않는 것으로 설계되었으므로 컴파일 시점에서 보장되지만, 런타임 확인으로 문서화한다.
     */
    @Test
    void should_notExposeGoldInTopBar() {
        // TopBarView record에 gold() 메서드가 존재하지 않음을 리플렉션으로 확인.
        // gold 메서드가 없다면 상단바에 골드가 노출될 수 없다.
        final boolean hasGoldMethod =
                java.util.Arrays.stream(
                                com.myapps.web.myrpg.application.dto.TopBarView.class
                                        .getDeclaredMethods())
                        .anyMatch(method -> "gold".equals(method.getName()));

        assertThat(hasGoldMethod).as("TopBarView에 gold() 메서드가 없어야 한다 (상단바에 골드 미표시)").isFalse();
    }
}
