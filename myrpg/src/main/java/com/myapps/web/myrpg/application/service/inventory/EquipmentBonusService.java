package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusKind;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;
import java.util.Optional;

/** 장착 장비의 스탯/바이탈 보너스 합산 및 내구도 감소/파손 시 자동 장착 해제를 담당하는 협력 객체. */
public final class EquipmentBonusService {

    private static final String LOG_TYPE_ITEM = "item";

    private final InventoryQuerySupport query;
    private final ActionLog actionLog;
    private final GameMessageService gameMessageService;

    /**
     * EquipmentBonusService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param actionLog 활동 로그 컴포넌트
     * @param gameMessageService 게임 메시지 다국어 서비스
     */
    public EquipmentBonusService(
            final InventoryQuerySupport query,
            final ActionLog actionLog,
            final GameMessageService gameMessageService) {
        this.query = query;
        this.actionLog = actionLog;
        this.gameMessageService = gameMessageService;
    }

    /**
     * 기본 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus() {
        return equippedBonus(null);
    }

    /**
     * 특정 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * <p>인벤토리({@code storage=INVENTORY})에서 장착 중인({@code equipped=true}) 장비의 {@link EquipBonus} 목록을
     * 순회하여 {@link BonusTarget#kind()}로 분기하여 STAT 계열은 {@link Stats}로, VITAL 계열은 {@link VitalMax}로
     * 합산한다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 전체/기본)
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus(final Long characterId) {
        final List<OwnedItem> equippedItems = query.findEquippedInventoryItems(characterId);

        Stats statBonus = Stats.ZERO;
        VitalMax vitalBonus = new VitalMax(0, 0, 0);

        for (final OwnedItem owned : equippedItems) {
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(owned);
            if (equipOpt.isEmpty()) {
                continue;
            }
            final EquipmentItem equipItem = equipOpt.get();
            for (final EquipBonus bonus : equipItem.bonuses()) {
                if (bonus.target().kind() == BonusKind.STAT) {
                    statBonus = applyStatDelta(statBonus, bonus.target(), bonus.amount());
                } else {
                    vitalBonus = applyVitalDelta(vitalBonus, bonus.target(), bonus.amount());
                }
            }
        }

        return new EquippedBonusResult(statBonus, vitalBonus);
    }

    /**
     * 장착 장비의 내구도를 감소시키고, 0에 도달하면 자동 장착 해제한다.
     *
     * <p>전투에서 공격 턴당 호출되며, 내구도가 0에 도달한 장비는 자동으로 장착 해제되어 보너스가 소멸된다. 자동 해제 시 활동 로그에 해제 메시지를 남긴다.
     *
     * @param progress 캐릭터 진행상황 (로그 기록용)
     * @param amount 감소시킬 내구도량 (양수)
     */
    public void reduceDurabilityAndAutoUnequip(
            final CharacterProgress progress, final double amount) {
        final Long charId = progress != null ? progress.getId() : null;
        final List<OwnedItem> equippedItems = query.findEquippedInventoryItems(charId);

        for (final OwnedItem equipped : equippedItems) {
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(equipped);
            if (equipOpt.isEmpty()) {
                continue;
            }
            equipped.reduceDurability(amount);
            if (equipped.getCurrentDurability() <= 0.0) {
                autoUnequipBroken(equipped, equipOpt.get().name());
            }
        }
    }

    /**
     * 스탯 대상별로 변경량을 적용한다.
     *
     * @param stats 기존 스탯
     * @param target 변경 대상
     * @param amount 변경 수치
     * @return 변경된 스탯
     */
    public Stats applyStatDelta(final Stats stats, final BonusTarget target, final int amount) {
        return switch (target) {
            case STR -> stats.withStrDelta(amount);
            case DEX -> stats.withDexDelta(amount);
            case INT -> stats.withIntDelta(amount);
            case CRITICAL -> stats.withCriticalDelta(amount);
            case DEF -> stats.withDefenseDelta(amount);
            default -> stats;
        };
    }

    /**
     * 바이탈 대상별로 변경량을 적용한다.
     *
     * @param vitalMax 기존 바이탈
     * @param target 변경 대상
     * @param amount 변경 수치
     * @return 변경된 바이탈
     */
    public VitalMax applyVitalDelta(
            final VitalMax vitalMax, final BonusTarget target, final int amount) {
        return switch (target) {
            case HP -> vitalMax.withHpDelta(amount);
            case MP -> vitalMax.withMpDelta(amount);
            case STAMINA -> vitalMax.withStaminaDelta(amount);
            default -> vitalMax;
        };
    }

    private void autoUnequipBroken(final OwnedItem equipped, final String itemName) {
        equipped.unequip();
        final String brokenMsg = gameMessageService.get("log.item.durability_broken", itemName);
        actionLog.add(brokenMsg, LOG_TYPE_ITEM);
    }
}
