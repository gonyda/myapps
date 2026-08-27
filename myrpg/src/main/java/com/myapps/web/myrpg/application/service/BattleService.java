package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillDamagePolicy;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TurnInput;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.BattleStateRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.service.BattleResolver;
import com.myapps.web.myrpg.domain.service.RockPaperScissors;
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
@SuppressWarnings("PMD.CyclomaticComplexity")
public class BattleService {

    private static final double MELEE_COEF = 1.0;
    private static final double ARCHERY_COEF = 0.85;
    private static final double MAGIC_COEF = 1.2;
    private static final int MAGIC_FAIL_PERCENT = 10;
    private static final int FLEE_SUCCESS_PERCENT = 50;
    private static final double DURABILITY_PER_ATTACK = 0.05;
    private static final int MONSTER_NORMAL_MULTIPLIER = 100;
    private static final int MONSTER_HEAVY_MULTIPLIER = 150;
    private static final int PERCENT_DIVISOR = 100;
    private static final int CRITICAL_ROLL_MAX = 1000;
    private static final String LOG_TYPE_COMBAT = "combat";
    private static final String LOG_TYPE_GROWTH = "growth";
    private static final int NORMAL_CLASH_DURATION_MS = 1000;
    private static final int DEFAULT_CLASH_DURATION_MS = 1500;
    private static final String BADGE_LABEL_NORMAL = "⚡ 일반공격 태세";
    private static final String BADGE_LABEL_HEAVY = "💥 강공격 차징 중!";
    private static final String BADGE_LABEL_DEFENSE = "🛡️ 방어 태세";
    private static final String BADGE_LABEL_BOW_FIRST_STRIKE = "🏹 선제 사격 기회!";
    private static final String BADGE_LABEL_PREEMPTIVE_PLAYER = "⚡ 선제 공격 찬스!";
    private static final String BADGE_LABEL_PREEMPTIVE_MONSTER = "⚠️ 몬스터의 확정 선제 일반공격!";
    private static final String BADGE_CLASS_NORMAL = "badge-stance-normal";
    private static final String BADGE_CLASS_HEAVY = "badge-stance-heavy";
    private static final String BADGE_CLASS_DEFENSE = "badge-stance-defense";
    private static final String BADGE_CLASS_PREEMPTIVE_PLAYER = "badge-stance-preemptive-player";
    private static final String BADGE_CLASS_PREEMPTIVE_MONSTER = "badge-stance-preemptive-monster";

    private final BattleStateRepository battleStateRepository;
    private final BattleResolver resolver;
    private final MonsterService monsterService;
    private final MonsterAiService monsterAiService;
    private final MonsterRewardService monsterRewardService;
    private final SkillService skillService;
    private final SkillDamagePolicy skillDamagePolicy;
    private final BattleLogFormatter logFormatter;
    private final InventoryService inventoryService;
    private final ProgressionService progressionService;
    private final CharacterService characterService;
    private final StatProgression statProgression;
    private final ActionLog actionLog;
    private final Random random;
    private final SkillCatalogService skillCatalogService;
    private final CharacterSkillRepository characterSkillRepository;
    private final ItemCatalogService itemCatalogService;
    private final DungeonService dungeonService;

    /**
     * BattleService를 생성한다.
     *
     * @param battleStateRepository 전투 상태 리포지토리
     * @param resolver 순수 데미지 계산 서비스
     * @param monsterService 몬스터 카탈로그 서비스
     * @param monsterAiService 몬스터 AI 서비스
     * @param monsterRewardService 몬스터 보상 서비스
     * @param skillService 스킬 시스템 서비스
     * @param inventoryService 인벤토리 서비스
     * @param progressionService 경험치/사망 서비스
     * @param characterService 캐릭터 저장 서비스
     * @param statProgression 레벨 스탯 계산 정책
     * @param actionLog 활동 로그
     * @param random 난수 생성기
     * @param skillCatalogService 스킬 카탈로그 서비스
     * @param characterSkillRepository 캐릭터 보유 스킬 리포지토리
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param dungeonService 던전 관리 서비스
     */
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
        this.battleStateRepository = battleStateRepository;
        this.resolver = resolver;
        this.monsterService = monsterService;
        this.monsterAiService = monsterAiService;
        this.monsterRewardService = monsterRewardService;
        this.skillService = skillService;
        this.skillDamagePolicy = new SkillDamagePolicy();
        this.logFormatter = new BattleLogFormatter();
        this.inventoryService = inventoryService;
        this.progressionService = progressionService;
        this.characterService = characterService;
        this.statProgression = statProgression;
        this.actionLog = actionLog;
        this.random = random;
        this.skillCatalogService = skillCatalogService;
        this.characterSkillRepository = characterSkillRepository;
        this.itemCatalogService = itemCatalogService;
        this.dungeonService = dungeonService;
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

        return buildClashBattleView(
                state, monster, progress, intent, bowFirstStrike, preemptiveParty);
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
    @SuppressWarnings({"PMD.NcssCount", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
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

        if (skill instanceof UltimateSkill
                && ownedSkillOpt.isPresent()
                && ownedSkillOpt.get().getUltimateCooldown() > 0) {
            actionLog.add(
                    "궁극기 쿨타임 대기 중입니다. (" + ownedSkillOpt.get().getUltimateCooldown() + "승 남음)",
                    LOG_TYPE_COMBAT);
            return buildNoOpResult();
        }

        final PreemptiveParty currentPreemptive = state.getPreemptiveParty();
        final List<String> combatLines = new ArrayList<>();

        int playerDamage = 0;
        int monsterDamage = 0;
        boolean playerCritical = false;
        boolean monsterCritical = false;
        boolean blocked = false;
        boolean countered = false;
        boolean firstStrike = false;
        boolean castFailure = false;
        List<HitResult> playerHits = List.of();
        SkillType monsterAction =
                state.getCurrentMonsterIntent() != null
                        ? state.getCurrentMonsterIntent()
                        : monsterAiService.nextAction();
        PreemptiveParty nextPreemptive = PreemptiveParty.NONE;

        if (currentPreemptive == PreemptiveParty.MONSTER) {
            monsterAction = SkillType.NORMAL;
            monsterDamage = resolveMonsterPreemptiveDamage(progress, monster);
            firstStrike = true;
        } else {
            final ResourceKind resourceKind = skill.talent().resourceKind();
            final int resourceCost = resolveResourceCost(skill, progress);

            if (!hasEnoughResource(progress, resourceKind, resourceCost)) {
                actionLog.add(resourceKind.label() + "이(가) 부족합니다.", LOG_TYPE_COMBAT);
                return buildInsufficientResult(skill, resourceKind);
            }

            deductResource(progress, resourceKind, resourceCost);
            castFailure = checkMagicCastFailure(skill, combatLines);

            if (!castFailure) {
                final SkillTalent equippedTalent =
                        skill.talent() == SkillTalent.COMMON ? SkillTalent.MELEE : skill.talent();
                final boolean isPlayerPreemptive =
                        currentPreemptive == PreemptiveParty.PLAYER
                                || isBowFirstStrike(state, equippedTalent);

                if (skill instanceof UltimateSkill ultimateSkill && ownedSkillOpt.isPresent()) {
                    final TurnCombatResult combat =
                            resolveUltimateCombat(
                                    progress, monster, ultimateSkill, ownedSkillOpt.get());
                    playerDamage = combat.playerDamage;
                    playerCritical = combat.playerCritical;
                    firstStrike = true;
                    playerHits = combat.playerHits;
                } else if (skill instanceof RecoverySkill recoverySkill
                        && ownedSkillOpt.isPresent()) {
                    final TurnCombatResult combat =
                            resolveRecoveryCombat(
                                    progress,
                                    monster,
                                    recoverySkill,
                                    ownedSkillOpt.get(),
                                    state,
                                    combatLines,
                                    isPlayerPreemptive ? null : monsterAction);
                    monsterDamage = combat.monsterDamage;
                    firstStrike = isPlayerPreemptive;
                } else if (skill instanceof BuffSkill buffSkill && ownedSkillOpt.isPresent()) {
                    final TurnCombatResult combat =
                            resolveBuffCombat(
                                    progress,
                                    monster,
                                    buffSkill,
                                    ownedSkillOpt.get(),
                                    state,
                                    combatLines,
                                    isPlayerPreemptive ? null : monsterAction);
                    monsterDamage = combat.monsterDamage;
                    firstStrike = isPlayerPreemptive;
                } else if (skill instanceof CcSkill ccSkill && ownedSkillOpt.isPresent()) {
                    final TurnCombatResult combat =
                            resolveCcCombat(
                                    progress,
                                    monster,
                                    ccSkill,
                                    ownedSkillOpt.get(),
                                    state,
                                    combatLines,
                                    isPlayerPreemptive ? null : monsterAction);
                    monsterDamage = combat.monsterDamage;
                    firstStrike = isPlayerPreemptive;
                } else if (isPlayerPreemptive) {
                    final TurnCombatResult combat =
                            resolvePlayerPreemptive(progress, monster, skill, equippedTalent);
                    playerDamage = combat.playerDamage;
                    playerCritical = combat.playerCritical;
                    firstStrike = true;
                    playerHits = combat.playerHits;
                } else {
                    final TurnCombatResult combat =
                            resolveNormalCombat(
                                    progress, monster, skill, equippedTalent, monsterAction);
                    playerDamage = combat.playerDamage;
                    monsterDamage = combat.monsterDamage;
                    playerCritical = combat.playerCritical;
                    monsterCritical = combat.monsterCritical;
                    blocked = combat.blocked;
                    countered = combat.countered;
                    firstStrike = combat.firstStrike;
                    playerHits = combat.playerHits;

                    if (!firstStrike && playerDamage > 0 && monsterDamage > 0) {
                        final boolean playerFirst = determineTurnOrder(skill.type(), monsterAction);
                        if (playerFirst && playerDamage >= state.getMonsterCurrentHp()) {
                            monsterDamage = 0;
                        } else if (!playerFirst && monsterDamage >= progress.getHpCurrent()) {
                            playerDamage = 0;
                        }
                    }

                    if ("defense".equals(skill.id())
                            && RockPaperScissors.isNormalFamily(monsterAction)) {
                        nextPreemptive = PreemptiveParty.PLAYER;
                    } else if (RockPaperScissors.isNormalFamily(skill.type())
                            && monsterAction == SkillType.DEFENSE) {
                        nextPreemptive = PreemptiveParty.MONSTER;
                    }
                }

                if (playerDamage > 0
                        && state.getNextAttackAmpPercent() > 0
                        && (equippedTalent == SkillTalent.MELEE
                                || equippedTalent == SkillTalent.ARCHERY)) {
                    playerDamage = (playerDamage * (100 + state.getNextAttackAmpPercent())) / 100;
                    state.setNextAttackAmpPercent(0);
                }

                applySpecialSkillSideEffects(
                        progress,
                        state,
                        skill,
                        ownedSkillOpt,
                        monster,
                        equippedTalent,
                        combatLines);
            } else {
                monsterDamage = resolveMonsterOnlyDamage(progress, monster, monsterAction);
            }
        }

        applyManaShieldAndDamage(progress, state, playerDamage, monsterDamage, combatLines);
        final BattleLogInput logInput =
                new BattleLogInput(
                        skill.label(),
                        skill.type(),
                        monster.name(),
                        monsterAction,
                        playerDamage,
                        monsterDamage,
                        playerCritical,
                        firstStrike,
                        castFailure,
                        playerHits);
        combatLines.addAll(logFormatter.combatLines(logInput));

        if (skill.type() != SkillType.DOT) {
            applyDoTTick(state, monster, combatLines);
        }
        applyMeditationRegen(progress, combatLines);

        final boolean monsterKilled = state.getMonsterCurrentHp() <= 0;
        if (currentPreemptive != PreemptiveParty.MONSTER) {
            skillService.onSkillUsed(progress.getId(), skillId);
            if (monsterKilled && skill.type() != SkillType.DEFENSE) {
                skillService.onSkillKill(progress.getId(), skillId);
            }

            if (skill.type() != SkillType.DEFENSE && !castFailure) {
                inventoryService.reduceDurabilityAndAutoUnequip(progress, DURABILITY_PER_ATTACK);
            }
        }

        DropResult reward = null;
        long experienceGained = 0;
        Outcome outcome = Outcome.NONE;
        DungeonClearResult dungeonClearResult = null;
        final List<String> settlementLines = new ArrayList<>();

        if (monsterKilled) {
            reward = processKillReward(progress, monster, settlementLines);
            experienceGained = monster.experience();
            final KillResolution killResolution =
                    handleMonsterKilled(progress, state, monster, combatLines, settlementLines);
            outcome = killResolution.outcome();
            dungeonClearResult = killResolution.dungeonClearResult();
            if (outcome == Outcome.WIN) {
                skillService.onBattleWon(progress.getId());
            }
        } else if (progress.isDead()) {
            outcome = handleDeath(progress, state, settlementLines);
        }

        state.setPreemptiveParty(nextPreemptive);
        state.setCurrentMonsterIntent(null);
        state.setStandby(true);
        state.setTurnCount(state.getTurnCount() + 1);
        battleStateRepository.save(state);
        characterService.saveTurn(progress);

        settlementLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));

        return new BattleTurnResult(
                skill.type(),
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
        final boolean success = random.nextInt(PERCENT_DIVISOR) < FLEE_SUCCESS_PERCENT;
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

    // ─── Private: attack power ──────────────────────────────────────────────

    /**
     * 착용 무기 재능·주스탯·장비/스킬 보너스로 공격력을 산출한다.
     *
     * <p>공격력 = round(주스탯 × 재능계수). 주스탯은 레벨 스탯 + 장비 보너스 + 스킬 랭크업 보너스에서 재능별 대응 필드를 추출한다. 재능계수: 근접 1.0,
     * 활 0.85, 마법 1.2.
     *
     * <p>밸런싱 튜닝값(재능계수)은 구현·밸런싱 단계에서 미세 조정 가능하다.
     */
    private int attackPower(final CharacterProgress progress, final SkillTalent equippedTalent) {
        final Stats levelStats =
                statProgression.levelStatsFor(progress.getCurrentLevel(), progress.getTalent());
        final EquippedBonusResult equipBonus = resolveEquippedBonus(progress);
        final Stats skillBonus = skillService.rankupBonus(progress.getId());

        final int totalStr = levelStats.str() + equipBonus.statBonus().str() + skillBonus.str();
        final int totalDex = levelStats.dex() + equipBonus.statBonus().dex() + skillBonus.dex();
        final int totalInt =
                levelStats.intelligence()
                        + equipBonus.statBonus().intelligence()
                        + skillBonus.intelligence();

        final int primaryStat = resolvePrimaryStat(equippedTalent, totalStr, totalDex, totalInt);
        final double coefficient = resolveCoefficient(equippedTalent);

        return (int) Math.round(primaryStat * coefficient);
    }

    private int resolvePrimaryStat(
            final SkillTalent talent, final int str, final int dex, final int intelligence) {
        return switch (talent) {
            case MELEE, COMMON -> str;
            case ARCHERY -> dex;
            case MAGIC -> intelligence;
        };
    }

    private double resolveCoefficient(final SkillTalent talent) {
        return switch (talent) {
            case MELEE, COMMON -> MELEE_COEF;
            case ARCHERY -> ARCHERY_COEF;
            case MAGIC -> MAGIC_COEF;
        };
    }

    // ─── Private: resource check ────────────────────────────────────────────

    private boolean hasEnoughResource(
            final CharacterProgress progress, final ResourceKind kind, final int cost) {
        return switch (kind) {
            case STAMINA -> progress.getStaminaCurrent() >= cost;
            case MP -> progress.getMpCurrent() >= cost;
        };
    }

    private void deductResource(
            final CharacterProgress progress, final ResourceKind kind, final int cost) {
        if (kind == ResourceKind.STAMINA) {
            progress.spendStamina(cost);
        } else {
            progress.spendMp(cost);
        }
    }

    private boolean checkMagicCastFailure(final Skill skill, final List<String> logLines) {
        if (skill.talent() != SkillTalent.MAGIC) {
            return false;
        }
        if (skill.type() == SkillType.DEFENSE) {
            return false;
        }
        final boolean failed = random.nextInt(PERCENT_DIVISOR) < MAGIC_FAIL_PERCENT;
        if (failed) {
            logLines.add("🔮 [" + skill.label() + "] 캐스팅 실패! (집중이 흐트러짐)");
        }
        return failed;
    }

    // ─── Private: combat resolution ─────────────────────────────────────────

    private TurnCombatResult resolveCombat(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final Skill skill,
            final SkillTalent equippedTalent,
            final SkillType monsterAction) {
        if (isBowFirstStrike(state, equippedTalent)) {
            return resolvePlayerPreemptive(progress, monster, skill, equippedTalent);
        }
        return resolveNormalCombat(progress, monster, skill, equippedTalent, monsterAction);
    }

    private boolean isBowFirstStrike(final BattleState state, final SkillTalent equippedTalent) {
        return state.getTurnCount() == 1 && equippedTalent == SkillTalent.ARCHERY;
    }

    private TurnCombatResult resolvePlayerPreemptive(
            final CharacterProgress progress,
            final Monster monster,
            final Skill skill,
            final SkillTalent equippedTalent) {
        if (skill.type() == SkillType.DEFENSE) {
            return new TurnCombatResult(0, 0, false, false, false, false, true, List.of());
        }

        final int playerAttack = attackPower(progress, equippedTalent);
        final int multiplier = resolvePlayerMultiplier(skill, progress);
        final int effectiveCritical = resolveEffectivePlayerCritical(skill, progress);
        final int hitCount = resolvePlayerHitCount(skill);

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

    private int resolveMonsterPreemptiveDamage(
            final CharacterProgress progress, final Monster monster) {
        final int playerDefense = resolvePlayerDefense(progress);
        final int monsterBase =
                resolver.baseDamage(
                        monster.attackPower(), MONSTER_NORMAL_MULTIPLIER, playerDefense);
        final boolean monsterCrit = resolver.rollCritical(monster.critical());
        return resolver.finalDamage(monsterBase, 1.0, monsterCrit);
    }

    private TurnCombatResult resolveNormalCombat(
            final CharacterProgress progress,
            final Monster monster,
            final Skill skill,
            final SkillTalent equippedTalent,
            final SkillType monsterAction) {
        final int playerAttack = attackPower(progress, equippedTalent);
        final int playerMultiplier = resolvePlayerMultiplier(skill, progress);
        final int playerDefense = resolvePlayerDefense(progress);
        final int playerCritical = resolveEffectivePlayerCritical(skill, progress);
        final int playerBlockRate = resolvePlayerBlockRate(skill, progress);
        final int playerCounterPercent = resolvePlayerCounterPercent(skill, progress);
        final int playerHitCount = resolvePlayerHitCount(skill);
        final int monsterMultiplier =
                monsterAction == SkillType.HEAVY
                        ? MONSTER_HEAVY_MULTIPLIER
                        : MONSTER_NORMAL_MULTIPLIER;

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
                        monsterMultiplier,
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

    // ─── Private: turn order ────────────────────────────────────────────────

    /**
     * 선후공을 결정한다.
     *
     * <p>동일 타입 무승부: 50:50 랜덤. 일반↔방어(방어 승): 결정론적으로 공격자(일반)가 먼저. 그 외(한쪽만 피해 있는 경우): 피해를 주는 쪽이 먼저.
     *
     * @return {@code true}면 플레이어가 먼저 공격
     */
    private boolean determineTurnOrder(final SkillType playerType, final SkillType monsterType) {
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

    // ─── Private: stat resolution ───────────────────────────────────────────

    private int resolveResourceCost(final Skill skill, final CharacterProgress progress) {
        final Optional<CharacterSkill> csOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id());
        if (csOpt.isPresent()) {
            final SkillRank rank = csOpt.get().getRank();
            if (skill instanceof DefenseSkill defenseSkill) {
                return defenseSkill.resourceCostAt(rank);
            }
            if (skill instanceof RecoverySkill recoverySkill) {
                return recoverySkill.resourceCostAt(rank);
            }
        }
        return skill.resourceCost();
    }

    private int resolvePlayerMultiplier(final Skill skill, final CharacterProgress progress) {
        final Optional<CharacterSkill> csOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id());
        if (csOpt.isEmpty()) {
            return MONSTER_NORMAL_MULTIPLIER;
        }
        final CharacterSkill cs = csOpt.get();
        if (skill instanceof DamageSkill damageSkill) {
            return skillDamagePolicy.multiplier(damageSkill, cs.getRank());
        }
        if (skill instanceof DotSkill dotSkill) {
            return dotSkill.initialMultiplierAt(cs.getRank());
        }
        return MONSTER_NORMAL_MULTIPLIER;
    }

    private int resolvePlayerBlockRate(final Skill skill, final CharacterProgress progress) {
        if (!(skill instanceof DefenseSkill defenseSkill)) {
            return 0;
        }
        final Optional<CharacterSkill> csOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id());
        if (csOpt.isEmpty()) {
            return 0;
        }
        return skillDamagePolicy.blockRate(defenseSkill, csOpt.get().getRank());
    }

    private int resolvePlayerCounterPercent(final Skill skill, final CharacterProgress progress) {
        if (!(skill instanceof DefenseSkill defenseSkill)) {
            return 0;
        }
        final Optional<CharacterSkill> csOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id());
        if (csOpt.isEmpty()) {
            return 0;
        }
        return skillDamagePolicy.counterMultiplier(defenseSkill, csOpt.get().getRank());
    }

    private int resolvePlayerDefense(final CharacterProgress progress) {
        final Stats levelStats =
                statProgression.levelStatsFor(progress.getCurrentLevel(), progress.getTalent());
        final EquippedBonusResult equipBonus = resolveEquippedBonus(progress);
        final Stats skillBonus = skillService.rankupBonus(progress.getId());
        return levelStats.defense() + equipBonus.statBonus().defense() + skillBonus.defense();
    }

    private int resolvePlayerCritical(final CharacterProgress progress) {
        final Stats levelStats =
                statProgression.levelStatsFor(progress.getCurrentLevel(), progress.getTalent());
        final EquippedBonusResult equipBonus = resolveEquippedBonus(progress);
        final Stats skillBonus = skillService.rankupBonus(progress.getId());
        return levelStats.critical() + equipBonus.statBonus().critical() + skillBonus.critical();
    }

    private EquippedBonusResult resolveEquippedBonus(final CharacterProgress progress) {
        if (progress == null || progress.getId() == null || progress.getId().equals(1L)) {
            final EquippedBonusResult bonus = inventoryService.equippedBonus();
            if (bonus != null) {
                return bonus;
            }
        }
        final EquippedBonusResult bonus =
                inventoryService.equippedBonus(progress != null ? progress.getId() : null);
        return bonus != null ? bonus : new EquippedBonusResult(Stats.ZERO, new VitalMax(0, 0, 0));
    }

    /**
     * 스킬의 크리 보너스를 반영한 실효 크리티컬 수치를 산출한다.
     *
     * <p>딜 스킬의 경우 스킬의 {@code critBonus}를, 카운터 어택 등 방어 스킬의 경우 랭크별 {@code critBonusByRank}를 캐릭터 크리티컬에
     * 가산하고, 상한({@code CRITICAL_ROLL_MAX = 1000})을 초과하지 않도록 보정한다.
     *
     * @param skill 플레이어가 사용하는 스킬
     * @param progress 캐릭터 진행 상태
     * @return 실효 크리티컬 수치 (0~1000)
     */
    private int resolveEffectivePlayerCritical(
            final Skill skill, final CharacterProgress progress) {
        final int baseCritical = resolvePlayerCritical(progress);
        int bonus = 0;
        if (skill instanceof DamageSkill ds) {
            bonus = ds.critBonus();
        } else if (skill instanceof DefenseSkill defSkill) {
            final Optional<CharacterSkill> csOpt =
                    characterSkillRepository.findByCharacterIdAndSkillId(
                            progress.getId(), skill.id());
            if (csOpt.isPresent()) {
                bonus = defSkill.critBonusAt(csOpt.get().getRank());
            }
        }
        return Math.min(CRITICAL_ROLL_MAX, baseCritical + bonus);
    }

    /**
     * 스킬의 히트 수를 결정한다.
     *
     * <p>딜 스킬의 경우 스킬에 설정된 {@code hitCount}를 반환하고, 방어 스킬이나 기타 스킬의 경우 1을 반환한다.
     *
     * @param skill 플레이어가 사용하는 스킬
     * @return 히트 수 (1 이상)
     */
    private int resolvePlayerHitCount(final Skill skill) {
        return (skill instanceof DamageSkill ds) ? ds.hitCount() : 1;
    }

    // ─── Private: special skill combat handlers ─────────────────────────────

    private TurnCombatResult resolveUltimateCombat(
            final CharacterProgress progress,
            final Monster monster,
            final UltimateSkill ultimateSkill,
            final CharacterSkill charSkill) {
        final SkillTalent equippedTalent =
                ultimateSkill.talent() == SkillTalent.COMMON
                        ? SkillTalent.MELEE
                        : ultimateSkill.talent();
        final int playerAttack = attackPower(progress, equippedTalent);
        final int multiplier = ultimateSkill.multiplierAt(charSkill.getRank());
        final int effectiveCritical =
                resolveEffectivePlayerCritical(ultimateSkill, progress) + ultimateSkill.critBonus();
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

    private TurnCombatResult resolveRecoveryCombat(
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
        combatLines.add("💖 [" + recoverySkill.label() + "] HP +" + actualHealed + " 회복");

        final int monsterDamage =
                resolveMonsterDamageForSupportTurn(
                        progress, monster, state, combatLines, monsterAction);
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    private TurnCombatResult resolveBuffCombat(
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

    private TurnCombatResult resolveCcCombat(
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
            combatLines.add("⛓️ [" + ccSkill.label() + "] " + monster.name() + " 기절 성공! (1턴)");
        } else {
            combatLines.add("⛓️ [" + ccSkill.label() + "] 저항으로 제어 효과 실패");
        }

        final int monsterDamage =
                resolveMonsterDamageForSupportTurn(
                        progress, monster, state, combatLines, monsterAction);
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    private int resolveMonsterDamageForSupportTurn(
            final CharacterProgress progress,
            final Monster monster,
            final BattleState state,
            final List<String> combatLines,
            final SkillType monsterAction) {
        if (monsterAction == null) {
            return 0;
        }
        if (state.getMonsterStunnedTurns() > 0) {
            combatLines.add("❄️ [" + monster.name() + "] 빙결/기절 상태로 행동 불가");
            state.setMonsterStunnedTurns(state.getMonsterStunnedTurns() - 1);
            return 0;
        }
        return resolveMonsterOnlyDamage(progress, monster, monsterAction);
    }

    private void applySpecialSkillSideEffects(
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

        if (skill instanceof DamageSkill damageSkill) {
            final int freezeRate = damageSkill.freezeRateAt(charSkill.getRank());
            if (freezeRate > 0 && random.nextInt(PERCENT_DIVISOR) < freezeRate) {
                state.setMonsterStunnedTurns(1);
                combatLines.add("❄️ [빙결] " + monster.name() + " 꽁꽁 얼어붙음! (1턴 행동 불가)");
            }
        }

        if (skill instanceof DotSkill dotSkill) {
            final int dotTurns = dotSkill.dotTurnsAt(charSkill.getRank());
            final int dotRate = dotSkill.dotPerTurnAt(charSkill.getRank());
            final int playerAttack = attackPower(progress, equippedTalent);
            final int dotDamage = Math.max(1, (playerAttack * dotRate) / 100);
            state.setDotTurnsLeft(dotTurns);
            state.setDotDamagePerTurn(dotDamage);
            combatLines.add(
                    "🩸 ["
                            + skill.label()
                            + "] "
                            + monster.name()
                            + "에게 지속 피해 표식 ("
                            + dotTurns
                            + "턴)");
        }

        if (skill.type() == SkillType.DEBUFF) {
            state.setNextAttackAmpPercent(30);
            combatLines.add("💢 [레이지 임팩트] 다음 물리 피해 +30% 증폭");
        }
    }

    // ─── Private: monster-only damage (cast failure) ────────────────────────

    private int resolveMonsterOnlyDamage(
            final CharacterProgress progress,
            final Monster monster,
            final SkillType monsterAction) {
        if (monsterAction == SkillType.DEFENSE) {
            return 0;
        }
        final int monsterMultiplier =
                monsterAction == SkillType.HEAVY
                        ? MONSTER_HEAVY_MULTIPLIER
                        : MONSTER_NORMAL_MULTIPLIER;
        final int playerDef = resolvePlayerDefense(progress);
        final int baseDmg =
                resolver.baseDamage(monster.attackPower(), monsterMultiplier, playerDef);
        final boolean crit = resolver.rollCritical(monster.critical());
        return resolver.finalDamage(baseDmg, 1.0, crit);
    }

    // ─── Private: damage application & turn effects ─────────────────────────

    private void applyManaShieldAndDamage(
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

    private void applyDoTTick(
            final BattleState state, final Monster monster, final List<String> combatLines) {
        if (state.getDotTurnsLeft() > 0 && state.getMonsterCurrentHp() > 0) {
            final int dotDmg = state.getDotDamagePerTurn();
            state.setMonsterCurrentHp(Math.max(0, state.getMonsterCurrentHp() - dotDmg));
            state.setDotTurnsLeft(state.getDotTurnsLeft() - 1);
            combatLines.add("🩸 [지속 피해] " + monster.name() + " " + dotDmg + " 도트 피해");
        }
    }

    private void applyMeditationRegen(
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
                    combatLines.add("🧘 [메디테이션] MP +" + actualRegened + " 회복");
                }
            }
        }
    }

    // ─── Private: kill reward ───────────────────────────────────────────────

    private DropResult processKillReward(
            final CharacterProgress progress, final Monster monster, final List<String> logLines) {
        final DropResult drop = monsterRewardService.rollDrop(monster);
        inventoryService.acquire(progress, drop);
        final LevelUpResult levelUpResult =
                progressionService.gainExperience(progress, monster.experience());

        final StringBuilder sb = new StringBuilder();
        sb.append("승리! EXP +").append(monster.experience()).append(" | Gold +").append(drop.gold());

        if (!drop.items().isEmpty()) {
            final List<String> itemSummaries = new ArrayList<>();
            for (final DroppedItem item : drop.items()) {
                final String itemName = resolveItemName(item.itemId());
                itemSummaries.add(itemName + " x" + item.quantity());
            }
            sb.append(" | ").append(String.join(", ", itemSummaries));
        }
        logLines.add(sb.toString());

        if (levelUpResult != null && levelUpResult.levelsGained() > 0) {
            actionLog.add(
                    "🎉 레벨업! Lv."
                            + levelUpResult.newLevel()
                            + " 달성! (AP +"
                            + levelUpResult.levelsGained()
                            + ")",
                    LOG_TYPE_GROWTH);
        }

        return drop;
    }

    private String resolveItemName(final String itemId) {
        return itemCatalogService.byId(itemId).map(Item::name).orElse(itemId);
    }

    private record KillResolution(Outcome outcome, DungeonClearResult dungeonClearResult) {}

    private KillResolution handleMonsterKilled(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final List<String> combatLines,
            final List<String> settlementLines) {
        final Long characterId =
                progress.getId() != null ? progress.getId() : state.getCharacterId();

        final Optional<DungeonInstance> dungeonOpt =
                dungeonService != null
                        ? dungeonService.getActiveDungeon(characterId)
                        : Optional.empty();

        if (dungeonOpt.isPresent()) {
            final DungeonInstance dungeon = dungeonOpt.get();
            final boolean isBoss =
                    dungeon.bossRoomId().equals(dungeon.currentRoomId())
                            || "boss".equals(monster.type())
                            || "giant-spider".equals(monster.id());

            if (isBoss) {
                final DungeonClearResult clearResult = dungeonService.onBossDefeated(characterId);
                state.setActive(false);
                return new KillResolution(Outcome.WIN, clearResult);
            }

            if (!state.isDungeonMonsterDeducted()) {
                dungeonService.onMonsterDefeated(characterId, monster.id());
                state.setDungeonMonsterDeducted(true);
            }

            final boolean chainCombat = random.nextInt(PERCENT_DIVISOR) < 10;
            if (chainCombat) {
                state.setMonsterCurrentHp(monster.maxHp());
                state.setTurnCount(1);
                state.setActive(true);
                final String chainMsg = "🚨 [연쇄 조우] " + monster.name() + " 무리가 추가로 기습!";
                combatLines.add(chainMsg);
                settlementLines.add(chainMsg);
                return new KillResolution(Outcome.NONE, null);
            }
        }

        state.setActive(false);
        return new KillResolution(Outcome.WIN, null);
    }

    // ─── Private: death ─────────────────────────────────────────────────────

    private Outcome handleDeath(
            final CharacterProgress progress,
            final BattleState state,
            final List<String> logLines) {
        if (dungeonService != null) {
            final Long characterId =
                    progress.getId() != null ? progress.getId() : state.getCharacterId();
            dungeonService.handlePlayerDeath(characterId);
        }
        final DeathResult deathResult = progressionService.die(progress);
        state.setActive(false);
        logLines.add("쓰러졌다… 티르코네일에서 부활 (경험치 -" + deathResult.experienceLost() + ")");
        return Outcome.LOSE;
    }

    // ─── Private: timeout ───────────────────────────────────────────────────

    private BattleTurnResult handleTimeoutTurn(
            final CharacterProgress progress, final BattleState state, final Monster monster) {
        final List<String> combatLines = new ArrayList<>();
        final SkillType monsterIntent = state.getCurrentMonsterIntent();
        final int monsterDamage;
        final SkillType monsterAction;

        if (monsterIntent == null) {
            combatLines.add("⏳ [시간 초과] 선제 공격 기회 상실");
            monsterDamage = 0;
            monsterAction = null;
        } else {
            monsterAction = monsterIntent;
            combatLines.add("⏳ [시간 초과] 무방비 피격");
            monsterDamage = resolveMonsterOnlyDamage(progress, monster, monsterAction);
            applyManaShieldAndDamage(progress, state, 0, monsterDamage, combatLines);

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
                combatLines.add("[" + monster.name() + "] 🛡️ 방어 태세 유지");
            }
        }

        Outcome outcome = Outcome.NONE;
        final List<String> settlementLines = new ArrayList<>();
        if (progress.isDead()) {
            outcome = handleDeath(progress, state, settlementLines);
        }

        state.setPreemptiveParty(PreemptiveParty.NONE);
        state.setCurrentMonsterIntent(null);
        state.setStandby(true);
        state.setTurnCount(state.getTurnCount() + 1);
        battleStateRepository.save(state);
        characterService.saveTurn(progress);

        settlementLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));

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
                combatLines);
    }

    // ─── Private: flee ──────────────────────────────────────────────────────

    private BattleTurnResult handleFleeSuccess(
            final BattleState state, final List<String> combatLines) {
        state.setActive(false);
        battleStateRepository.save(state);
        actionLog.add("도망쳤다!", LOG_TYPE_COMBAT);
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
                true,
                Outcome.FLED,
                null,
                0,
                List.of(),
                combatLines);
    }

    private BattleTurnResult handleFleeFailure(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final List<String> combatLines) {
        final int monsterDmg = resolveMonsterOnlyDamage(progress, monster, SkillType.NORMAL);
        progress.damageHp(monsterDmg);

        Outcome outcome = Outcome.NONE;
        if (progress.isDead()) {
            final List<String> deathLines = new ArrayList<>();
            outcome = handleDeath(progress, state, deathLines);
            deathLines.forEach(line -> actionLog.add(line, LOG_TYPE_COMBAT));
        }

        combatLines.add("⚠️ [도망 실패] " + monster.name() + "에게 저지당해 " + monsterDmg + " 피해");

        state.setPreemptiveParty(PreemptiveParty.NONE);
        state.setCurrentMonsterIntent(null);
        state.setStandby(true);
        state.setTurnCount(state.getTurnCount() + 1);
        battleStateRepository.save(state);
        characterService.saveTurn(progress);

        return new BattleTurnResult(
                null,
                0,
                SkillType.NORMAL,
                monsterDmg,
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
                combatLines);
    }

    // ─── Private: safe terminate / no-op results ────────────────────────────

    private BattleTurnResult safeTerminateAndReturn(final BattleState state) {
        state.setActive(false);
        battleStateRepository.save(state);
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
                true,
                Outcome.NONE,
                null,
                0,
                List.of(),
                List.of());
    }

    private BattleTurnResult buildNoOpResult() {
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
                List.of());
    }

    private BattleTurnResult buildInsufficientResult(
            final Skill skill, final ResourceKind insufficientKind) {
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
                List.of());
    }

    // ─── Private: clash view construction ────────────────────────────────────

    private BattleView buildClashBattleView(
            final BattleState state,
            final Monster monster,
            final CharacterProgress progress,
            final SkillType intent,
            final boolean bowFirstStrike,
            final PreemptiveParty preemptiveParty) {
        final List<BattleSkillButton> skills = combatSkills(progress);
        final int durationMs = resolveClashDuration(intent);
        final String badgeLabel = resolveStanceBadgeLabel(intent, bowFirstStrike, preemptiveParty);
        final String badgeClass = resolveStanceBadgeClass(intent, preemptiveParty);

        return new BattleView(
                monster.name(),
                monster.level(),
                state.getMonsterCurrentHp(),
                monster.maxHp(),
                skills,
                false,
                false,
                intent,
                durationMs,
                badgeLabel,
                badgeClass,
                bowFirstStrike);
    }

    private int resolveClashDuration(final SkillType intent) {
        if (intent == SkillType.NORMAL) {
            return NORMAL_CLASH_DURATION_MS;
        }
        return DEFAULT_CLASH_DURATION_MS;
    }

    private String resolveStanceBadgeLabel(
            final SkillType intent,
            final boolean bowFirstStrike,
            final PreemptiveParty preemptiveParty) {
        if (bowFirstStrike) {
            return BADGE_LABEL_BOW_FIRST_STRIKE;
        }
        if (preemptiveParty == PreemptiveParty.PLAYER) {
            return BADGE_LABEL_PREEMPTIVE_PLAYER;
        }
        if (preemptiveParty == PreemptiveParty.MONSTER) {
            return BADGE_LABEL_PREEMPTIVE_MONSTER;
        }
        if (intent == null) {
            return null;
        }
        return switch (intent) {
            case NORMAL -> BADGE_LABEL_NORMAL;
            case HEAVY -> BADGE_LABEL_HEAVY;
            case DEFENSE -> BADGE_LABEL_DEFENSE;
            default -> BADGE_LABEL_NORMAL;
        };
    }

    private String resolveStanceBadgeClass(
            final SkillType intent, final PreemptiveParty preemptiveParty) {
        if (preemptiveParty == PreemptiveParty.PLAYER) {
            return BADGE_CLASS_PREEMPTIVE_PLAYER;
        }
        if (preemptiveParty == PreemptiveParty.MONSTER) {
            return BADGE_CLASS_PREEMPTIVE_MONSTER;
        }
        if (intent == null) {
            return null;
        }
        return switch (intent) {
            case NORMAL -> BADGE_CLASS_NORMAL;
            case HEAVY -> BADGE_CLASS_HEAVY;
            case DEFENSE -> BADGE_CLASS_DEFENSE;
            default -> BADGE_CLASS_NORMAL;
        };
    }

    // ─── Private: internal record ───────────────────────────────────────────

    private record TurnCombatResult(
            int playerDamage,
            int monsterDamage,
            boolean playerCritical,
            boolean monsterCritical,
            boolean blocked,
            boolean countered,
            boolean firstStrike,
            List<HitResult> playerHits) {}
}
