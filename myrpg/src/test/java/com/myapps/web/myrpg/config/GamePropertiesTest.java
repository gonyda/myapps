package com.myapps.web.myrpg.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GamePropertiesTest {

    private final GameProperties gameProperties;

    @Autowired
    GamePropertiesTest(final GameProperties gameProperties) {
        this.gameProperties = gameProperties;
    }

    @Test
    @DisplayName("application-game.yml 설정값이 GameProperties record에 정상적으로 바인딩된다")
    void should_bindPropertiesCorrectly_fromYaml() {
        assertThat(gameProperties).isNotNull();

        // 1. Gathering
        assertThat(gameProperties.gathering()).isNotNull();
        assertThat(gameProperties.gathering().woodcutSpawnRate()).isEqualTo(50);
        assertThat(gameProperties.gathering().woodcutSuccessRate()).isEqualTo(50);
        assertThat(gameProperties.gathering().woodcutStaminaCost()).isEqualTo(5);

        // 2. Inventory
        assertThat(gameProperties.inventory()).isNotNull();
        assertThat(gameProperties.inventory().maxSlots()).isEqualTo(30);
        assertThat(gameProperties.inventory().defaultPotionQty()).isEqualTo(5);
        assertThat(gameProperties.inventory().equipmentMaxDurability()).isEqualTo(20);

        // 3. Town
        assertThat(gameProperties.town()).isNotNull();
        assertThat(gameProperties.town().healCost()).isEqualTo(100);
        assertThat(gameProperties.town().repairSuccessRate()).isEqualTo(95);
        assertThat(gameProperties.town().repairAmount()).isEqualTo(1);

        // 4. Battle
        assertThat(gameProperties.battle()).isNotNull();
        assertThat(gameProperties.battle().fleeSuccessRate()).isEqualTo(50);
        assertThat(gameProperties.battle().ambushRate()).isEqualTo(5);
        assertThat(gameProperties.battle().magicFailRate()).isEqualTo(10);
        assertThat(gameProperties.battle().durabilityPerAttack()).isEqualTo(0.05);
        assertThat(gameProperties.battle().meleeCoef()).isEqualTo(1.0);
        assertThat(gameProperties.battle().archeryCoef()).isEqualTo(0.85);
        assertThat(gameProperties.battle().magicCoef()).isEqualTo(1.2);
        assertThat(gameProperties.battle().criticalMultiplier()).isEqualTo(1.5);
        assertThat(gameProperties.battle().monsterNormalMultiplier()).isEqualTo(100);
        assertThat(gameProperties.battle().monsterHeavyMultiplier()).isEqualTo(150);
        assertThat(gameProperties.battle().aiNormalWeight()).isEqualTo(34);
        assertThat(gameProperties.battle().aiHeavyWeight()).isEqualTo(33);
        assertThat(gameProperties.battle().aiDefenseWeight()).isEqualTo(33);

        // 5. Progression
        assertThat(gameProperties.progression()).isNotNull();
        assertThat(gameProperties.progression().maxLevel()).isEqualTo(100);
        assertThat(gameProperties.progression().deathPenaltyRate()).isEqualTo(0.10);

        // 6. Movement
        assertThat(gameProperties.movement()).isNotNull();
        assertThat(gameProperties.movement().worldMoveMinutes()).isEqualTo(15);
        assertThat(gameProperties.movement().dungeonMoveMinutes()).isEqualTo(5);
    }
}
