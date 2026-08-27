package com.myapps.web.myrpg.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;
import org.junit.jupiter.api.Test;

class BattleSkillButtonTest {

    @Test
    void should_create_emptySlot_with_proper_defaults() {
        final BattleSkillButton button = BattleSkillButton.emptySlot(3);

        assertThat(button.slotIndex()).isEqualTo(3);
        assertThat(button.empty()).isTrue();
        assertThat(button.enabled()).isFalse();
        assertThat(button.label()).isEqualTo("빈 슬롯");
        assertThat(button.icon()).isEqualTo("➕");
        assertThat(button.cssClass()).contains("slot-empty");
    }

    @Test
    void should_return_proper_css_classes_based_on_skill_state() {
        // Disabled skill
        final BattleSkillButton disabledBtn =
                new BattleSkillButton(
                        "smash",
                        "스매시",
                        SkillType.HEAVY,
                        ResourceKind.STAMINA,
                        10,
                        0,
                        true,
                        0,
                        false,
                        "무기 불일치",
                        false,
                        "⚔️");
        assertThat(disabledBtn.cssClass()).isEqualTo("slot-disabled");

        // Ultimate ready skill
        final BattleSkillButton readyUltBtn =
                new BattleSkillButton(
                        "mana_burst",
                        "마나버스트",
                        SkillType.ULTIMATE,
                        ResourceKind.MP,
                        50,
                        0,
                        true,
                        1,
                        true,
                        null,
                        false,
                        "🪄");
        assertThat(readyUltBtn.cssClass()).contains("ultimate-ready");

        // Ultimate charging skill
        final BattleSkillButton chargingUltBtn =
                new BattleSkillButton(
                        "mana_burst",
                        "마나버스트",
                        SkillType.ULTIMATE,
                        ResourceKind.MP,
                        50,
                        5,
                        false,
                        2,
                        true,
                        null,
                        false,
                        "🪄");
        assertThat(chargingUltBtn.cssClass()).contains("ultimate-cooldown");

        // Normal active skill
        final BattleSkillButton normalBtn =
                new BattleSkillButton(
                        "smash",
                        "스매시",
                        SkillType.HEAVY,
                        ResourceKind.STAMINA,
                        10,
                        0,
                        true,
                        0,
                        true,
                        null,
                        false,
                        "⚔️");
        assertThat(normalBtn.cssClass()).isEqualTo("skill-type-heavy");
    }
}
