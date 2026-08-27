package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 전투 화면에서 스킬 슬롯 버튼으로 표시할 정보를 담는 불변 레코드.
 *
 * <p>0~9번 10개 슬롯을 표현하며, 착용 무기 재능과 일치하는 스킬만 활성화(enabled=true)된다.
 *
 * @param id 스킬 카탈로그 ID (빈 슬롯 시 null)
 * @param label 스킬 표시명 (예: "윈드밀", "파이널 히트")
 * @param type 스킬 공격/방어 유형 (NORMAL/HEAVY/DEFENSE/ULTIMATE 등)
 * @param resourceKind 소모 자원 종류 (STAMINA 또는 MP)
 * @param resourceCost 소모 자원량
 * @param ultimateCooldown 궁극기 잔여 쿨다운 (0이면 사용 가능)
 * @param ready 궁극기 사용 준비 완료 여부 (ULTIMATE 타입이며 쿨다운 0)
 * @param slotIndex 0~9 슬롯 번호
 * @param enabled 현재 착용 무기와 부합하여 활성화되었는지 여부
 * @param disabledReason 비활성 사유 (예: "활 착용 필요", "스태미나 부족" 등)
 * @param empty 빈 슬롯 여부
 * @param icon 스킬 아이콘
 */
public record BattleSkillButton(
        String id,
        String label,
        SkillType type,
        ResourceKind resourceKind,
        int resourceCost,
        int ultimateCooldown,
        boolean ready,
        int slotIndex,
        boolean enabled,
        String disabledReason,
        boolean empty,
        String icon) {

    /** 하위 호환 생성자 (8인자): slotIndex, enabled, disabledReason, empty, icon 기본값 적용. */
    public BattleSkillButton(
            final String id,
            final String label,
            final SkillType type,
            final ResourceKind resourceKind,
            final int resourceCost,
            final int ultimateCooldown,
            final boolean ready) {
        this(
                id,
                label,
                type,
                resourceKind,
                resourceCost,
                ultimateCooldown,
                ready,
                0,
                true,
                null,
                false,
                "⚔️");
    }

    /** 하위 호환 생성자: ultimateCooldown=0, ready=false. */
    public BattleSkillButton(
            final String id,
            final String label,
            final SkillType type,
            final ResourceKind resourceKind,
            final int resourceCost) {
        this(id, label, type, resourceKind, resourceCost, 0, type == SkillType.ULTIMATE);
    }

    /** 빈 슬롯 팩토리 메서드. */
    public static BattleSkillButton emptySlot(final int slotIndex) {
        return new BattleSkillButton(
                null,
                "빈 슬롯",
                SkillType.NORMAL,
                ResourceKind.STAMINA,
                0,
                0,
                false,
                slotIndex,
                false,
                "스킬 미등록",
                true,
                "➕");
    }

    /** UI 렌더링용 CSS 클래스 문자열을 생성한다. */
    public String cssClass() {
        if (empty) {
            return "slot-empty";
        }
        if (!enabled) {
            return "slot-disabled";
        }
        final String typeClass =
                "skill-type-" + (type != null ? type.name().toLowerCase() : "normal");
        if (type == SkillType.ULTIMATE) {
            return typeClass + (ultimateCooldown > 0 ? " ultimate-cooldown" : " ultimate-ready");
        }
        return typeClass;
    }
}
