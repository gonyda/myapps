package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.UltimateSkill;

/**
 * 스킬 서브타입별 전투 처리를 위임받는 방문자 인터페이스.
 *
 * <p>서비스 협력객체(랜덤/리포지토리/메시지 등)를 캡처한 구현체가 각 서브타입에 대응하는 전투 처리를 수행한다. sealed {@link Skill}의 8종 서브타입을 모두
 * 방문한다.
 *
 * @param <R> 방문 결과 타입 (서비스 측에서 바인딩)
 */
public interface SkillCombatVisitor<R> {

    /**
     * 일반/속성 딜 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 딜 스킬
     * @return 방문 결과
     */
    R visitDamage(DamageSkill skill);

    /**
     * 방어/반격 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 방어 스킬
     * @return 방문 결과
     */
    R visitDefense(DefenseSkill skill);

    /**
     * 회복 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 회복 스킬
     * @return 방문 결과
     */
    R visitRecovery(RecoverySkill skill);

    /**
     * 결전 궁극기 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 궁극기 스킬
     * @return 방문 결과
     */
    R visitUltimate(UltimateSkill skill);

    /**
     * 상시 영구 패시브 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 패시브 스킬
     * @return 방문 결과
     */
    R visitPassive(PassiveSkill skill);

    /**
     * 강화/보호 버프 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 버프 스킬
     * @return 방문 결과
     */
    R visitBuff(BuffSkill skill);

    /**
     * 상태이상 CC 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill CC 스킬
     * @return 방문 결과
     */
    R visitCc(CcSkill skill);

    /**
     * 지속 피해 도트 스킬을 방문하여 전투 처리를 수행한다.
     *
     * @param skill 도트 스킬
     * @return 방문 결과
     */
    R visitDot(DotSkill skill);
}
