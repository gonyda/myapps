package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ItemType;
import java.util.List;

/**
 * 인벤토리·은행 목록의 한 행에 대응하는 뷰 모델.
 *
 * <p>이름·타입 라벨·수량·장착 여부·사용/착용 가능 여부·내구도·상세 텍스트를 담으며, {@code detailLines}는 렌더 시점에 임베드되어 🔍 클릭 시 별도 서버
 * 요청 없이 표시된다.
 *
 * @param ownedItemId 보유 아이템 엔티티 PK
 * @param name 아이템 표시명
 * @param typeLabel 아이템 타입의 한글 라벨 (예: "포션", "무기", "방어구")
 * @param type 아이템 타입 enum
 * @param quantity 보유 수량 (소비형 스택, 장비는 1)
 * @param equipped 장착 여부
 * @param usable 사용 가능 여부 (포션이면 true)
 * @param equippable 착용 가능 여부 (장비이면 true)
 * @param currentDurability 현재 내구도 (장비만, 포션이면 null)
 * @param maxDurability 최대 내구도 (장비만, 포션이면 null)
 * @param detailLines 상세 설명 줄 목록 (임베드용)
 */
public record OwnedItemView(
        long ownedItemId,
        String name,
        String typeLabel,
        ItemType type,
        int quantity,
        boolean equipped,
        boolean usable,
        boolean equippable,
        Double currentDurability,
        Integer maxDurability,
        List<String> detailLines) {}
