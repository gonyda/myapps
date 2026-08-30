package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.service.RockPaperScissors;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 전투 한 턴의 활동 로그 문자열을 생성하는 순수 포매터.
 *
 * <p>가위바위보 9칸 매트릭스의 결과({@link BattleLogInput})를 받아 플레이어 행동 로그와 몬스터 행동 로그를 표준 형식으로 산출한다. '크리티컬' 단어를
 * 일체 사용하지 않고 '💥' 이모지로 통일하며, [아이콘/주체] [스킬/행동] 구조를 준수한다.
 */
@Component
public class BattleLogFormatter {

    private static final String HIT_SEPARATOR = " · ";
    private static final String CRITICAL_SUFFIX = "💥";
    private static final int MULTI_HIT_THRESHOLD = 2;

    private final GameMessageService msg;

    /**
     * BattleLogFormatter를 생성한다 (Spring 주입용).
     *
     * @param msg 메시지 리졸버 서비스
     */
    @org.springframework.beans.factory.annotation.Autowired
    public BattleLogFormatter(final GameMessageService msg) {
        this.msg = msg;
    }

    /** 이전 호환용 기본 생성자 (메시지 서비스 부재 시 기본 리졸버 연동). */
    public BattleLogFormatter() {
        this(null);
    }

    private String getMsg(final String code, final Object... args) {
        if (msg != null) {
            return msg.get(code, args);
        }
        // Fallback formatting when msg is null
        return switch (code) {
            case "battle.first_strike.monster_ambush" ->
                    "⚠️ [적 선제공격] [" + args[0] + "] 기습 ➔ " + args[1] + " 피해 피격";
            case "battle.attack.ultimate_multi" ->
                    "👑 [결전 궁극기] ["
                            + args[0]
                            + "] "
                            + args[1]
                            + "연타 ("
                            + args[2]
                            + ") ➔ 총 "
                            + args[3]
                            + " 관통 피해";
            case "battle.attack.ultimate_single" ->
                    "👑 [결전 궁극기] [" + args[0] + "]" + args[1] + " 100% 관통 ➔ " + args[2] + " 피해";
            case "battle.first_strike.hold" -> "⚡ [선제 공격] 선제 찬스였으나 [" + args[0] + "] 태세 유지";
            case "battle.attack.multi" ->
                    args[0] + " [" + args[1] + "] " + args[2] + "연타 (" + args[3] + ") ➔ 총 "
                            + args[4] + " 피해";
            case "battle.attack.single" ->
                    args[0] + " [" + args[1] + "]" + args[2] + " ➔ " + args[3] + "에게 " + args[4]
                            + " 피해";
            case "battle.turn.block_perfect" ->
                    "⚔️ [" + args[0] + "] 🛡️ " + args[1] + "의 완전 방어에 가로막힘 (0 피해)";
            case "battle.turn.miss" -> args[0] + " [" + args[1] + "] 빗나감 (0 피해)";
            case "battle.turn.vs_defense_multi" ->
                    "⚔️ ["
                            + args[0]
                            + "] "
                            + args[1]
                            + "연타 ("
                            + args[2]
                            + ") 🛡️ 적 방어에 막힘 ➔ 총 "
                            + args[3]
                            + " 피해";
            case "battle.turn.vs_defense_single" ->
                    "⚔️ [" + args[0] + "]" + args[1] + " 🛡️ 적 방어에 막힘 ➔ " + args[2] + " 피해";
            case "battle.turn.counter_crit" ->
                    "⚡ [" + args[0] + "] 💥 적 공격을 흘려내며 ➔ " + args[1] + " 치명 반격!";
            case "battle.turn.defense_success" ->
                    "🛡️ [" + args[0] + "] 방어 성공 & 반격! ➔ " + args[1] + "에게 " + args[2] + " 반격 피해";
            case "battle.turn.defense_window" ->
                    "🛡️ [" + args[0] + "] 완벽 방어! ➔ 빈틈 포착 (다음 턴 선제 찬스⚡)";
            case "battle.turn.defense_penetrated" -> "⚠️ [" + args[0] + "] 몬스터 강공격에 방어선 관통!";
            case "battle.turn.counter_miss" -> "⚡ [" + args[0] + "] 적이 공격하지 않아 빗나감";
            case "battle.turn.defense_stalemate" -> "🛡️ [" + args[0] + "] 맞방어 교착 상태";
            case "battle.turn.defense_full_block" -> "🛡️ [" + args[0] + "] 적의 공격을 완벽히 막아냄 (0 피해)";
            case "battle.monster.miss" -> "[" + args[0] + "] " + args[1] + " ➔ 빗나감";
            case "battle.monster.blocked" ->
                    "[" + args[0] + "] " + args[1] + " ➔ 🛡️ 방어로 경감되어 " + args[2] + " 피해";
            case "battle.monster.hit" ->
                    "[" + args[0] + "] " + args[1] + " ➔ " + args[2] + " 피해 피격";
            case "battle.monster.defense_hold" -> "[" + args[0] + "] 🛡️ 방어 태세 유지";
            case "battle.monster.defense_counter" ->
                    "[" + args[0] + "] 🛡️ 방어 성공 & 반격 ➔ " + args[1] + " 피해 피격";
            case "battle.monster.defense_alert" ->
                    "[" + args[0] + "] 🛡️ 공격 방어 성공 ➔ 반격 태세 (다음 턴 선제 주의⚠️)";
            case "battle.monster.defense_break" -> "[" + args[0] + "] 💥 방어선 관통됨!";
            case "battle.monster.defense_full" -> "[" + args[0] + "] 🛡️ 완전 방어 (0 피해)";
            default -> code;
        };
    }

    /**
     * 한 턴의 전투 로그 줄을 생성한다.
     *
     * @param input 턴 결과 로그 입력
     * @return 활동 로그에 추가할 문자열 목록
     */
    public List<String> combatLines(final BattleLogInput input) {
        final List<String> lines = new ArrayList<>();
        if (input.castFailure()) {
            addMonsterLine(lines, input);
            return lines;
        }
        if (input.firstStrike()) {
            addFirstStrikeLine(lines, input);
            return lines;
        }
        addPlayerLine(lines, input);
        addMonsterLine(lines, input);
        return lines;
    }

    /** 선제 공격 로그를 추가한다. */
    private void addFirstStrikeLine(final List<String> lines, final BattleLogInput input) {
        if (input.playerDamage() <= 0 && input.monsterDamage() > 0) {
            lines.add(
                    getMsg(
                            "battle.first_strike.monster_ambush",
                            input.monsterName(),
                            input.monsterDamage()));
            return;
        }

        if (input.playerType() == SkillType.ULTIMATE) {
            if (isMultiHit(input)) {
                lines.add(
                        getMsg(
                                "battle.attack.ultimate_multi",
                                input.skillLabel(),
                                input.playerHits().size(),
                                formatHits(input.playerHits()),
                                input.playerDamage()));
            } else {
                final String critTag = input.playerCritical() ? " 💥" : "";
                lines.add(
                        getMsg(
                                "battle.attack.ultimate_single",
                                input.skillLabel(),
                                critTag,
                                input.playerDamage()));
            }
            return;
        }

        if (input.playerDamage() <= 0) {
            lines.add(getMsg("battle.first_strike.hold", input.skillLabel()));
            return;
        }

        if (isMultiHit(input)) {
            lines.add(
                    getMsg(
                            "battle.attack.multi",
                            "⚡ [선제 공격]",
                            input.skillLabel(),
                            input.playerHits().size(),
                            formatHits(input.playerHits()),
                            input.playerDamage()));
        } else {
            final String critTag = input.playerCritical() ? " 💥" : "";
            lines.add(
                    getMsg(
                            "battle.attack.single",
                            "⚡ [선제 공격]",
                            input.skillLabel(),
                            critTag,
                            input.monsterName(),
                            input.playerDamage()));
        }
    }

    /** 플레이어 행동 로그를 추가한다. */
    private void addPlayerLine(final List<String> lines, final BattleLogInput input) {
        if (input.playerType() == SkillType.DEFENSE) {
            addPlayerDefenseLine(lines, input);
            return;
        }
        if (input.playerDamage() <= 0) {
            if (input.monsterAction() == SkillType.DEFENSE) {
                lines.add(
                        getMsg(
                                "battle.turn.block_perfect",
                                input.skillLabel(),
                                input.monsterName()));
            } else {
                lines.add(getMsg("battle.turn.miss", "🗡️", input.skillLabel()));
            }
            return;
        }
        if (input.monsterAction() == SkillType.DEFENSE
                && RockPaperScissors.isNormalFamily(input.playerType())) {
            if (isMultiHit(input)) {
                lines.add(
                        getMsg(
                                "battle.turn.vs_defense_multi",
                                input.skillLabel(),
                                input.playerHits().size(),
                                formatHits(input.playerHits()),
                                input.playerDamage()));
            } else {
                final String critTag = input.playerCritical() ? " 💥" : "";
                lines.add(
                        getMsg(
                                "battle.turn.vs_defense_single",
                                input.skillLabel(),
                                critTag,
                                input.playerDamage()));
            }
            return;
        }
        if (isMultiHit(input)) {
            addMultiHitLine(lines, input);
        } else {
            addSingleHitLine(lines, input);
        }
    }

    /** 멀티히트(hitCount ≥ 2) 로그 1줄을 추가한다. */
    private void addMultiHitLine(final List<String> lines, final BattleLogInput input) {
        lines.add(
                getMsg(
                        "battle.attack.multi",
                        "⚔️",
                        input.skillLabel(),
                        input.playerHits().size(),
                        formatHits(input.playerHits()),
                        input.playerDamage()));
    }

    /** 단일 히트 플레이어 공격 로그 1줄을 추가한다. */
    private void addSingleHitLine(final List<String> lines, final BattleLogInput input) {
        final String critTag = input.playerCritical() ? " 💥" : "";
        lines.add(
                getMsg(
                        "battle.attack.single",
                        "🗡️",
                        input.skillLabel(),
                        critTag,
                        input.monsterName(),
                        input.playerDamage()));
    }

    /** 플레이어가 방어 스킬을 사용했을 때의 로그를 추가한다. */
    private void addPlayerDefenseLine(final List<String> lines, final BattleLogInput input) {
        final boolean isCounter = input.skillLabel() != null && input.skillLabel().contains("카운터");
        if (input.playerDamage() > 0) {
            if (isCounter) {
                lines.add(
                        getMsg(
                                "battle.turn.counter_crit",
                                input.skillLabel(),
                                input.playerDamage()));
            } else {
                lines.add(
                        getMsg(
                                "battle.turn.defense_success",
                                input.skillLabel(),
                                input.monsterName(),
                                input.playerDamage()));
            }
        } else if (input.monsterAction() == SkillType.NORMAL) {
            lines.add(getMsg("battle.turn.defense_window", input.skillLabel()));
        } else if (input.monsterDamage() > 0) {
            lines.add(getMsg("battle.turn.defense_penetrated", input.skillLabel()));
        } else {
            if (isCounter) {
                lines.add(getMsg("battle.turn.counter_miss", input.skillLabel()));
            } else if (input.monsterAction() == SkillType.DEFENSE) {
                lines.add(getMsg("battle.turn.defense_stalemate", input.skillLabel()));
            } else {
                lines.add(getMsg("battle.turn.defense_full_block", input.skillLabel()));
            }
        }
    }

    /** 몬스터 행동 로그 한 줄을 추가한다. */
    private void addMonsterLine(final List<String> lines, final BattleLogInput input) {
        if (input.monsterAction() == SkillType.DEFENSE) {
            addMonsterDefenseLine(lines, input);
            return;
        }
        final String actionLabel = input.monsterAction() == SkillType.HEAVY ? "강공격" : "일반공격";
        final boolean isCounter = input.skillLabel() != null && input.skillLabel().contains("카운터");
        if (input.playerType() == SkillType.DEFENSE && input.monsterAction() == SkillType.NORMAL) {
            if (isCounter) {
                lines.add(getMsg("battle.monster.miss", input.monsterName(), actionLabel));
            } else {
                lines.add(
                        getMsg(
                                "battle.monster.blocked",
                                input.monsterName(),
                                actionLabel,
                                input.monsterDamage()));
            }
        } else if (input.monsterDamage() > 0) {
            lines.add(
                    getMsg(
                            "battle.monster.hit",
                            input.monsterName(),
                            actionLabel,
                            input.monsterDamage()));
        } else {
            lines.add(getMsg("battle.monster.miss", input.monsterName(), actionLabel));
        }
    }

    /** 몬스터가 방어했을 때의 로그 한 줄을 추가한다. */
    private void addMonsterDefenseLine(final List<String> lines, final BattleLogInput input) {
        if (input.castFailure() || input.playerType() == SkillType.DEFENSE) {
            lines.add(getMsg("battle.monster.defense_hold", input.monsterName()));
        } else if (input.monsterDamage() > 0) {
            lines.add(
                    getMsg(
                            "battle.monster.defense_counter",
                            input.monsterName(),
                            input.monsterDamage()));
        } else if (RockPaperScissors.isNormalFamily(input.playerType())) {
            lines.add(getMsg("battle.monster.defense_alert", input.monsterName()));
        } else if (input.playerDamage() > 0) {
            lines.add(getMsg("battle.monster.defense_break", input.monsterName()));
        } else {
            lines.add(getMsg("battle.monster.defense_full", input.monsterName()));
        }
    }

    /** 멀티히트 여부를 판정한다. */
    private boolean isMultiHit(final BattleLogInput input) {
        return input.playerHits() != null && input.playerHits().size() >= MULTI_HIT_THRESHOLD;
    }

    /** 각 히트별 피해 및 크리티컬 여부를 포맷팅한다. */
    private String formatHits(final List<HitResult> hits) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            if (i > 0) {
                builder.append(HIT_SEPARATOR);
            }
            final HitResult hit = hits.get(i);
            builder.append(hit.damage());
            if (hit.critical()) {
                builder.append(CRITICAL_SUFFIX);
            }
        }
        return builder.toString();
    }
}
