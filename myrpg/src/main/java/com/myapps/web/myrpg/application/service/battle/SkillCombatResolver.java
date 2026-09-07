package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import com.myapps.web.myrpg.domain.service.RockPaperScissors;
import com.myapps.web.myrpg.domain.service.SkillCombatVisitor;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 스킬 타입별 전투 처리·선후공·디스패치를 담당하는 협력 클래스.
 *
 * <p>방문자 패턴({@link SkillCombatVisitor})을 내부화하여 스킬 서브타입에 따른 전투 핸들러 디스패치 및 선후공/선제공격/특수 효과 처리를 수행합니다.
 */
public final class SkillCombatResolver {

    private static final int PERCENT_DIVISOR = 100;

    private final BattleResolver resolver;
    private final CombatStatResolver combatStatResolver;
    private final CharacterSkillRepository characterSkillRepository;
    private final StatProgression statProgression;
    private final SkillService skillService;
    private final GameMessageService gameMessageService;
    private final GameProperties gameProperties;
    private final Random random;

    /**
     * SkillCombatResolver 인스턴스를 생성한다.
     *
     * @param resolver 전투 해석기
     * @param combatStatResolver 전투 스탯 해석기
     * @param characterSkillRepository 캐릭터 스킬 레포지토리
     * @param statProgression 스탯 성장 제공자
     * @param skillService 스킬 서비스
     * @param gameMessageService 게임 메시지 서비스
     * @param gameProperties 게임 프로퍼티
     * @param random 난수 생성기
     */
    public SkillCombatResolver(
            final BattleResolver resolver,
            final CombatStatResolver combatStatResolver,
            final CharacterSkillRepository characterSkillRepository,
            final StatProgression statProgression,
            final SkillService skillService,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties,
            final Random random) {
        this.resolver = resolver;
        this.combatStatResolver = combatStatResolver;
        this.characterSkillRepository = characterSkillRepository;
        this.statProgression = statProgression;
        this.skillService = skillService;
        this.gameMessageService = gameMessageService;
        this.gameProperties = gameProperties;
        this.random = random;
    }

    /**
     * 정상 턴의 스킬 전투를 디스패치한다 (단일 진입점).
     *
     * @param context 전투 처리 입력 컨텍스트
     * @return 턴 전투 결과 및 다음 선제 파티를 포함하는 결과 객체
     */
    public SkillCombatOutcome resolveSkillCombat(final SkillCombatContext context) {
        final CombatDispatchVisitor visitor = new CombatDispatchVisitor(context);
        final TurnCombatResult combat = context.skill().accept(visitor);
        return new SkillCombatOutcome(combat, visitor.nextPreemptive());
    }

    /**
     * 활 무기 착용 시 첫 턴 선제공격 여부를 판정한다.
     *
     * @param state 현재 전투 상태
     * @param equippedTalent 착용 중인 무기 계열 재능
     * @return 1턴이고 활을 착용 중이면 true
     */
    public boolean isBowFirstStrike(final BattleState state, final SkillTalent equippedTalent) {
        return state.getTurnCount() == 1 && equippedTalent == SkillTalent.ARCHERY;
    }

    /**
     * 몬스터 선제 공격 시의 피해량을 계산한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param monster 대상 몬스터
     * @return 몬스터가 플레이어에게 입히는 최종 피해량
     */
    public int resolveMonsterPreemptiveDamage(
            final CharacterProgress progress, final Monster monster) {
        final int playerDefense = combatStatResolver.resolvePlayerDefense(progress);
        final int monsterBase =
                resolver.baseDamage(
                        monster.attackPower(),
                        gameProperties.battle().monsterNormalMultiplier(),
                        playerDefense);
        final boolean monsterCrit = resolver.rollCritical(monster.critical());
        return resolver.finalDamage(monsterBase, 1.0, monsterCrit);
    }

    /**
     * 몬스터의 행동(스킬 타입)에 따른 대미지 배율을 해석한다 (중복 통합 헬퍼).
     *
     * @param monsterAction 몬스터의 행동
     * @return 적용할 몬스터 대미지 배율
     */
    public int monsterMultiplier(final SkillType monsterAction) {
        return monsterAction == SkillType.HEAVY
                ? gameProperties.battle().monsterHeavyMultiplier()
                : gameProperties.battle().monsterNormalMultiplier();
    }

    /**
     * 몬스터 단독 공격 피해량을 계산한다 (캐스팅 실패, 타임아웃, 도망 실패 등).
     *
     * @param progress 캐릭터 진행 상태
     * @param monster 대상 몬스터
     * @param monsterAction 몬스터의 행동
     * @return 몬스터가 플레이어에게 입히는 최종 피해량
     */
    public int resolveMonsterOnlyDamage(
            final CharacterProgress progress,
            final Monster monster,
            final SkillType monsterAction) {
        if (monsterAction == SkillType.DEFENSE) {
            return 0;
        }
        final int resolvedMonsterMultiplier = monsterMultiplier(monsterAction);
        final int playerDef = combatStatResolver.resolvePlayerDefense(progress);
        final int baseDmg =
                resolver.baseDamage(monster.attackPower(), resolvedMonsterMultiplier, playerDef);
        final boolean crit = resolver.rollCritical(monster.critical());
        return resolver.finalDamage(baseDmg, 1.0, crit);
    }

    /**
     * 특수 스킬 부가 효과(빙결, DoT 부여, 디버프 분노 증폭 등)를 적용한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param state 현재 전투 상태
     * @param skill 사용된 스킬
     * @param ownedSkillOpt 보유 스킬 정보
     * @param monster 대상 몬스터
     * @param equippedTalent 착용 중인 무기 계열 재능
     * @param combatLines 전투 로그 라인 목록
     */
    public void applySpecialSkillSideEffects(
            final CharacterProgress progress,
            final BattleState state,
            final Skill skill,
            final Optional<CharacterSkill> ownedSkillOpt,
            final Monster monster,
            final SkillTalent equippedTalent,
            final List<String> combatLines) {
        if (ownedSkillOpt.isEmpty()) {
            return;
        }
        final CharacterSkill charSkill = ownedSkillOpt.get();

        switch (skill) {
            case DamageSkill damageSkill -> {
                final int freezeRate = damageSkill.freezeRateAt(charSkill.getRank());
                if (freezeRate > 0 && random.nextInt(PERCENT_DIVISOR) < freezeRate) {
                    state.setMonsterStunnedTurns(1);
                    final String freezeMsg =
                            gameMessageService.get("battle.freeze", monster.name());
                    combatLines.add(freezeMsg);
                }
            }
            case DotSkill dotSkill -> {
                final int dotTurns = dotSkill.dotTurnsAt(charSkill.getRank());
                final int dotRate = dotSkill.dotPerTurnAt(charSkill.getRank());
                final int playerAttack = combatStatResolver.attackPower(progress, equippedTalent);
                final int dotDamage = Math.max(1, (playerAttack * dotRate) / 100);
                state.setDotTurnsLeft(dotTurns);
                state.setDotDamagePerTurn(dotDamage);
                final String dotMsg =
                        gameMessageService.get("battle.dot", monster.name(), dotDamage);
                combatLines.add(dotMsg);
            }
            default -> {}
        }

        if (skill.type() == SkillType.DEBUFF) {
            state.setNextAttackAmpPercent(30);
            final String rageMsg = gameMessageService.get("battle.rage");
            combatLines.add(rageMsg);
        }
    }

    TurnCombatResult resolvePlayerPreemptive(
            final CharacterProgress progress,
            final Monster monster,
            final Skill skill,
            final SkillTalent equippedTalent) {
        if (skill.type() == SkillType.DEFENSE) {
            return new TurnCombatResult(0, 0, false, false, false, false, true, List.of());
        }

        final int playerAttack = combatStatResolver.attackPower(progress, equippedTalent);
        final int multiplier = combatStatResolver.resolvePlayerMultiplier(skill, progress);
        final int effectiveCritical =
                combatStatResolver.resolveEffectivePlayerCritical(skill, progress);
        final int hitCount = combatStatResolver.resolvePlayerHitCount(skill);

        final List<HitResult> hits =
                resolver.multiHitDamage(
                        playerAttack,
                        multiplier,
                        monster.defense(),
                        1.0,
                        effectiveCritical,
                        hitCount);
        final int totalDamage = hits.stream().mapToInt(HitResult::damage).sum();
        final boolean anyCrit = hits.stream().anyMatch(HitResult::critical);

        return new TurnCombatResult(totalDamage, 0, anyCrit, false, false, false, true, hits);
    }

    TurnCombatResult resolveNormalCombat(
            final CharacterProgress progress,
            final Monster monster,
            final Skill skill,
            final SkillTalent equippedTalent,
            final SkillType monsterAction) {
        final int playerAttack = combatStatResolver.attackPower(progress, equippedTalent);
        final int playerMultiplier = combatStatResolver.resolvePlayerMultiplier(skill, progress);
        final int playerDefense = combatStatResolver.resolvePlayerDefense(progress);
        final int playerCritical =
                combatStatResolver.resolveEffectivePlayerCritical(skill, progress);
        final int playerBlockRate = combatStatResolver.resolvePlayerBlockRate(skill, progress);
        final int playerCounterPercent =
                combatStatResolver.resolvePlayerCounterPercent(skill, progress);
        final int playerHitCount = combatStatResolver.resolvePlayerHitCount(skill);
        final int resolvedMonsterMultiplier = monsterMultiplier(monsterAction);

        final boolean isCounterAttack = "counter_attack".equals(skill.id());
        final TurnInput input =
                new TurnInput(
                        skill.type(),
                        monsterAction,
                        playerAttack,
                        monster.attackPower(),
                        playerDefense,
                        monster.defense(),
                        playerMultiplier,
                        resolvedMonsterMultiplier,
                        playerBlockRate,
                        monster.defenseBlockRate(),
                        playerCounterPercent,
                        monster.defenseCounterRate(),
                        playerCritical,
                        monster.critical(),
                        playerHitCount,
                        isCounterAttack);

        final ResolvedTurn resolved = resolver.resolve(input);
        return new TurnCombatResult(
                resolved.playerDamageToMonster(),
                resolved.monsterDamageToPlayer(),
                resolved.playerCritical(),
                resolved.monsterCritical(),
                resolved.blocked(),
                resolved.countered(),
                false,
                resolved.playerHits());
    }

    boolean determineTurnOrder(final SkillType playerType, final SkillType monsterType) {
        if (playerType == monsterType
                || (RockPaperScissors.isNormalFamily(playerType)
                        && RockPaperScissors.isNormalFamily(monsterType))) {
            return random.nextInt(2) == 0;
        }
        if (RockPaperScissors.isNormalFamily(playerType) && monsterType == SkillType.DEFENSE) {
            return true;
        }
        if (playerType == SkillType.DEFENSE && RockPaperScissors.isNormalFamily(monsterType)) {
            return false;
        }
        return random.nextInt(2) == 0;
    }

    TurnCombatResult resolveUltimateCombat(
            final CharacterProgress progress,
            final Monster monster,
            final UltimateSkill ultimateSkill,
            final CharacterSkill charSkill) {
        final SkillTalent equippedTalent =
                ultimateSkill.talent() == SkillTalent.COMMON
                        ? SkillTalent.MELEE
                        : ultimateSkill.talent();
        final int playerAttack = combatStatResolver.attackPower(progress, equippedTalent);
        final int multiplier = ultimateSkill.multiplierAt(charSkill.getRank());
        final int effectiveCritical =
                combatStatResolver.resolveEffectivePlayerCritical(ultimateSkill, progress)
                        + ultimateSkill.critBonus();
        final int hitCount = ultimateSkill.hitCountAt(charSkill.getRank());

        final List<HitResult> hits =
                resolver.multiHitDamage(
                        playerAttack, multiplier, 0, 1.0, effectiveCritical, hitCount);
        final int totalDamage = hits.stream().mapToInt(HitResult::damage).sum();
        final boolean anyCrit = hits.stream().anyMatch(HitResult::critical);

        charSkill.setUltimateCooldown(ultimateSkill.coolWinsAt(charSkill.getRank()));
        characterSkillRepository.save(charSkill);

        return new TurnCombatResult(totalDamage, 0, anyCrit, false, false, false, true, hits);
    }

    TurnCombatResult resolveRecoveryCombat(
            final CharacterProgress progress,
            final Monster monster,
            final RecoverySkill recoverySkill,
            final CharacterSkill charSkill,
            final BattleState state,
            final List<String> combatLines,
            final SkillType monsterAction) {
        final VitalMax baseVital =
                statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent());
        final VitalMax skillVital = skillService.rankupVitalBonus(progress.getId());
        final int maxHp = baseVital.hp() + skillVital.hp();
        final int healAmount = recoverySkill.healAmountAt(charSkill.getRank());
        final int beforeHp = progress.getHpCurrent();
        progress.healHp(healAmount, maxHp);
        final int actualHealed = progress.getHpCurrent() - beforeHp;
        final String healMsg =
                gameMessageService.get("battle.recovery", recoverySkill.label(), actualHealed);
        combatLines.add(healMsg);

        final int monsterDamage =
                resolveMonsterDamageForSupportTurn(
                        progress, monster, state, combatLines, monsterAction);
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    TurnCombatResult resolveBuffCombat(
            final CharacterProgress progress,
            final Monster monster,
            final BuffSkill buffSkill,
            final CharacterSkill charSkill,
            final BattleState state,
            final List<String> combatLines,
            final SkillType monsterAction) {
        state.setManaShieldTurnsLeft(buffSkill.durationTurns());
        state.setManaShieldAbsorbRate(buffSkill.absorbRateAt(charSkill.getRank()));
        combatLines.add(
                "✨ [마나 실드] 피해 "
                        + buffSkill.absorbRateAt(charSkill.getRank())
                        + "% MP 흡수 버프 ("
                        + buffSkill.durationTurns()
                        + "턴)");

        final int monsterDamage =
                resolveMonsterDamageForSupportTurn(
                        progress, monster, state, combatLines, monsterAction);
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    TurnCombatResult resolveCcCombat(
            final CharacterProgress progress,
            final Monster monster,
            final CcSkill ccSkill,
            final CharacterSkill charSkill,
            final BattleState state,
            final List<String> combatLines,
            final SkillType monsterAction) {
        final int rate = ccSkill.successRateAt(charSkill.getRank());
        final boolean success = random.nextInt(PERCENT_DIVISOR) < rate;
        if (success) {
            state.setMonsterStunnedTurns(1);
            final String ccSuccessMsg =
                    gameMessageService.get("battle.cc.success", ccSkill.label(), monster.name());
            combatLines.add(ccSuccessMsg);
        } else {
            final String ccResistMsg = gameMessageService.get("battle.cc.resist", ccSkill.label());
            combatLines.add(ccResistMsg);
        }

        final int monsterDamage =
                resolveMonsterDamageForSupportTurn(
                        progress, monster, state, combatLines, monsterAction);
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    int resolveMonsterDamageForSupportTurn(
            final CharacterProgress progress,
            final Monster monster,
            final BattleState state,
            final List<String> combatLines,
            final SkillType monsterAction) {
        if (monsterAction == null) {
            return 0;
        }
        if (state.getMonsterStunnedTurns() > 0) {
            final String stunMsg = gameMessageService.get("battle.stun_frozen", monster.name());
            combatLines.add(stunMsg);
            state.setMonsterStunnedTurns(state.getMonsterStunnedTurns() - 1);
            return 0;
        }
        return resolveMonsterOnlyDamage(progress, monster, monsterAction);
    }

    /**
     * 스킬 타입별 전투 처리를 디스패치하는 내부 방문자 구현체.
     *
     * <p>협력 객체 및 컨텍스트를 활용하여 각 서브타입에 대응하는 전투 핸들러를 호출한다.
     */
    private final class CombatDispatchVisitor implements SkillCombatVisitor<TurnCombatResult> {
        private final SkillCombatContext context;
        private PreemptiveParty nextPreemptive = PreemptiveParty.NONE;

        private CombatDispatchVisitor(final SkillCombatContext context) {
            this.context = context;
        }

        @Override
        public TurnCombatResult visitDamage(final DamageSkill skill) {
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitDefense(final DefenseSkill skill) {
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitRecovery(final RecoverySkill skill) {
            if (context.ownedSkillOpt().isPresent()) {
                final TurnCombatResult combat =
                        resolveRecoveryCombat(
                                context.progress(),
                                context.monster(),
                                skill,
                                context.ownedSkillOpt().get(),
                                context.state(),
                                context.combatLines(),
                                context.isPlayerPreemptive() ? null : context.monsterAction());
                return new TurnCombatResult(
                        0,
                        combat.monsterDamage(),
                        false,
                        false,
                        false,
                        false,
                        context.isPlayerPreemptive(),
                        List.of());
            }
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitUltimate(final UltimateSkill skill) {
            if (context.ownedSkillOpt().isPresent()) {
                return resolveUltimateCombat(
                        context.progress(),
                        context.monster(),
                        skill,
                        context.ownedSkillOpt().get());
            }
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitPassive(final PassiveSkill skill) {
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitBuff(final BuffSkill skill) {
            if (context.ownedSkillOpt().isPresent()) {
                final TurnCombatResult combat =
                        resolveBuffCombat(
                                context.progress(),
                                context.monster(),
                                skill,
                                context.ownedSkillOpt().get(),
                                context.state(),
                                context.combatLines(),
                                context.isPlayerPreemptive() ? null : context.monsterAction());
                return new TurnCombatResult(
                        0,
                        combat.monsterDamage(),
                        false,
                        false,
                        false,
                        false,
                        context.isPlayerPreemptive(),
                        List.of());
            }
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitCc(final CcSkill skill) {
            if (context.ownedSkillOpt().isPresent()) {
                final TurnCombatResult combat =
                        resolveCcCombat(
                                context.progress(),
                                context.monster(),
                                skill,
                                context.ownedSkillOpt().get(),
                                context.state(),
                                context.combatLines(),
                                context.isPlayerPreemptive() ? null : context.monsterAction());
                return new TurnCombatResult(
                        0,
                        combat.monsterDamage(),
                        false,
                        false,
                        false,
                        false,
                        context.isPlayerPreemptive(),
                        List.of());
            }
            return resolveDefaultCombat(skill);
        }

        @Override
        public TurnCombatResult visitDot(final DotSkill skill) {
            return resolveDefaultCombat(skill);
        }

        private TurnCombatResult resolveDefaultCombat(final Skill skill) {
            if (context.isPlayerPreemptive()) {
                return resolvePlayerPreemptive(
                        context.progress(), context.monster(), skill, context.equippedTalent());
            }
            final TurnCombatResult combat =
                    resolveNormalCombat(
                            context.progress(),
                            context.monster(),
                            skill,
                            context.equippedTalent(),
                            context.monsterAction());
            int resolvedPlayerDamage = combat.playerDamage();
            int resolvedMonsterDamage = combat.monsterDamage();

            if (!combat.firstStrike() && resolvedPlayerDamage > 0 && resolvedMonsterDamage > 0) {
                final boolean playerFirst =
                        determineTurnOrder(skill.type(), context.monsterAction());
                if (playerFirst && resolvedPlayerDamage >= context.state().getMonsterCurrentHp()) {
                    resolvedMonsterDamage = 0;
                } else if (!playerFirst
                        && resolvedMonsterDamage >= context.progress().getHpCurrent()) {
                    resolvedPlayerDamage = 0;
                }
            }

            if ("defense".equals(skill.id())
                    && RockPaperScissors.isNormalFamily(context.monsterAction())) {
                nextPreemptive = PreemptiveParty.PLAYER;
            } else if (RockPaperScissors.isNormalFamily(skill.type())
                    && context.monsterAction() == SkillType.DEFENSE) {
                nextPreemptive = PreemptiveParty.MONSTER;
            }

            return new TurnCombatResult(
                    resolvedPlayerDamage,
                    resolvedMonsterDamage,
                    combat.playerCritical(),
                    combat.monsterCritical(),
                    combat.blocked(),
                    combat.countered(),
                    combat.firstStrike(),
                    combat.playerHits());
        }

        private PreemptiveParty nextPreemptive() {
            return nextPreemptive;
        }
    }
}
