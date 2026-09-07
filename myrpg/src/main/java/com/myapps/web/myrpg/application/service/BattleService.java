package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.service.battle.BattleRewardService;
import com.myapps.web.myrpg.application.service.battle.BattleTurnResultFactory;
import com.myapps.web.myrpg.application.service.battle.BattleViewAssembler;
import com.myapps.web.myrpg.application.service.battle.CombatStatResolver;
import com.myapps.web.myrpg.application.service.battle.KillResolution;
import com.myapps.web.myrpg.application.service.battle.SkillCombatContext;
import com.myapps.web.myrpg.application.service.battle.SkillCombatOutcome;
import com.myapps.web.myrpg.application.service.battle.SkillCombatResolver;
import com.myapps.web.myrpg.application.service.battle.TurnCombatResult;
import com.myapps.web.myrpg.application.service.battle.TurnEffectApplier;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillDamagePolicy;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전투 오케스트레이션 애플리케이션 서비스.
 *
 * <p>전투 시작({@code start})·턴 진행({@code takeTurn})·도망({@code flee})· 전투 재개({@code resumeIfActive})·전투
 * 스킬 목록({@code combatSkills})을 기존 서비스들을 조립하여 수행한다.
 *
 * <p>자원 소모·재능 특성(활 1턴 선제·마법 캐스팅 실패)·선후공· HP 감소·사망·보상·저장을 순서대로 처리하며, 각 턴 결과를 {@link
 * BattleTurnResult}로 반환한다.
 *
 * <p>이연 seam:
 *
 * <ul>
 *   <li>7순위(대장간) 스펙에서 수리 기능이 추가되면 전투 후 수리 권유 UI와 연동. 현재는 파손 시 자동 해제까지만.
 *   <li>인챈트 스펙 확정 후 보스 전용 드랍 테이블 및 인챈트 아이템 드랍을 연결.
 *   <li>10순위(던전) 스펙에서 던전 노드 내 연속 전투와 보스 조우를 구현.
 * </ul>
 */
@Service
public class BattleService {

    private static final int PERCENT_DIVISOR = 100;
    private static final String LOG_TYPE_COMBAT = "combat";

    private final BattleStateRepository battleStateRepository;
    private final MonsterService monsterService;
    private final MonsterAiService monsterAiService;
    private final SkillService skillService;
    private final BattleLogFormatter logFormatter;
    private final InventoryService inventoryService;
    private final CharacterService characterService;
    private final ActionLog actionLog;
    private final Random random;
    private final SkillCatalogService skillCatalogService;
    private final CharacterSkillRepository characterSkillRepository;
    private final GameMessageService gameMessageService;
    private final GameProperties gameProperties;
    private final CombatStatResolver combatStatResolver;
    private final SkillCombatResolver skillCombatResolver;
    private final TurnEffectApplier turnEffectApplier;
    private final BattleRewardService battleRewardService;
    private final BattleViewAssembler battleViewAssembler;
    private final BattleTurnResultFactory turnResultFactory;

    /** BattleService를 생성한다 (Spring 주입용). */
    @org.springframework.beans.factory.annotation.Autowired
    public BattleService(
            final BattleStateRepository battleStateRepository,
            final BattleResolver resolver,
            final MonsterService monsterService,
            final MonsterAiService monsterAiService,
            final MonsterRewardService monsterRewardService,
            final SkillService skillService,
            final InventoryService inventoryService,
            final ProgressionService progressionService,
            final CharacterService characterService,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final Random random,
            final SkillCatalogService skillCatalogService,
            final CharacterSkillRepository characterSkillRepository,
            final ItemCatalogService itemCatalogService,
            final DungeonService dungeonService,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties) {
        this.battleStateRepository = battleStateRepository;
        this.monsterService = monsterService;
        this.monsterAiService = monsterAiService;
        this.skillService = skillService;
        final SkillDamagePolicy skillDamagePolicy = new SkillDamagePolicy();
        this.gameMessageService =
                gameMessageService != null ? gameMessageService : new GameMessageService(null);
        this.gameProperties =
                gameProperties != null
                        ? gameProperties
                        : new GameProperties(null, null, null, null, null, null);
        this.logFormatter = new BattleLogFormatter(this.gameMessageService);
        this.inventoryService = inventoryService;
        this.characterService = characterService;
        this.actionLog = actionLog;
        this.random = random;
        this.skillCatalogService = skillCatalogService;
        this.characterSkillRepository = characterSkillRepository;
        this.combatStatResolver =
                new CombatStatResolver(
                        statProgression,
                        this.skillService,
                        this.inventoryService,
                        this.characterSkillRepository,
                        skillDamagePolicy,
                        this.gameProperties,
                        this.gameMessageService,
                        this.random);
        this.skillCombatResolver =
                new SkillCombatResolver(
                        resolver,
                        this.combatStatResolver,
                        this.characterSkillRepository,
                        statProgression,
                        this.skillService,
                        this.gameMessageService,
                        this.gameProperties,
                        this.random);
        this.turnEffectApplier =
                new TurnEffectApplier(this.skillService, statProgression, this.gameMessageService);
        this.battleRewardService =
                new BattleRewardService(
                        monsterRewardService,
                        this.inventoryService,
                        progressionService,
                        dungeonService,
                        itemCatalogService,
                        this.gameMessageService,
                        this.actionLog,
                        this.random);
        this.battleViewAssembler = new BattleViewAssembler(this.gameMessageService);
        this.turnResultFactory = new BattleTurnResultFactory();
    }

    /** 이전 호환용 생성자. */
    public BattleService(
            final BattleStateRepository battleStateRepository,
            final BattleResolver resolver,
            final MonsterService monsterService,
            final MonsterAiService monsterAiService,
            final MonsterRewardService monsterRewardService,
            final SkillService skillService,
            final InventoryService inventoryService,
            final ProgressionService progressionService,
            final CharacterService characterService,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final Random random,
            final SkillCatalogService skillCatalogService,
            final CharacterSkillRepository characterSkillRepository,
            final ItemCatalogService itemCatalogService,
            final DungeonService dungeonService) {
        this(
                battleStateRepository,
                resolver,
                monsterService,
                monsterAiService,
                monsterRewardService,
                skillService,
                inventoryService,
                progressionService,
                characterService,
                statProgression,
                actionLog,
                random,
                skillCatalogService,
                characterSkillRepository,
                itemCatalogService,
                dungeonService,
                new GameMessageService(null),
                new GameProperties(null, null, null, null, null, null));
    }

    /**
     * 전투를 시작한다.
     *
     * <p>지정된 몬스터의 최대 HP로 {@link BattleState}를 생성하여 영속한다. 전투 시작 인트로 메시지는 컨트롤러가 {@code turnLog}로
     * 표시하며, 하단 활동 로그에는 추가하지 않는다. 몬스터 카탈로그에서 해당 ID를 찾을 수 없으면 전투를 시작하지 않고 {@code null}을 반환한다(예외 없이 안전
     * 처리).
     *
     * @param progress 캐릭터 진행 상태
     * @param monsterId 전투 대상 몬스터 ID
     * @param ambush 기습 여부
     * @return 생성된 전투 상태, 몬스터 미지 시 {@code null}
     */
    @Transactional
    public BattleState start(
            final CharacterProgress progress, final String monsterId, final boolean ambush) {
        final Optional<Monster> monsterOpt = monsterService.byId(monsterId);
        if (monsterOpt.isEmpty()) {
            return null;
        }
        final Monster monster = monsterOpt.get();
        final BattleState state =
                new BattleState(progress.getId(), monsterId, monster.maxHp(), ambush);
        final BattleState saved = battleStateRepository.save(state);
        return saved;
    }

    /**
     * 액티브 공방 페이즈를 개시한다.
     *
     * <p>대치 페이즈({@code standby = true})에서 플레이어가 [공방 개시]를 클릭했을 때 호출된다. 착용 무기 기준 활 1턴 선제 사격 여부를 판정하고,
     * 몬스터 의도를 추첨하여 전투 상태를 공방 페이즈({@code standby = false})로 전환한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param state 현재 전투 상태
     * @return 공방 페이즈 뷰 모델
     */
    @Transactional
    public BattleView startClash(final CharacterProgress progress, final BattleState state) {
        final Monster monster =
                monsterService
                        .byId(state.getMonsterId())
                        .orElseThrow(() -> new IllegalStateException("몬스터 정보를 찾을 수 없습니다."));

        final boolean isBow =
                (progress == null || progress.getId() == null || progress.getId().equals(1L))
                        ? inventoryService.isBowEquipped()
                        : inventoryService.isBowEquipped(progress.getId());
        final boolean bowFirstStrike = state.getTurnCount() == 1 && isBow;
        final PreemptiveParty preemptiveParty = state.getPreemptiveParty();

        final SkillType intent;
        if (bowFirstStrike || preemptiveParty == PreemptiveParty.PLAYER) {
            intent = null;
        } else if (preemptiveParty == PreemptiveParty.MONSTER) {
            intent = SkillType.NORMAL;
        } else {
            intent = monsterAiService.nextAction();
        }

        state.setCurrentMonsterIntent(intent);
        state.setStandby(false);
        battleStateRepository.save(state);

        final List<BattleSkillButton> skills = combatSkills(progress);
        return battleViewAssembler.buildClashBattleView(
                state, monster, skills, intent, bowFirstStrike, preemptiveParty);
    }

    /**
     * 전투 턴을 진행한다.
     *
     * <p>자원 검사·소모 → 마법 캐스팅 실패 판정 → 몬스터 행동 결정 → 재능 분기(활 1턴 선제 / 일반 매트릭스) → 선후공 결정·선공 처치 시 후공 스킵 → HP
     * 적용 → 스킬 훅(사용/막타) → 내구도 감소 → 처치 보상 → 사망 처리 → 저장 순서로 한 턴을 오케스트레이션한다.
     *
     * <p>전투 액션 로그(플레이어/몬스터 행동·선제·캐스팅 실패)는 {@code BattleTurnResult.combatLines}에만 담기고, 하단 활동 로그에는
     * 추가되지 않는다. 결산(처치 보상)·사망 라인만 하단 {@code actionLog}에 추가한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param state 현재 전투 상태
     * @param skillId 플레이어가 선택한 스킬 ID
     * @return 턴 결과
     */
    @Transactional
    public BattleTurnResult takeTurn(
            final CharacterProgress progress, final BattleState state, final String skillId) {
        final Optional<Monster> monsterOpt = monsterService.byId(state.getMonsterId());
        if (monsterOpt.isEmpty()) {
            return safeTerminateAndReturn(state);
        }
        final Monster monster = monsterOpt.get();

        if ("timeout".equalsIgnoreCase(skillId)) {
            return handleTimeoutTurn(progress, state, monster);
        }

        final Optional<Skill> skillOpt = skillCatalogService.byId(skillId);
        if (skillOpt.isEmpty()) {
            return buildNoOpResult();
        }
        final Skill skill = skillOpt.get();
        final Optional<CharacterSkill> ownedSkillOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skillId);

        if (checkUltimateCooldown(skill, ownedSkillOpt)) {
            return buildNoOpResult();
        }

        final PreemptiveParty currentPreemptive = state.getPreemptiveParty();
        final List<String> combatLines = new ArrayList<>();
        final SkillType monsterAction =
                currentPreemptive == PreemptiveParty.MONSTER
                        ? SkillType.NORMAL
                        : (state.getCurrentMonsterIntent() != null
                                ? state.getCurrentMonsterIntent()
                                : monsterAiService.nextAction());

        final TurnCombatResult combat;
        final boolean castFailure;
        PreemptiveParty nextPreemptive = PreemptiveParty.NONE;

        if (currentPreemptive == PreemptiveParty.MONSTER) {
            final int monsterDamage =
                    skillCombatResolver.resolveMonsterPreemptiveDamage(progress, monster);
            combat = TurnCombatResult.monsterPreemptive(monsterDamage);
            castFailure = false;
        } else {
            final ResourceKind resourceKind = skill.talent().resourceKind();
            final int resourceCost = combatStatResolver.resolveResourceCost(skill, progress);

            if (!combatStatResolver.hasEnoughResource(progress, resourceKind, resourceCost)) {
                final String lackMsg =
                        gameMessageService.get("system.resource_lack", resourceKind.label());
                actionLog.add(lackMsg, LOG_TYPE_COMBAT);
                return buildInsufficientResult(skill, resourceKind);
            }

            combatStatResolver.deductResource(progress, resourceKind, resourceCost);
            castFailure = combatStatResolver.checkMagicCastFailure(skill, combatLines);

            if (!castFailure) {
                final SkillTalent equippedTalent =
                        skill.talent() == SkillTalent.COMMON ? SkillTalent.MELEE : skill.talent();
                final boolean isPlayerPreemptive =
                        currentPreemptive == PreemptiveParty.PLAYER
                                || skillCombatResolver.isBowFirstStrike(state, equippedTalent);

                final SkillCombatContext context =
                        new SkillCombatContext(
                                skill,
                                progress,
                                monster,
                                state,
                                ownedSkillOpt,
                                combatLines,
                                equippedTalent,
                                isPlayerPreemptive,
                                monsterAction);
                final SkillCombatOutcome outcome = skillCombatResolver.resolveSkillCombat(context);
                final int amplifiedDmg =
                        applyNextAttackAmp(state, equippedTalent, outcome.combat().playerDamage());
                combat = outcome.combat().withPlayerDamage(amplifiedDmg);
                nextPreemptive = outcome.nextPreemptive();

                skillCombatResolver.applySpecialSkillSideEffects(
                        progress,
                        state,
                        skill,
                        ownedSkillOpt,
                        monster,
                        equippedTalent,
                        combatLines);
            } else {
                final int monsterDamage =
                        skillCombatResolver.resolveMonsterOnlyDamage(
                                progress, monster, monsterAction);
                combat = TurnCombatResult.monsterOnly(monsterDamage);
            }
        }

        turnEffectApplier.applyManaShieldAndDamage(
                progress, state, combat.playerDamage(), combat.monsterDamage(), combatLines);
        combatLines.addAll(
                logFormatter.combatLines(
                        new BattleLogInput(
                                skill.label(),
                                skill.type(),
                                monster.name(),
                                monsterAction,
                                combat.playerDamage(),
                                combat.monsterDamage(),
                                combat.playerCritical(),
                                combat.firstStrike(),
                                castFailure,
                                combat.playerHits())));

        applyEndOfTurnEffects(progress, state, monster, skill.type() != SkillType.DOT, combatLines);
        postProcessSkillUsage(progress, skillId, skill, castFailure, currentPreemptive);

        final boolean monsterKilled = state.getMonsterCurrentHp() <= 0;
        final List<String> settlementLines = new ArrayList<>();
        final KillResolution resolution =
                resolveSettlement(progress, state, monster, combatLines, settlementLines);
        final long experienceGained = monsterKilled ? monster.experience() : 0L;

        finalizeTurn(state, progress, nextPreemptive);
        settlementLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));

        return turnResultFactory.combat(
                skill.type(),
                combat.playerDamage(),
                monsterAction,
                combat.monsterDamage(),
                combat.playerCritical(),
                combat.monsterCritical(),
                combat.blocked(),
                combat.countered(),
                castFailure,
                combat.firstStrike(),
                resolution.outcome(),
                resolution.reward(),
                experienceGained,
                combat.playerHits(),
                combatLines,
                resolution.dungeonClearResult());
    }

    /**
     * 도망을 시도한다.
     *
     * <p>50% 확률로 성공하며, 성공 시 전투를 종료하고 실패 시 몬스터의 일방 공격을 받은 뒤 전투를 계속한다. 도망 실패로 HP가 0이 되면 사망 처리를 수행한다.
     *
     * <p>도망 성공 메시지는 하단 {@code actionLog}에 추가하고, 도망 실패(몬스터 피해) 메시지는 중앙 {@code combatLines}에만 담는다.
     *
     * @param progress 캐릭터 진행 상태
     * @param state 현재 전투 상태
     * @return 도망 시도 결과
     */
    @Transactional
    public BattleTurnResult flee(final CharacterProgress progress, final BattleState state) {
        final Optional<Monster> monsterOpt = monsterService.byId(state.getMonsterId());
        if (monsterOpt.isEmpty()) {
            return safeTerminateAndReturn(state);
        }
        final Monster monster = monsterOpt.get();
        final int fleeRate = gameProperties.battle().fleeSuccessRate();
        final boolean success = random.nextInt(PERCENT_DIVISOR) < fleeRate;
        final List<String> combatLines = new ArrayList<>();

        if (success) {
            return handleFleeSuccess(state, combatLines);
        }
        return handleFleeFailure(progress, state, monster, combatLines);
    }

    /**
     * 활성 전투를 재개하기 위해 조회한다.
     *
     * <p>활성 전투가 있으면 몬스터 카탈로그 존재를 검증하고, 소실된 경우 안전 종료 후 빈 {@code Optional}을 반환한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 활성 전투 상태, 없거나 소실 시 빈 {@code Optional}
     */
    @Transactional
    public Optional<BattleState> resumeIfActive(final CharacterProgress progress) {
        final Optional<BattleState> stateOpt =
                battleStateRepository.findByCharacterIdAndActiveTrue(progress.getId());
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }
        final BattleState state = stateOpt.get();
        final Optional<Monster> monsterOpt = monsterService.byId(state.getMonsterId());
        if (monsterOpt.isEmpty()) {
            state.setActive(false);
            battleStateRepository.save(state);
            return Optional.empty();
        }
        return Optional.of(state);
    }

    /**
     * 현재 착용 무기 기준의 전투 스킬 목록을 반환한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 전투에서 사용 가능한 스킬 버튼 목록
     */
    public List<BattleSkillButton> combatSkills(final CharacterProgress progress) {
        return inventoryService.combatSkills(progress);
    }

    /**
     * 대치 페이즈에서 무기 세트를 전환(스왑)한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 무기 교체 성공 시 true, 교체할 무기가 없으면 false
     */
    @Transactional
    public boolean swapWeapon(final CharacterProgress progress) {
        return inventoryService.swapWeapon(progress);
    }

    // ─── Private: timeout ───────────────────────────────────────────────────

    private BattleTurnResult handleTimeoutTurn(
            final CharacterProgress progress, final BattleState state, final Monster monster) {
        final List<String> combatLines = new ArrayList<>();
        final SkillType monsterIntent = state.getCurrentMonsterIntent();
        final int monsterDamage;
        final SkillType monsterAction;

        if (monsterIntent == null) {
            final String timeoutPreemptiveMsg = gameMessageService.get("battle.timeout.preemptive");
            combatLines.add(timeoutPreemptiveMsg);
            monsterDamage = 0;
            monsterAction = null;
        } else {
            monsterAction = monsterIntent;
            final String timeoutHitMsg = gameMessageService.get("battle.timeout.hit");
            combatLines.add(timeoutHitMsg);
            monsterDamage =
                    skillCombatResolver.resolveMonsterOnlyDamage(progress, monster, monsterAction);
            turnEffectApplier.applyManaShieldAndDamage(
                    progress, state, 0, monsterDamage, combatLines);

            if (monsterAction != SkillType.DEFENSE) {
                final String actionLabel = monsterAction == SkillType.HEAVY ? "강공격" : "일반공격";
                combatLines.add(
                        "["
                                + monster.name()
                                + "] "
                                + actionLabel
                                + " ➔ "
                                + monsterDamage
                                + " 피해 피격");
            } else {
                final String defHoldMsg =
                        gameMessageService.get("battle.monster.defense_hold", monster.name());
                combatLines.add(defHoldMsg);
            }
        }

        Outcome outcome = Outcome.NONE;
        final List<String> settlementLines = new ArrayList<>();
        if (progress.isDead()) {
            outcome = battleRewardService.handleDeath(progress, state, settlementLines);
        }

        finalizeTurn(state, progress, PreemptiveParty.NONE);

        settlementLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));

        return turnResultFactory.monsterOnly(monsterAction, monsterDamage, outcome, combatLines);
    }

    // ─── Private: flee ──────────────────────────────────────────────────────

    private BattleTurnResult handleFleeSuccess(
            final BattleState state, final List<String> combatLines) {
        state.setActive(false);
        battleStateRepository.save(state);
        final String fleeSuccessMsg = gameMessageService.get("battle.flee.success");
        actionLog.add(fleeSuccessMsg, LOG_TYPE_COMBAT);
        return turnResultFactory.terminal(Outcome.FLED, combatLines);
    }

    private BattleTurnResult handleFleeFailure(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final List<String> combatLines) {
        final int monsterDmg =
                skillCombatResolver.resolveMonsterOnlyDamage(progress, monster, SkillType.NORMAL);
        progress.damageHp(monsterDmg);

        Outcome outcome = Outcome.NONE;
        if (progress.isDead()) {
            final List<String> deathLines = new ArrayList<>();
            outcome = battleRewardService.handleDeath(progress, state, deathLines);
            deathLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));
        }

        final String fleeFailMsg =
                gameMessageService.get("battle.flee.fail", monster.name(), monsterDmg);
        combatLines.add(fleeFailMsg);

        finalizeTurn(state, progress, PreemptiveParty.NONE);

        return turnResultFactory.monsterOnly(SkillType.NORMAL, monsterDmg, outcome, combatLines);
    }

    // ─── Private: safe terminate / no-op results ────────────────────────────

    private BattleTurnResult safeTerminateAndReturn(final BattleState state) {
        state.setActive(false);
        battleStateRepository.save(state);
        return turnResultFactory.terminal(Outcome.NONE, List.of());
    }

    private BattleTurnResult buildNoOpResult() {
        return turnResultFactory.noOp();
    }

    private BattleTurnResult buildInsufficientResult(
            final Skill skill, final ResourceKind insufficientKind) {
        return turnResultFactory.insufficient(skill, insufficientKind);
    }

    /** 턴 종료 상태 확정(중복 통합): takeTurn/타임아웃/도망 실패 3곳이 공유. */
    private void finalizeTurn(
            final BattleState state,
            final CharacterProgress progress,
            final PreemptiveParty nextPreemptive) {
        state.setPreemptiveParty(nextPreemptive);
        state.setCurrentMonsterIntent(null);
        state.setStandby(true);
        state.setTurnCount(state.getTurnCount() + 1);
        battleStateRepository.save(state);
        characterService.saveTurn(progress);
    }

    private int applyNextAttackAmp(
            final BattleState state, final SkillTalent equippedTalent, final int playerDamage) {
        if (playerDamage > 0
                && state.getNextAttackAmpPercent() > 0
                && (equippedTalent == SkillTalent.MELEE || equippedTalent == SkillTalent.ARCHERY)) {
            final int amplified = (playerDamage * (100 + state.getNextAttackAmpPercent())) / 100;
            state.setNextAttackAmpPercent(0);
            return amplified;
        }
        return playerDamage;
    }

    private void postProcessSkillUsage(
            final CharacterProgress progress,
            final String skillId,
            final Skill skill,
            final boolean castFailure,
            final PreemptiveParty currentPreemptive) {
        if (currentPreemptive != PreemptiveParty.MONSTER) {
            skillService.onSkillUsed(progress.getId(), skillId);

            if (skill.type() != SkillType.DEFENSE && !castFailure) {
                inventoryService.reduceDurabilityAndAutoUnequip(
                        progress, gameProperties.battle().durabilityPerAttack());
            }
        }
    }

    private KillResolution resolveSettlement(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final List<String> combatLines,
            final List<String> settlementLines) {
        if (state.getMonsterCurrentHp() <= 0) {
            final KillResolution killResolution =
                    battleRewardService.handleMonsterKilled(
                            progress, state, monster, combatLines, settlementLines);
            if (killResolution.outcome() == Outcome.WIN) {
                skillService.onBattleWon(progress.getId());
            }
            return killResolution;
        }
        if (progress.isDead()) {
            final Outcome deathOutcome =
                    battleRewardService.handleDeath(progress, state, settlementLines);
            return new KillResolution(deathOutcome, null, null);
        }
        return new KillResolution(Outcome.NONE, null, null);
    }

    private boolean checkUltimateCooldown(
            final Skill skill, final Optional<CharacterSkill> ownedSkillOpt) {
        if (isUltimateOnCooldown(skill, ownedSkillOpt)) {
            final String cdMsg =
                    gameMessageService.get(
                            "log.combat.ultimate_cooldown",
                            ownedSkillOpt.get().getUltimateCooldown());
            actionLog.add(cdMsg, LOG_TYPE_COMBAT);
            return true;
        }
        return false;
    }

    private void applyEndOfTurnEffects(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final boolean applyDoTTick,
            final List<String> combatLines) {
        if (applyDoTTick) {
            turnEffectApplier.applyDoTTick(state, monster, combatLines);
        }
        turnEffectApplier.applyMeditationRegen(progress, combatLines);
    }

    private boolean isUltimateOnCooldown(
            final Skill skill, final Optional<CharacterSkill> ownedSkillOpt) {
        return skill.type() == SkillType.ULTIMATE
                && ownedSkillOpt.isPresent()
                && ownedSkillOpt.get().getUltimateCooldown() > 0;
    }
}
