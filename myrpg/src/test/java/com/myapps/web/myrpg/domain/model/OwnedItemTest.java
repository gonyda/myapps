package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OwnedItem 도메인 모델의 수리(repairBy) 동작을 검증하는 단위 테스트.
 *
 * <p>1포인트 수리 시 내구도 정수/소수점 증가, max 상한 캡핑을 확인한다.
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3</b>
 */
class OwnedItemTest {

    private static final String TEST_ITEM_ID = "test_equipment";
    private static final double MAX_DURABILITY = 20.0;

    /**
     * repairBy(1.0, max)로 내구도가 정확히 +1 복원됨을 검증한다.
     */
    @Test
    void should_increaseDurabilityByOne_when_repair() {
        final OwnedItem item = createEquipmentWithDurability(12.0);

        item.repairBy(1.0, MAX_DURABILITY);

        assertThat(item.getCurrentDurability()).isEqualTo(13.0);
    }

    /**
     * 소수점 내구도에 repairBy(1.0, max)를 적용하면 실제 double에 +1이 적용됨을 검증한다.
     * (12.4 → 13.4, 올림/내림 없음)
     */
    @Test
    void should_addOneToFractionalDurability_when_repair() {
        final OwnedItem item = createEquipmentWithDurability(12.4);

        item.repairBy(1.0, MAX_DURABILITY);

        assertThat(item.getCurrentDurability()).isEqualTo(13.4);
    }

    /**
     * repairBy 후 내구도가 internally max를 초과하지 않고 max로 상한 캡핑됨을 검증한다.
     */
    @Test
    void should_capAtMax_when_repairExceedsMax() {
        final OwnedItem item = createEquipmentWithDurability(19.8);

        item.repairBy(1.0, MAX_DURABILITY);

        assertThat(item.getCurrentDurability()).isEqualTo(MAX_DURABILITY);
    }

    /**
     * 테스트용 장비 인스턴스를 생성한다.
     *
     * @param durability 초기 현재 내구도
     * @return 설정된 내구도를 가진 OwnedItem
     */
    private OwnedItem createEquipmentWithDurability(final double durability) {
        return new OwnedItem(
                TEST_ITEM_ID,
                1,
                StorageKind.INVENTORY,
                false,
                durability
        );
    }
}