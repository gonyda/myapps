package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;

/** 턴 전투 중 발생하는 피해 적용 및 턴 지속 효과(마나실드, DoT, 명상)를 처리하는 협력 객체. */
public final class TurnEffectApplier {

    private static final int PERCENT_DIVISOR = 100;

    private final SkillService skillService;
    private final StatProgression statProgression;
    private final GameMessageService gameMessageService;

    public TurnEffectApplier(
            final SkillService skillService,
            final StatProgression statProgression,
            final GameMessageService gameMessageService) {
        this.skillService = skillService;
        this.statProgression = statProgression;
        this.gameMessageService = gameMessageService;
    }

    /** 마나 실드 흡수 및 플레이어·몬스터 HP 피해를 적용한다. */
    public void applyManaShieldAndDamage(
            final CharacterProgress progress,
            final BattleState state,
            final int playerDamage,
            final int rawMonsterDamage,
            final List<String> combatLines) {
        int monsterDamage = rawMonsterDamage;
        if (monsterDamage > 0 && state.getManaShieldTurnsLeft() > 0) {
            final int absorbRate = state.getManaShieldAbsorbRate();
            final int toAbsorb = (monsterDamage * absorbRate) / PERCENT_DIVISOR;
            final int availableMp = progress.getMpCurrent();
            final int actualAbsorbed = Math.min(toAbsorb, availableMp);
            if (actualAbsorbed > 0) {
                progress.spendMp(actualAbsorbed);
                monsterDamage -= actualAbsorbed;
                combatLines.add(
                        "✨ [마나 실드] "
                                + actualAbsorbed
                                + " 피해를 MP로 흡수 (남은 MP: "
                                + progress.getMpCurrent()
                                + ")");
            }
            state.setManaShieldTurnsLeft(state.getManaShieldTurnsLeft() - 1);
        }

        if (monsterDamage > 0) {
            progress.damageHp(monsterDamage);
        }
        if (playerDamage > 0) {
            final int newHp = Math.max(0, state.getMonsterCurrentHp() - playerDamage);
            state.setMonsterCurrentHp(newHp);
        }
    }

    /** DoT(지속 피해) 틱 데미지를 몬스터에게 적용한다. */
    public void applyDoTTick(
            final BattleState state, final Monster monster, final List<String> combatLines) {
        if (state.getDotTurnsLeft() > 0 && state.getMonsterCurrentHp() > 0) {
            final int dotDmg = state.getDotDamagePerTurn();
            state.setMonsterCurrentHp(Math.max(0, state.getMonsterCurrentHp() - dotDmg));
            state.setDotTurnsLeft(state.getDotTurnsLeft() - 1);
            final String dotMsg = gameMessageService.get("battle.dot", monster.name(), dotDmg);
            combatLines.add(dotMsg);
        }
    }

    /** 명상 패시브에 의한 턴당 MP 재생을 적용한다. */
    public void applyMeditationRegen(
            final CharacterProgress progress, final List<String> combatLines) {
        if (!progress.isDead()) {
            final int regen = skillService.meditationTurnRegen(progress.getId());
            if (regen > 0) {
                final VitalMax baseVital =
                        statProgression.vitalMaxFor(
                                progress.getCurrentLevel(), progress.getTalent());
                final VitalMax skillVital = skillService.rankupVitalBonus(progress.getId());
                final int maxMp = baseVital.mp() + skillVital.mp();
                final int beforeMp = progress.getMpCurrent();
                progress.recoverMp(regen, maxMp);
                final int actualRegened = progress.getMpCurrent() - beforeMp;
                if (actualRegened > 0) {
                    final String medMsg =
                            gameMessageService.get("battle.meditation", actualRegened);
                    combatLines.add(medMsg);
                }
            }
        }
    }
}
