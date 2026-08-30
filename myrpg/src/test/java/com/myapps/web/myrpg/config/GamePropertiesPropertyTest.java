package com.myapps.web.myrpg.config;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class GamePropertiesPropertyTest {

    @Provide
    Arbitrary<GameProperties> validGameProperties() {
        final Arbitrary<GameProperties.GatheringProperties> gathering =
                Combinators.combine(
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.integers().between(1, 100))
                        .as(GameProperties.GatheringProperties::new);

        final Arbitrary<GameProperties.InventoryProperties> inventory =
                Combinators.combine(
                                Arbitraries.integers().between(1, 100),
                                Arbitraries.integers().between(1, 99),
                                Arbitraries.integers().between(1, 100))
                        .as(GameProperties.InventoryProperties::new);

        final Arbitrary<GameProperties.TownProperties> town =
                Combinators.combine(
                                Arbitraries.integers().between(1, 10000),
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.integers().between(1, 100))
                        .as(GameProperties.TownProperties::new);

        final Arbitrary<GameProperties.BattleProperties> battle =
                Combinators.combine(
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.integers().between(0, 100),
                                Arbitraries.doubles().between(0.01, 1.0),
                                Arbitraries.doubles().between(0.1, 5.0),
                                Arbitraries.doubles().between(0.1, 5.0),
                                Arbitraries.doubles().between(0.1, 5.0),
                                Arbitraries.doubles().between(1.0, 5.0))
                        .as(
                                (flee, ambush, magicFail, dura, melee, arch, magic, crit) ->
                                        new GameProperties.BattleProperties(
                                                flee, ambush, magicFail, dura, melee, arch, magic,
                                                crit, 100, 150, 34, 33, 33));

        final Arbitrary<GameProperties.ProgressionProperties> progression =
                Combinators.combine(
                                Arbitraries.integers().between(1, 200),
                                Arbitraries.doubles().between(0.0, 1.0))
                        .as(GameProperties.ProgressionProperties::new);

        final Arbitrary<GameProperties.MovementProperties> movement =
                Combinators.combine(
                                Arbitraries.integers().between(1, 60),
                                Arbitraries.integers().between(1, 60))
                        .as(GameProperties.MovementProperties::new);

        return Combinators.combine(gathering, inventory, town, battle, progression, movement)
                .as(GameProperties::new);
    }

    @Property(tries = 100)
    void should_satisfyValueRangeInvariants_forAnyValidProperties(
            @ForAll("validGameProperties") final GameProperties properties) {
        // 1. 확률값 범위 불변식 (0 <= rate <= 100)
        assertThat(properties.gathering().woodcutSpawnRate()).isBetween(0, 100);
        assertThat(properties.gathering().woodcutSuccessRate()).isBetween(0, 100);
        assertThat(properties.town().repairSuccessRate()).isBetween(0, 100);
        assertThat(properties.battle().fleeSuccessRate()).isBetween(0, 100);
        assertThat(properties.battle().ambushRate()).isBetween(0, 100);
        assertThat(properties.battle().magicFailRate()).isBetween(0, 100);

        // 2. 수량/비용 양수 불변식 (value > 0)
        assertThat(properties.gathering().woodcutStaminaCost()).isGreaterThan(0);
        assertThat(properties.inventory().maxSlots()).isGreaterThan(0);
        assertThat(properties.inventory().defaultPotionQty()).isGreaterThan(0);
        assertThat(properties.inventory().equipmentMaxDurability()).isGreaterThan(0);
        assertThat(properties.town().healCost()).isGreaterThan(0);
        assertThat(properties.town().repairAmount()).isGreaterThan(0);
        assertThat(properties.progression().maxLevel()).isGreaterThan(0);
        assertThat(properties.movement().worldMoveMinutes()).isGreaterThan(0);
        assertThat(properties.movement().dungeonMoveMinutes()).isGreaterThan(0);

        // 3. 전투 계수 범위 불변식 (0.0 < coef <= 5.0)
        assertThat(properties.battle().meleeCoef()).isBetween(0.0, 5.0);
        assertThat(properties.battle().archeryCoef()).isBetween(0.0, 5.0);
        assertThat(properties.battle().magicCoef()).isBetween(0.0, 5.0);
        assertThat(properties.battle().criticalMultiplier()).isBetween(1.0, 5.0);
    }
}
