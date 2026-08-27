package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.service.RockPaperScissors;
import java.util.ArrayList;
import java.util.List;

/**
 * 전투 한 턴의 활동 로그 문자열을 생성하는 순수 포매터.
 *
 * <p>가위바위보 9칸 매트릭스의 결과({@link BattleLogInput})를 받아 플레이어 행동 로그와 몬스터 행동 로그를 표준 형식으로 산출한다. '크리티컬' 단어를
 * 일체 사용하지 않고 '💥' 이모지로 통일하며, [아이콘/주체] [스킬/행동] 구조를 준수한다.
 */
public class BattleLogFormatter {

    private static final String HIT_SEPARATOR = " · ";
    private static final String CRITICAL_SUFFIX = "💥";
    private static final String ARROW = " ➔ ";
    private static final int MULTI_HIT_THRESHOLD = 2;

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
                    "⚠️ [적 선제공격] ["
                            + input.monsterName()
                            + "] 기습"
                            + ARROW
                            + input.monsterDamage()
                            + " 피해 피격");
            return;
        }

        final String skillTag = "[" + input.skillLabel() + "]";
        if (input.playerType() == SkillType.ULTIMATE) {
            if (isMultiHit(input)) {
                lines.add(
                        "👑 [결전 궁극기] "
                                + skillTag
                                + " "
                                + input.playerHits().size()
                                + "연타 ("
                                + formatHits(input.playerHits())
                                + ")"
                                + ARROW
                                + "총 "
                                + input.playerDamage()
                                + " 관통 피해");
            } else {
                final String critTag = input.playerCritical() ? " 💥" : "";
                lines.add(
                        "👑 [결전 궁극기] "
                                + skillTag
                                + critTag
                                + " 100% 관통"
                                + ARROW
                                + input.playerDamage()
                                + " 피해");
            }
            return;
        }

        if (input.playerDamage() <= 0) {
            lines.add("⚡ [선제 공격] 선제 찬스였으나 " + skillTag + " 태세 유지");
            return;
        }

        if (isMultiHit(input)) {
            lines.add(
                    "⚡ [선제 공격] "
                            + skillTag
                            + " "
                            + input.playerHits().size()
                            + "연타 ("
                            + formatHits(input.playerHits())
                            + ")"
                            + ARROW
                            + "총 "
                            + input.playerDamage()
                            + " 피해");
        } else {
            final String critTag = input.playerCritical() ? " 💥" : "";
            lines.add(
                    "⚡ [선제 공격] "
                            + skillTag
                            + critTag
                            + ARROW
                            + input.monsterName()
                            + "에게 "
                            + input.playerDamage()
                            + " 피해");
        }
    }

    /** 플레이어 행동 로그를 추가한다. */
    private void addPlayerLine(final List<String> lines, final BattleLogInput input) {
        final String skillTag = "[" + input.skillLabel() + "]";
        if (input.playerType() == SkillType.DEFENSE) {
            addPlayerDefenseLine(lines, input, skillTag);
            return;
        }
        if (input.playerDamage() <= 0) {
            if (input.monsterAction() == SkillType.DEFENSE) {
                lines.add(
                        "⚔️ " + skillTag + " 🛡️ " + input.monsterName() + "의 완전 방어에 가로막힘 (0 피해)");
            } else {
                lines.add("🗡️ " + skillTag + " 빗나감 (0 피해)");
            }
            return;
        }
        if (input.monsterAction() == SkillType.DEFENSE
                && RockPaperScissors.isNormalFamily(input.playerType())) {
            if (isMultiHit(input)) {
                lines.add(
                        "⚔️ "
                                + skillTag
                                + " "
                                + input.playerHits().size()
                                + "연타 ("
                                + formatHits(input.playerHits())
                                + ") 🛡️ 적 방어에 막힘"
                                + ARROW
                                + "총 "
                                + input.playerDamage()
                                + " 피해");
            } else {
                final String critTag = input.playerCritical() ? " 💥" : "";
                lines.add(
                        "⚔️ "
                                + skillTag
                                + critTag
                                + " 🛡️ 적 방어에 막힘"
                                + ARROW
                                + input.playerDamage()
                                + " 피해");
            }
            return;
        }
        if (isMultiHit(input)) {
            addMultiHitLine(lines, input, skillTag);
        } else {
            addSingleHitLine(lines, input, skillTag);
        }
    }

    /** 멀티히트(hitCount ≥ 2) 로그 1줄을 추가한다. */
    private void addMultiHitLine(
            final List<String> lines, final BattleLogInput input, final String skillTag) {
        lines.add(
                "⚔️ "
                        + skillTag
                        + " "
                        + input.playerHits().size()
                        + "연타 ("
                        + formatHits(input.playerHits())
                        + ")"
                        + ARROW
                        + "총 "
                        + input.playerDamage()
                        + " 피해");
    }

    /** 단일 히트 플레이어 공격 로그 1줄을 추가한다. */
    private void addSingleHitLine(
            final List<String> lines, final BattleLogInput input, final String skillTag) {
        final String critTag = input.playerCritical() ? " 💥" : "";
        lines.add(
                "🗡️ "
                        + skillTag
                        + critTag
                        + ARROW
                        + input.monsterName()
                        + "에게 "
                        + input.playerDamage()
                        + " 피해");
    }

    /** 플레이어가 방어 스킬을 사용했을 때의 로그를 추가한다. */
    private void addPlayerDefenseLine(
            final List<String> lines, final BattleLogInput input, final String skillTag) {
        final boolean isCounter = input.skillLabel() != null && input.skillLabel().contains("카운터");
        if (input.playerDamage() > 0) {
            if (isCounter) {
                lines.add(
                        "⚡ "
                                + skillTag
                                + " 💥 적 공격을 흘려내며"
                                + ARROW
                                + input.playerDamage()
                                + " 치명 반격!");
            } else {
                lines.add(
                        "🛡️ "
                                + skillTag
                                + " 방어 성공 & 반격!"
                                + ARROW
                                + input.monsterName()
                                + "에게 "
                                + input.playerDamage()
                                + " 반격 피해");
            }
        } else if (input.monsterAction() == SkillType.NORMAL) {
            lines.add("🛡️ " + skillTag + " 완벽 방어!" + ARROW + "빈틈 포착 (다음 턴 선제 찬스⚡)");
        } else if (input.monsterDamage() > 0) {
            lines.add("⚠️ " + skillTag + " 몬스터 강공격에 방어선 관통!");
        } else {
            if (isCounter) {
                lines.add("⚡ " + skillTag + " 적이 공격하지 않아 빗나감");
            } else if (input.monsterAction() == SkillType.DEFENSE) {
                lines.add("🛡️ " + skillTag + " 맞방어 교착 상태");
            } else {
                lines.add("🛡️ " + skillTag + " 적의 공격을 완벽히 막아냄 (0 피해)");
            }
        }
    }

    /** 몬스터 행동 로그 한 줄을 추가한다. */
    private void addMonsterLine(final List<String> lines, final BattleLogInput input) {
        if (input.monsterAction() == SkillType.DEFENSE) {
            addMonsterDefenseLine(lines, input);
            return;
        }
        final String monsterTag = "[" + input.monsterName() + "]";
        final String actionLabel = input.monsterAction() == SkillType.HEAVY ? "강공격" : "일반공격";
        final boolean isCounter = input.skillLabel() != null && input.skillLabel().contains("카운터");
        if (input.playerType() == SkillType.DEFENSE && input.monsterAction() == SkillType.NORMAL) {
            if (isCounter) {
                lines.add("🐺 " + monsterTag + " " + actionLabel + ARROW + "빗나감");
            } else {
                lines.add(
                        "🐺 "
                                + monsterTag
                                + " "
                                + actionLabel
                                + ARROW
                                + "🛡️ 방어로 경감되어 "
                                + input.monsterDamage()
                                + " 피해");
            }
        } else if (input.monsterDamage() > 0) {
            lines.add(
                    "🐺 "
                            + monsterTag
                            + " "
                            + actionLabel
                            + ARROW
                            + input.monsterDamage()
                            + " 피해 피격");
        } else {
            lines.add("🐺 " + monsterTag + " " + actionLabel + ARROW + "빗나감");
        }
    }

    /** 몬스터가 방어했을 때의 로그 한 줄을 추가한다. */
    private void addMonsterDefenseLine(final List<String> lines, final BattleLogInput input) {
        final String monsterTag = "[" + input.monsterName() + "]";
        if (input.castFailure() || input.playerType() == SkillType.DEFENSE) {
            lines.add("🐺 " + monsterTag + " 🛡️ 방어 태세 유지");
        } else if (input.monsterDamage() > 0) {
            lines.add(
                    "🐺 "
                            + monsterTag
                            + " 🛡️ 방어 성공 & 반격"
                            + ARROW
                            + input.monsterDamage()
                            + " 피해 피격");
        } else if (RockPaperScissors.isNormalFamily(input.playerType())) {
            lines.add("🐺 " + monsterTag + " 🛡️ 공격 방어 성공" + ARROW + "반격 태세 (다음 턴 선제 주의⚠️)");
        } else if (input.playerDamage() > 0) {
            lines.add("🐺 " + monsterTag + " 💥 방어선 관통됨!");
        } else {
            lines.add("🐺 " + monsterTag + " 🛡️ 완전 방어 (0 피해)");
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
