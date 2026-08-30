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
        if (msg != null) {
            this.msg = msg;
        } else {
            final org.springframework.context.support.ResourceBundleMessageSource source =
                    new org.springframework.context.support.ResourceBundleMessageSource();
            source.setBasename("messages");
            source.setDefaultEncoding("UTF-8");
            this.msg = new GameMessageService(source);
        }
    }

    /** 이전 호환용 기본 생성자 (메시지 서비스 부재 시 기본 리졸버 연동). */
    public BattleLogFormatter() {
        this(null);
    }

    private String getMsg(final String code, final Object... args) {
        return msg.get(code, args);
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
