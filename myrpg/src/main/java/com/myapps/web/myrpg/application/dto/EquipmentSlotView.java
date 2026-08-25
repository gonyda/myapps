package com.myapps.web.myrpg.application.dto;

/**
 * 장비 팝업의 개별 슬롯 렌더링을 위한 뷰 DTO.
 *
 * @param slotId 슬롯 식별자 (예: "HEAD", "BODY", "MAIN_HAND", "OFF_HAND", "HANDS", "FEET", "ACC1",
 *     "ACC2", "ROBE")
 * @param slotLabel 슬롯 표시 이름 (예: "머리", "갑옷", "주무기", "보조손", "손", "발", "악세사리 1", "악세사리 2", "로브")
 * @param silhouetteIcon 빈 슬롯 음각 아이콘 (예: "🪖", "🥋", "🗡️", "🛡️", "🧤", "👢", "💍", "🧥")
 * @param equipped 장착 여부
 * @param locked 미구현 잠금 여부 (악세사리, 로브)
 * @param blockedByTwoHanded 양손무기 착용으로 인한 점유 비활성화 여부
 * @param ownedItemId 장착된 아이템의 DB PK (미장착 시 null)
 * @param itemName 장착된 아이템명 (미장착 시 null)
 * @param itemIcon 장착된 아이템 아이콘 (미장착 시 null)
 * @param itemType 장착된 아이템 타입명 (미장착 시 null)
 * @param currentDurability 현재 내구도 (미장착 시 null)
 * @param maxDurability 최대 내구도 (미장착 시 null)
 * @param durabilityPercent 내구도 백분율 0~100 (미장착 시 0)
 * @param durabilityStatus 내구도 상태 ("normal", "warning", "danger")
 * @param bonusesSummary 스탯 보너스 요약 텍스트 (예: "STR +5, DEF +2")
 * @param detailText 아이템 상세 설명 문구
 */
public record EquipmentSlotView(
        String slotId,
        String slotLabel,
        String silhouetteIcon,
        boolean equipped,
        boolean locked,
        boolean blockedByTwoHanded,
        Long ownedItemId,
        String itemName,
        String itemIcon,
        String itemType,
        Integer currentDurability,
        Integer maxDurability,
        int durabilityPercent,
        String durabilityStatus,
        String bonusesSummary,
        String detailText) {

    /** 잠긴 슬롯(악세사리 1/2, 로브) 뷰를 생성한다. */
    public static EquipmentSlotView locked(
            final String slotId, final String slotLabel, final String silhouetteIcon) {
        return new EquipmentSlotView(
                slotId,
                slotLabel,
                silhouetteIcon,
                false,
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                "normal",
                null,
                "향후 업데이트 예정인 슬롯입니다.");
    }

    /** 빈 슬롯 뷰를 생성한다. */
    public static EquipmentSlotView empty(
            final String slotId, final String slotLabel, final String silhouetteIcon) {
        return new EquipmentSlotView(
                slotId,
                slotLabel,
                silhouetteIcon,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                "normal",
                null,
                null);
    }

    /** 양손무기로 인해 점유된 보조손 슬롯 뷰를 생성한다. */
    public static EquipmentSlotView blockedByTwoHanded(
            final String slotId, final String slotLabel, final String silhouetteIcon) {
        return new EquipmentSlotView(
                slotId,
                slotLabel,
                silhouetteIcon,
                false,
                false,
                true,
                null,
                "양손무기 점유",
                "⛔",
                null,
                null,
                null,
                0,
                "normal",
                null,
                "양손 무기 착용 중에는 보조손을 사용할 수 없습니다.");
    }
}
