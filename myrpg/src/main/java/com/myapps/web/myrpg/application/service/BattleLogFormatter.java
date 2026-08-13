package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 전투 한 턴의 활동 로그 문자열을 생성하는 순수 포매터.
 *
 * <p>가위바위보 9칸 매트릭스의 결과({@link BattleLogInput})를 받아
 * 플레이어 행동 로그와 몬스터 행동 로그를 문자열 목록으로 산출한다.
 * 상태를 갖지 않고 외부 의존성이 없어 단위 테스트가 용이하다.
 *
 * <p>일반 턴은 2줄(플레이어 1줄 + 몬스터 1줄)을 반환한다.
 * 활 1턴 선제 사격은 1~2줄, 마법 캐스팅 실패는 몬스터 1줄만 반환한다
 * (캐스팅 실패 문구 자체는 상위 서비스에서 이미 기록된다).
 *
 * <p>멀티히트(hitCount ≥ 2)일 때는 헤더("{스킬}({타입}) {N}연타")와
 * 브레이크다운("{d1}  {d2}(치명)  … = {합계} 피해")을 생성한다.
 *
 * <p>방어 승리(반격) 상황은 플레이어·몬스터 어느 쪽이 방어하든 동일한 구조로
 * 표현된다: 방어한 쪽은 "방어하며 반격!", 공격한 쪽은 경감된 피해를 각각 남긴다.
 */
public class BattleLogFormatter {

    private static final String HIT_SEPARATOR = "  ";
    private static final String CRITICAL_SUFFIX = "(치명)";
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

    /**
     * 선제 사격 로그를 추가한다.
     *
     * <p>멀티히트(playerHits.size() ≥ 2)이면 헤더+브레이크다운 2줄,
     * 단일이면 기존 형식 1줄을 추가한다.
     */
    private void addFirstStrikeLine(final List<String> lines, final BattleLogInput input) {
        final String prefix = input.skillLabel() + "(" + input.playerType().label() + ")";
        if (isMultiHit(input)) {
            lines.add("선제 사격! " + prefix + " " + input.playerHits().size() + "연타");
            lines.add(buildBreakdownLine(input.playerHits(), input.playerDamage()));
        } else {
            lines.add("선제 사격! " + prefix + "로 " + input.monsterName()
                    + "에게 " + input.playerDamage() + " 피해");
        }
    }

    /**
     * 플레이어 행동 로그를 추가한다.
     *
     * <p>방어 스킬은 반격/관통/교착으로 분기하고, 공격 스킬은 멀티히트·단일·빗나감에
     * 따라 문구를 결정한다.
     */
    private void addPlayerLine(final List<String> lines, final BattleLogInput input) {
        final String prefix = input.skillLabel() + "(" + input.playerType().label() + ")";
        if (input.playerType() == SkillType.DEFENSE) {
            addPlayerDefenseLine(lines, input, prefix);
            return;
        }
        if (input.playerDamage() <= 0) {
            lines.add(prefix + " 공격이 빗나갔다!");
            return;
        }
        if (isMultiHit(input)) {
            addMultiHitLines(lines, input, prefix);
        } else {
            addSingleHitLine(lines, input, prefix);
        }
    }

    /**
     * 멀티히트(hitCount ≥ 2) 로그 2줄(헤더 + 브레이크다운)을 추가한다.
     */
    private void addMultiHitLines(final List<String> lines, final BattleLogInput input,
                                  final String prefix) {
        lines.add(prefix + " " + input.playerHits().size() + "연타");
        lines.add(buildBreakdownLine(input.playerHits(), input.playerDamage()));
    }

    /**
     * 단일 히트 플레이어 공격 로그 1줄을 추가한다.
     */
    private void addSingleHitLine(final List<String> lines, final BattleLogInput input,
                                  final String prefix) {
        final String hit = prefix + "로 " + input.monsterName()
                + "에게 " + input.playerDamage() + " 피해";
        lines.add(input.playerCritical() ? hit + " (크리티컬!)" : hit);
    }

    /**
     * 플레이어가 방어 스킬을 사용했을 때의 로그 한 줄을 추가한다.
     *
     * <p>반격 피해가 있으면 방어 승리(반격), 반격이 없고 피해만 있으면 관통,
     * 양쪽 모두 0이면 교착으로 표현한다.
     */
    private void addPlayerDefenseLine(final List<String> lines, final BattleLogInput input,
                                      final String prefix) {
        if (input.playerDamage() > 0) {
            lines.add(prefix + "로 방어하며 반격! (" + input.playerDamage() + " 피해)");
        } else if (input.monsterDamage() > 0) {
            lines.add(prefix + " 방어가 뚫렸다!");
        } else {
            lines.add(prefix + "로 맞서 교착 상태!");
        }
    }

    /**
     * 몬스터 행동 로그 한 줄을 추가한다.
     *
     * <p>몬스터가 방어했으면 반격/관통/교착으로 분기하고, 공격했으면 적중 여부에
     * 따라 피해 또는 빗나감을 남긴다.
     */
    private void addMonsterLine(final List<String> lines, final BattleLogInput input) {
        if (input.monsterAction() == SkillType.DEFENSE) {
            addMonsterDefenseLine(lines, input);
            return;
        }
        final String action = input.monsterName() + "의 " + input.monsterAction().label() + "공격";
        if (input.monsterDamage() > 0) {
            lines.add(action + ", " + input.monsterDamage() + " 피해를 입음");
        } else {
            lines.add(action + "이 빗나갔다!");
        }
    }

    /**
     * 몬스터가 방어했을 때의 로그 한 줄을 추가한다.
     *
     * <p>반격 피해가 있으면 방어 승리(반격), 반격이 없고 플레이어 피해만 있으면
     * 관통, 양쪽 모두 0이면 방어 태세로 표현한다.
     */
    private void addMonsterDefenseLine(final List<String> lines, final BattleLogInput input) {
        if (input.monsterDamage() > 0) {
            lines.add(input.monsterName() + "이(가) 방어하며 반격! (" + input.monsterDamage() + " 피해)");
        } else if (input.playerDamage() > 0) {
            lines.add(input.monsterName() + "의 방어가 뚫렸다!");
        } else {
            lines.add(input.monsterName() + "이(가) 방어 태세를 취했다.");
        }
    }

    /**
     * 멀티히트 여부를 판정한다.
     *
     * @param input 로그 입력
     * @return playerHits 크기가 {@value #MULTI_HIT_THRESHOLD} 이상이면 {@code true}
     */
    private boolean isMultiHit(final BattleLogInput input) {
        return input.playerHits() != null && input.playerHits().size() >= MULTI_HIT_THRESHOLD;
    }

    /**
     * 히트별 브레이크다운 줄을 생성한다.
     *
     * <p>각 히트 피해를 두 칸 공백으로 나열하되, 크리티컬 히트에는 "(치명)" 접미사를 붙인다.
     * 마지막에 "= {합계} 피해"를 추가한다.
     *
     * @param hits        히트별 결과 목록
     * @param totalDamage 총 피해 합계
     * @return 브레이크다운 문자열 (예: "22  33(치명)  19 = 74 피해")
     */
    private String buildBreakdownLine(final List<HitResult> hits, final int totalDamage) {
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
        builder.append(" = ").append(totalDamage).append(" 피해");
        return builder.toString();
    }
}
