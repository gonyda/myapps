package com.myapps.web.myrpg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code application-game.yml}에 정의된 게임 밸런스 설정 프로퍼티를 바인딩하는 불변 Record.
 *
 * <p>채집, 인벤토리, 마을 편의, 전투, 성장, 이동 6대 도메인 그룹으로 구성됩니다.
 */
@ConfigurationProperties(prefix = "game")
public record GameProperties(
        GatheringProperties gathering,
        InventoryProperties inventory,
        TownProperties town,
        BattleProperties battle,
        ProgressionProperties progression,
        MovementProperties movement) {

    public GameProperties {
        gathering = gathering != null ? gathering : new GatheringProperties(50, 50, 5);
        inventory = inventory != null ? inventory : new InventoryProperties(30, 5, 20);
        town = town != null ? town : new TownProperties(100, 95, 1);
        battle =
                battle != null
                        ? battle
                        : new BattleProperties(
                                50, 5, 10, 0.05, 1.0, 0.85, 1.2, 1.5, 100, 150, 34, 33, 33);
        progression = progression != null ? progression : new ProgressionProperties(100, 0.10);
        movement = movement != null ? movement : new MovementProperties(15, 5);
    }

    public record GatheringProperties(
            int woodcutSpawnRate, int woodcutSuccessRate, int woodcutStaminaCost) {}

    public record InventoryProperties(
            int maxSlots, int defaultPotionQty, int equipmentMaxDurability) {}

    public record TownProperties(int healCost, int repairSuccessRate, int repairAmount) {}

    public record BattleProperties(
            int fleeSuccessRate,
            int ambushRate,
            int magicFailRate,
            double durabilityPerAttack,
            double meleeCoef,
            double archeryCoef,
            double magicCoef,
            double criticalMultiplier,
            int monsterNormalMultiplier,
            int monsterHeavyMultiplier,
            int aiNormalWeight,
            int aiHeavyWeight,
            int aiDefenseWeight) {}

    public record ProgressionProperties(int maxLevel, double deathPenaltyRate) {}

    public record MovementProperties(int worldMoveMinutes, int dungeonMoveMinutes) {}
}
