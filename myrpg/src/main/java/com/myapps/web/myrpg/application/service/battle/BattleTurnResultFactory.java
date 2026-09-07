package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;

/**
 * 19개 인자를 갖는 {@link BattleTurnResult}의 생성을 의미별로 캡슐화하는 결과 팩토리 협력 객체.
 *
 * <p>전투 턴 결과 생성 시 반복되던 빈 리스트·기본 플래그 설정 및 생성자 호출 중복을 제거한다.
 */
public final class BattleTurnResultFactory {

    /**
     * 정상 전투 턴 진행 결과를 생성한다.
     *
     * @param playerAction 플레이어가 선택한 스킬 타입
     * @param playerDamage 플레이어가 가한 피해
     * @param monsterAction 몬스터가 선택한 스킬 타입
     * @param monsterDamage 몬스터가 가한 피해
     * @param playerCritical 플레이어 크리티컬 발동 여부
     * @param monsterCritical 몬스터 크리티컬 발동 여부
     * @param blocked 방어로 경감된 여부
     * @param countered 반격 발생 여부
     * @param castFailure 마법 캐스팅 실패 여부
     * @param firstStrike 선제 공격 여부
     * @param outcome 전투 결과 유형
     * @param reward 처치 보상
     * @param experienceGained 획득 경험치
     * @param playerHits 플레이어 공격 히트별 상세
     * @param combatLines 전투 액션 로그
     * @param dungeonClearResult 던전 클리어 결과
     * @return 조립된 전투 턴 결과
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public BattleTurnResult combat(
            final SkillType playerAction,
            final int playerDamage,
            final SkillType monsterAction,
            final int monsterDamage,
            final boolean playerCritical,
            final boolean monsterCritical,
            final boolean blocked,
            final boolean countered,
            final boolean castFailure,
            final boolean firstStrike,
            final Outcome outcome,
            final DropResult reward,
            final long experienceGained,
            final List<HitResult> playerHits,
            final List<String> combatLines,
            final DungeonClearResult dungeonClearResult) {
        return new BattleTurnResult(
                playerAction,
                playerDamage,
                monsterAction,
                monsterDamage,
                playerCritical,
                monsterCritical,
                blocked,
                countered,
                castFailure,
                firstStrike,
                false,
                null,
                outcome != Outcome.NONE,
                outcome,
                reward,
                experienceGained,
                playerHits,
                combatLines,
                dungeonClearResult);
    }

    /**
     * 타임아웃 또는 도망 실패 등 몬스터만 일방적으로 행동/피해를 입힌 턴 결과를 생성한다.
     *
     * @param monsterAction 몬스터가 선택한 스킬 타입
     * @param monsterDamage 몬스터가 가한 피해
     * @param outcome 전투 결과 유형
     * @param combatLines 전투 액션 로그
     * @return 조립된 전투 턴 결과
     */
    public BattleTurnResult monsterOnly(
            final SkillType monsterAction,
            final int monsterDamage,
            final Outcome outcome,
            final List<String> combatLines) {
        return new BattleTurnResult(
                null,
                0,
                monsterAction,
                monsterDamage,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                outcome != Outcome.NONE,
                outcome,
                null,
                0,
                List.of(),
                combatLines,
                null);
    }

    /**
     * 도망 성공 또는 소실된 전투의 안전 종료 등 즉각 전투가 종결되는 턴 결과를 생성한다.
     *
     * @param outcome 전투 결과 유형 (FLED 또는 NONE)
     * @param lines 전투 액션 로그
     * @return 조립된 전투 턴 결과
     */
    public BattleTurnResult terminal(final Outcome outcome, final List<String> lines) {
        return new BattleTurnResult(
                null, 0, null, 0, false, false, false, false, false, false, false, null, true,
                outcome, null, 0, List.of(), lines, null);
    }

    /**
     * 미지 스킬이나 쿨타임 잔존 등으로 인해 아무런 공방 없이 턴이 무효 처리된 결과를 생성한다.
     *
     * @return 빈 전투 턴 결과
     */
    public BattleTurnResult noOp() {
        return new BattleTurnResult(
                null,
                0,
                null,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                false,
                Outcome.NONE,
                null,
                0,
                List.of(),
                List.of(),
                null);
    }

    /**
     * 스킬 사용에 필요한 자원(MP, SP 등)이 부족하여 턴이 진행되지 않은 결과를 생성한다.
     *
     * @param skill 사용하려 했던 스킬
     * @param insufficientKind 부족한 자원 종류
     * @return 자원 부족 턴 결과
     */
    public BattleTurnResult insufficient(final Skill skill, final ResourceKind insufficientKind) {
        return new BattleTurnResult(
                skill.type(),
                0,
                null,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                insufficientKind,
                false,
                Outcome.NONE,
                null,
                0,
                List.of(),
                List.of(),
                null);
    }
}
