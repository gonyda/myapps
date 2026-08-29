package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.FieldSkillResult;
import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.application.dto.SkillRowView;
import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillDamagePolicy;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.SkillRankupBonus;
import com.myapps.web.myrpg.domain.model.SkillRankupBonusDelta;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스킬 시스템 핵심 애플리케이션 서비스.
 *
 * <p>스킬 랭크업·보너스 합산·습득·시드·카운팅 훅·뷰 조립을 오케스트레이션한다. 카탈로그 조회({@link SkillCatalogService}), 영속({@link
 * CharacterSkillRepository}), 순수 정책({@link SkillRankPolicy}, {@link SkillRankupBonus}, {@link
 * SkillDamagePolicy})을 조합한다.
 *
 * <p>랭크업 트랜잭션 흐름: rankable 판정 → AP 사전검증(부족 시 예외) → {@code spendAbilityPoints} → {@code
 * rankUpTo(next)} → 저장.
 */
@Service
public class SkillService {

    private static final List<String> DEFAULT_SEED_SKILL_IDS =
            List.of("slash", "aimed_shot", "mana_bolt", "defense");
    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int ACTIVE_COOLDOWN_PERCENT = 0;
    private static final String TAB_ALL = "all";
    private static final String TAB_MELEE = "melee";
    private static final String TAB_ARCHERY = "archery";
    private static final String TAB_MAGIC = "magic";
    private static final String TAB_COMMON = "common";
    private static final String LABEL_MELEE = "근접전투";
    private static final String LABEL_ARCHERY = "활";
    private static final String LABEL_MAGIC = "마법";
    private static final String LABEL_COMMON = "공용";
    private static final String PRIMARY_STAT_DAMAGE = "보너스 데미지";
    private static final String PRIMARY_STAT_DEFENSE = "피해 경감";

    private final CharacterSkillRepository characterSkillRepository;
    private final CharacterProgressRepository characterProgressRepository;
    private final SkillCatalogService skillCatalogService;
    private final SkillRankPolicy skillRankPolicy;
    private final SkillRankupBonus skillRankupBonus;
    private final SkillDamagePolicy skillDamagePolicy;
    private final StatProgression statProgression;

    /**
     * SkillService를 생성한다.
     *
     * <p>{@link SkillRankPolicy}, {@link SkillRankupBonus}, {@link SkillDamagePolicy}는 순수 정책 객체이므로
     * 직접 인스턴스를 생성한다(Spring 빈 등록 불필요).
     *
     * @param characterSkillRepository 캐릭터 보유 스킬 리포지토리
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리 (AP 조회용)
     * @param skillCatalogService 스킬 카탈로그 로드·조회 서비스
     */
    public SkillService(
            final CharacterSkillRepository characterSkillRepository,
            final CharacterProgressRepository characterProgressRepository,
            final SkillCatalogService skillCatalogService) {
        this.characterSkillRepository = characterSkillRepository;
        this.characterProgressRepository = characterProgressRepository;
        this.skillCatalogService = skillCatalogService;
        this.skillRankPolicy = new SkillRankPolicy();
        this.skillRankupBonus = new SkillRankupBonus();
        this.skillDamagePolicy = new SkillDamagePolicy();
        this.statProgression = new StatProgression();
    }

    /**
     * 스킬 랭크업을 시도한다.
     *
     * <p>선행조건 판정 순서:
     *
     * <ol>
     *   <li>MASTER이면 승급 불가 → {@code false} 반환
     *   <li>패시브 스킬: AP만 검증하고 즉시 승급
     *   <li>지원/특수/궁극기 스킬: 막타 처치 조건 면제, 사용 횟수만 검증
     *   <li>직접 공격 스킬: 사용 횟수 및 막타 처치 조건 모두 검증
     *   <li>AP 부족 → {@link InsufficientAbilityPointsException} 발생
     * </ol>
     *
     * <p>승급 성공 시 (a) AP 소모, (b) 랭크 +1, (c) 카운트 리셋, (d) 저장을 수행한다.
     *
     * @param progress 캐릭터 진행상황 (AP 소모 대상)
     * @param skillId 승급 대상 스킬 ID
     * @return 승급 성공 여부 ({@code true}=성공, {@code false}=조건 미충족 또는 MASTER)
     * @throws InsufficientAbilityPointsException AP가 소모 비용 미만일 경우
     */
    @Transactional
    public boolean rankUp(final CharacterProgress progress, final String skillId) {
        final CharacterSkill skill = findSkill(progress.getId(), skillId);

        if (skill.getRank().isMax()) {
            return false;
        }

        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();
        final int apCost = skillRankPolicy.apCost(skill.getRank()).orElseThrow();

        if (progress.getAbilityPoints() < apCost) {
            throw new InsufficientAbilityPointsException(
                    "AP 부족: 필요 " + apCost + ", 보유 " + progress.getAbilityPoints());
        }

        if (!catalog.isPassive()) {
            final RankUpRequirement requirement =
                    skillRankPolicy.requirementFor(skill.getRank(), catalog.type()).orElseThrow();

            if (skill.getUsageCount() < requirement.requiredUsage()) {
                return false;
            }
        }

        progress.spendAbilityPoints(apCost);

        final SkillRank nextRank = skill.getRank().next().orElseThrow();
        skill.rankUpTo(nextRank);

        characterSkillRepository.save(skill);
        return true;
    }

    /**
     * 필드에서 힐링 등 회복 스킬을 사용한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 스킬 ID
     * @param maxHp 최대 HP 상한
     * @param maxMp 최대 MP 상한
     * @return 필드 스킬 사용 결과
     */
    @Transactional
    public FieldSkillResult useFieldSkill(
            final Long characterId, final String skillId, final int maxHp, final int maxMp) {
        final CharacterProgress progress =
                characterProgressRepository
                        .findById(characterId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "캐릭터 " + characterId + "을(를) 찾을 수 없습니다."));
        final CharacterSkill skill = findSkill(characterId, skillId);
        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();

        if (!(catalog instanceof RecoverySkill recoverySkill)) {
            return FieldSkillResult.failure(
                    "필드에서 사용할 수 없는 스킬입니다.",
                    progress.getHpCurrent(),
                    maxHp,
                    progress.getMpCurrent(),
                    maxMp);
        }

        if (progress.getHpCurrent() >= maxHp) {
            return FieldSkillResult.failure(
                    "이미 최대 체력입니다.", progress.getHpCurrent(), maxHp, progress.getMpCurrent(), maxMp);
        }

        final int cost = recoverySkill.resourceCostAt(skill.getRank());
        if (progress.getMpCurrent() < cost) {
            return FieldSkillResult.failure(
                    "MP가 부족합니다.", progress.getHpCurrent(), maxHp, progress.getMpCurrent(), maxMp);
        }

        final int healAmount = recoverySkill.healAmountAt(skill.getRank());
        final int beforeHp = progress.getHpCurrent();
        progress.spendMp(cost);
        progress.healHp(healAmount, maxHp);
        final int actualHealed = progress.getHpCurrent() - beforeHp;

        skill.increaseUsage();
        characterSkillRepository.save(skill);
        characterProgressRepository.save(progress);

        return FieldSkillResult.success(
                "생명력을 " + actualHealed + " 회복했습니다. (MP -" + cost + ")",
                progress.getHpCurrent(),
                maxHp,
                progress.getMpCurrent(),
                maxMp,
                actualHealed);
    }

    /**
     * 필드에서 힐링 등 회복 스킬을 사용한다 (기본 바이탈 계산 기준).
     *
     * @param characterId 캐릭터 ID
     * @param skillId 스킬 ID
     * @return 필드 스킬 사용 결과
     */
    @Transactional
    public FieldSkillResult useFieldSkill(final Long characterId, final String skillId) {
        final CharacterProgress progress =
                characterProgressRepository
                        .findById(characterId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "캐릭터 " + characterId + "을(를) 찾을 수 없습니다."));
        final VitalMax baseVital =
                statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent());
        final VitalMax skillVital = rankupVitalBonus(characterId);
        final int maxHp = baseVital.hp() + skillVital.hp();
        final int maxMp = baseVital.mp() + skillVital.mp();
        return useFieldSkill(characterId, skillId, maxHp, maxMp);
    }

    /**
     * 스킬 목록 팝업 뷰를 조립한다.
     *
     * <p>보유 스킬을 탭 필터로 걸러 행(SkillRowView)을 구성하며, 상단 10개 슬롯 도크 정보(slots)를 함께 조립한다.
     *
     * @param characterId 캐릭터 ID
     * @param activeTab 활성 탭 ("all"/"melee"/"archery"/"magic"/"common")
     * @return 스킬 목록 뷰 모델
     */
    public SkillListView buildListView(final Long characterId, final String activeTab) {
        final String effectiveTab =
                (activeTab == null || activeTab.isBlank()) ? TAB_ALL : activeTab;
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        final CharacterProgress progress =
                characterProgressRepository.findById(characterId).orElseThrow();
        final int abilityPoints = progress.getAbilityPoints();

        final List<SkillRowView> rows = new ArrayList<>();
        for (final CharacterSkill characterSkill : owned) {
            final Optional<Skill> catalogOpt =
                    skillCatalogService.byId(characterSkill.getSkillId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Skill catalog = catalogOpt.get();

            if (!matchesTab(catalog, effectiveTab)) {
                continue;
            }

            final SkillRowView row = buildRow(characterSkill, catalog, abilityPoints);
            rows.add(row);
        }

        final List<BattleSkillButton> slots = buildSkillSlots(characterId);
        return new SkillListView(effectiveTab, List.copyOf(rows), slots);
    }

    /**
     * 캐릭터의 10개 스킬 슬롯(0~9번) 뷰 목록을 조립한다.
     *
     * @param characterId 캐릭터 ID
     * @return 10개 슬롯의 BattleSkillButton 목록
     */
    public List<BattleSkillButton> buildSkillSlots(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        final BattleSkillButton[] slotArray = new BattleSkillButton[10];
        for (int i = 0; i < 10; i++) {
            slotArray[i] = BattleSkillButton.emptySlot(i);
        }

        for (final CharacterSkill cs : owned) {
            final Integer slotIdx = cs.getSlotIndex();
            if (slotIdx != null && slotIdx >= 0 && slotIdx < 10) {
                final Optional<Skill> catalogOpt = skillCatalogService.byId(cs.getSkillId());
                if (catalogOpt.isPresent()) {
                    final Skill catalog = catalogOpt.get();
                    final String icon = resolveSkillIcon(catalog);
                    final int cooldown = cs.getUltimateCooldown();
                    final boolean ready = catalog.type() == SkillType.ULTIMATE && cooldown == 0;
                    slotArray[slotIdx] =
                            new BattleSkillButton(
                                    catalog.id(),
                                    catalog.label(),
                                    catalog.type(),
                                    catalog.talent().resourceKind(),
                                    catalog.resourceCost(),
                                    cooldown,
                                    ready,
                                    slotIdx,
                                    true,
                                    null,
                                    false,
                                    icon);
                }
            }
        }
        return List.of(slotArray);
    }

    /**
     * 스킬을 지정된 핫바 슬롯(0~9)에 등록한다 (스왑 및 패시브 검증 포함).
     *
     * @param characterId 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID
     * @param targetSlotIndex 배정할 슬롯 번호 (0~9)
     */
    @Transactional
    public void assignSkillSlot(
            final Long characterId, final String skillId, final int targetSlotIndex) {
        if (targetSlotIndex < 0 || targetSlotIndex > 9) {
            throw new IllegalArgumentException("슬롯 번호는 0~9 사이여야 합니다: " + targetSlotIndex);
        }
        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();
        if (catalog instanceof PassiveSkill) {
            throw new IllegalArgumentException("패시브 스킬은 슬롯에 등록할 수 없습니다: " + skillId);
        }

        final CharacterSkill currentSkill = findSkill(characterId, skillId);
        final Integer originalSlot = currentSkill.getSlotIndex();

        // 대상 슬롯에 이미 다른 스킬이 장착되어 있는지 확인
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        for (final CharacterSkill other : owned) {
            if (!other.getSkillId().equals(skillId)
                    && other.getSlotIndex() != null
                    && other.getSlotIndex() == targetSlotIndex) {
                // 맞교환(스왑) 또는 해제
                other.setSlotIndex(originalSlot);
                characterSkillRepository.save(other);
                break;
            }
        }

        currentSkill.setSlotIndex(targetSlotIndex);
        characterSkillRepository.save(currentSkill);
    }

    /**
     * 특정 스킬의 슬롯 등록을 해제한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID
     */
    @Transactional
    public void clearSkillSlot(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        skill.clearSlot();
        characterSkillRepository.save(skill);
    }

    /**
     * 캐릭터의 모든 스킬 슬롯 등록을 초기화한다.
     *
     * @param characterId 캐릭터 ID
     */
    @Transactional
    public void clearAllSkillSlots(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        for (final CharacterSkill cs : owned) {
            if (cs.getSlotIndex() != null) {
                cs.clearSlot();
                characterSkillRepository.save(cs);
            }
        }
    }

    /**
     * 보유 액티브 스킬들을 기본 순서대로 0~9번 슬롯에 자동 배정한다.
     *
     * @param characterId 캐릭터 ID
     */
    @Transactional
    public void autoAssignDefaultSlots(final Long characterId) {
        clearAllSkillSlots(characterId);
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        int slot = 0;
        for (final CharacterSkill cs : owned) {
            final Optional<Skill> catalogOpt = skillCatalogService.byId(cs.getSkillId());
            if (catalogOpt.isPresent() && !(catalogOpt.get() instanceof PassiveSkill)) {
                cs.setSlotIndex(slot++);
                characterSkillRepository.save(cs);
                if (slot >= 10) {
                    break;
                }
            }
        }
    }

    /**
     * 스킬 승급 모달 뷰를 조립한다.
     *
     * <p>현재 랭크의 수치와 다음 랭크의 수치, 요구치, AP 비용 등을 모달 표시용으로 구성한다. MASTER 도달 시 다음 랭크 정보는 현재 랭크 정보와 동일하게
     * 채우고 {@code maxed=true}, {@code nextRankLabel=null}로 표시한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 승급 대상 스킬 ID
     * @return 승급 모달 뷰 모델
     */
    public SkillRankUpView buildRankUpView(final Long characterId, final String skillId) {
        final CharacterSkill characterSkill = findSkill(characterId, skillId);
        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();
        final CharacterProgress progress =
                characterProgressRepository.findById(characterId).orElseThrow();

        final SkillRank currentRank = characterSkill.getRank();
        final boolean maxed = currentRank.isMax();

        final String currentRankLabel = currentRank.label();
        final SkillRank nextRank = maxed ? null : currentRank.next().orElse(null);
        final String nextRankLabel = nextRank != null ? nextRank.label() : null;

        final int currentValue = primaryValue(catalog, currentRank);
        final int nextValue = maxed ? currentValue : primaryValue(catalog, nextRank);

        final Integer currentCounterValue = counterValue(catalog, currentRank);
        final Integer nextCounterValue =
                maxed ? currentCounterValue : counterValue(catalog, nextRank);

        final String primaryStatLabel = primaryStatLabel(catalog);

        final String resourceKindLabel = catalog.talent().resourceKind().label();
        final int resourceCost = resourceCost(catalog, currentRank);
        final Integer nextResourceCost = maxed ? null : resourceCost(catalog, nextRank);

        final Integer currentCritBonus = critBonusValue(catalog, currentRank);
        final Integer nextCritBonus = maxed ? null : critBonusValue(catalog, nextRank);

        final SkillRankupBonusDelta bonusDelta =
                maxed
                        ? SkillRankupBonusDelta.ZERO
                        : catalog.rankupBonusDelta(currentRank, nextRank);
        final String rankupBonusText = bonusDelta.toDisplayText();

        final List<com.myapps.web.myrpg.domain.model.SkillEffectRowView> effectRows =
                catalog.effectRowsAt(currentRank, nextRank);

        final int usageCurrent = characterSkill.getUsageCount();
        final boolean isPassive = catalog.isPassive();

        final RankUpRequirementInfo reqInfo =
                resolveRankUpRequirementInfo(catalog, currentRank, maxed, isPassive);

        final int apOwned = progress.getAbilityPoints();
        final boolean rankable =
                isRankableNow(
                        maxed,
                        apOwned,
                        reqInfo.apCost(),
                        isPassive,
                        usageCurrent,
                        reqInfo.usageRequired());

        return new SkillRankUpView(
                catalog.id(),
                catalog.label(),
                catalog.description(),
                currentRankLabel,
                nextRankLabel,
                primaryStatLabel,
                currentValue,
                nextValue,
                currentCounterValue,
                nextCounterValue,
                resourceKindLabel,
                resourceCost,
                nextResourceCost,
                currentCritBonus,
                nextCritBonus,
                rankupBonusText,
                usageCurrent,
                reqInfo.usageRequired(),
                reqInfo.apCost(),
                apOwned,
                rankable,
                maxed,
                effectRows,
                isPassive,
                !maxed && !isPassive);
    }

    private record RankUpRequirementInfo(int usageRequired, int apCost) {}

    private RankUpRequirementInfo resolveRankUpRequirementInfo(
            final Skill catalog,
            final SkillRank currentRank,
            final boolean maxed,
            final boolean isPassive) {
        if (maxed || isPassive) {
            final int apCost = maxed ? 0 : skillRankPolicy.apCost(currentRank).orElseThrow();
            return new RankUpRequirementInfo(0, apCost);
        }
        final RankUpRequirement requirement =
                skillRankPolicy.requirementFor(currentRank, catalog.type()).orElseThrow();
        final int apCost = skillRankPolicy.apCost(currentRank).orElseThrow();
        return new RankUpRequirementInfo(requirement.requiredUsage(), apCost);
    }

    private boolean isRankableNow(
            final boolean maxed,
            final int apOwned,
            final int apCost,
            final boolean isPassive,
            final int usageCurrent,
            final int usageRequired) {
        if (maxed || apOwned < apCost) {
            return false;
        }
        if (isPassive) {
            return true;
        }
        return usageCurrent >= usageRequired;
    }

    /**
     * 캐릭터의 스킬 랭크업 영구 스탯 보너스를 합산한다.
     *
     * <p>{@link SkillRankupBonus#sum(List, java.util.function.Function)}을 이용하여 보유 스킬 목록과 카탈로그에서 합산된
     * 스탯 보너스를 계산한다. 정보 팝업({@code PlayScreenViewHelper})이 사용한다.
     *
     * @param characterId 캐릭터 ID
     * @return 합산된 스킬 랭크업 스탯 보너스
     */
    public Stats rankupBonus(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        return skillRankupBonus.sum(owned, skillCatalogService::byId);
    }

    /**
     * 캐릭터의 스킬 랭크업 영구 바이탈(HP) 보너스를 합산한다.
     *
     * <p>{@link SkillRankupBonus#sumVital(List, java.util.function.Function)}을 이용하여 보유 스킬 목록과
     * 카탈로그에서 디펜스 스킬 랭크업으로 인한 추가 HP 최대치 보너스를 계산한다.
     *
     * @param characterId 캐릭터 ID
     * @return 합산된 스킬 랭크업 바이탈 보너스
     */
    public VitalMax rankupVitalBonus(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        return skillRankupBonus.sumVital(owned, skillCatalogService::byId);
    }

    /**
     * 스킬을 습득한다 (F 랭크·카운트 0으로 추가).
     *
     * <p>이미 보유한 스킬은 중복 추가하지 않으며, 카탈로그에 없는 스킬은 거부한다. 향후 스킬북 판매/구매(아이템 5순위·NPC상점 8순위)가 이 진입점을 호출한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 습득할 스킬 카탈로그 ID
     */
    @Transactional
    public void learnSkill(final Long characterId, final String skillId) {
        if (skillCatalogService.byId(skillId).isEmpty()) {
            return;
        }
        final Optional<CharacterSkill> existing =
                characterSkillRepository.findByCharacterIdAndSkillId(characterId, skillId);
        if (existing.isPresent()) {
            return;
        }
        characterSkillRepository.save(CharacterSkill.newSkill(characterId, skillId));
    }

    /**
     * 신규 캐릭터에게 기본 스킬을 시드한다.
     *
     * <p>기본 시드 스킬은 재능별 기본 공격 3종과 공용 방어 스킬 1종으로 구성되며, 각각 F 랭크로 추가된다: {@code slash}(베기, MELEE),
     * {@code aimed_shot}(조준 사격, ARCHERY), {@code mana_bolt}(마나 볼트, MAGIC), {@code defense}(디펜스,
     * COMMON). 캐릭터 생성 시({@code CharacterService.loadOrCreateDefault}) 호출된다.
     *
     * @param characterId 시드 대상 캐릭터 ID
     */
    @Transactional
    public void seedDefault(final Long characterId) {
        for (final String skillId : DEFAULT_SEED_SKILL_IDS) {
            learnSkill(characterId, skillId);
        }
        autoAssignDefaultSlots(characterId);
    }

    /**
     * 스킬 사용 이벤트를 처리한다 (사용 횟수 +1).
     *
     * <p>전투에서 스킬을 사용할 때마다 호출되며, 사용 횟수를 1 증가시킨다. 승급 조건의 사용 횟수 요구치 달성에 기여한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 사용된 스킬 ID
     */
    @Transactional
    public void onSkillUsed(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        skill.increaseUsage();
        characterSkillRepository.save(skill);
    }

    /**
     * 전투 승리 시 보유한 모든 궁극기 스킬의 남은 쿨다운(필요 승리 횟수)을 1 감소시킨다.
     *
     * @param characterId 캐릭터 ID
     */
    @Transactional
    public void onBattleWon(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        for (final CharacterSkill skill : owned) {
            if (skill.getUltimateCooldown() > 0) {
                skill.decrementUltimateCooldown();
                characterSkillRepository.save(skill);
            }
        }
    }

    /**
     * 메디테이션 패시브 스킬로 인한 턴당 마나 회복량을 조회한다.
     *
     * @param characterId 캐릭터 ID
     * @return 턴당 마나 회복량 (없으면 0)
     */
    public int meditationTurnRegen(final Long characterId) {
        final Optional<CharacterSkill> meditationOpt =
                characterSkillRepository.findByCharacterIdAndSkillId(characterId, "meditation");
        if (meditationOpt.isEmpty()) {
            return 0;
        }
        final Optional<Skill> catalogOpt = skillCatalogService.byId("meditation");
        if (catalogOpt.isEmpty() || !(catalogOpt.get() instanceof PassiveSkill passive)) {
            return 0;
        }
        final int totalRegen =
                passive.totalStatBonus() != null
                        ? passive.totalStatBonus().getOrDefault(BonusTarget.MP_REGEN, 5)
                        : 5;
        final SkillRank rank = meditationOpt.get().getRank();
        return 1 + (totalRegen - 1) * rank.order() / 15;
    }

    private CharacterSkill findSkill(final Long characterId, final String skillId) {
        return characterSkillRepository
                .findByCharacterIdAndSkillId(characterId, skillId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "캐릭터 "
                                                + characterId
                                                + "의 스킬 '"
                                                + skillId
                                                + "'을(를) 찾을 수 없습니다."));
    }

    private boolean matchesTab(final Skill catalog, final String tab) {
        final String effectiveTab = (tab == null || tab.isBlank()) ? TAB_ALL : tab;
        if (catalog instanceof PassiveSkill) {
            return TAB_ALL.equals(effectiveTab) || TAB_COMMON.equals(effectiveTab);
        }
        return switch (effectiveTab) {
            case TAB_ALL -> true;
            case TAB_MELEE -> catalog.talent() == SkillTalent.MELEE;
            case TAB_ARCHERY -> catalog.talent() == SkillTalent.ARCHERY;
            case TAB_MAGIC -> catalog.talent() == SkillTalent.MAGIC;
            case TAB_COMMON -> catalog.talent() == SkillTalent.COMMON;
            default -> true;
        };
    }

    private SkillRowView buildRow(
            final CharacterSkill characterSkill, final Skill catalog, final int abilityPoints) {
        final SkillRank rank = characterSkill.getRank();
        final boolean maxed = rank.isMax();
        final int progressPercent = calculateProgressPercent(characterSkill, catalog, rank, maxed);
        final boolean rankable =
                calculateRankable(characterSkill, catalog, rank, maxed, abilityPoints);
        final String talentLabel = talentLabel(catalog.talent());
        final boolean fieldUsable = catalog instanceof RecoverySkill;
        final String cooldownBadgeText = resolveCooldownBadgeText(characterSkill, catalog);
        final boolean isPassive = catalog instanceof PassiveSkill;

        final String icon = resolveSkillIcon(catalog);

        return new SkillRowView(
                catalog.id(),
                catalog.label(),
                talentLabel,
                rank.label(),
                progressPercent,
                rankable,
                maxed,
                fieldUsable,
                cooldownBadgeText,
                characterSkill.getSlotIndex(),
                isPassive,
                icon);
    }

    public String resolveSkillIcon(final Skill catalog) {
        if (catalog == null) {
            return "⚔️";
        }
        if (catalog.talent() == SkillTalent.COMMON || catalog instanceof PassiveSkill) {
            return "👑";
        }
        return switch (catalog.talent()) {
            case MELEE -> "⚔️";
            case ARCHERY -> "🏹";
            case MAGIC -> "🔮";
            case COMMON -> "👑";
        };
    }

    private String resolveCooldownBadgeText(
            final CharacterSkill characterSkill, final Skill catalog) {
        if (catalog instanceof UltimateSkill) {
            final int cooldown = characterSkill.getUltimateCooldown();
            if (cooldown > 0) {
                return "대기 " + cooldown + "승";
            }
            return "준비 완료";
        }
        return null;
    }

    private int calculateProgressPercent(
            final CharacterSkill characterSkill,
            final Skill catalog,
            final SkillRank rank,
            final boolean maxed) {
        if (maxed || catalog instanceof PassiveSkill) {
            return FULL_PROGRESS_PERCENT;
        }
        final RankUpRequirement requirement =
                skillRankPolicy.requirementFor(rank, catalog.type()).orElseThrow();
        final double usageRatio =
                Math.min(
                        (double) characterSkill.getUsageCount() / requirement.requiredUsage(), 1.0);
        return (int) (usageRatio * FULL_PROGRESS_PERCENT);
    }

    private boolean calculateRankable(
            final CharacterSkill characterSkill,
            final Skill catalog,
            final SkillRank rank,
            final boolean maxed,
            final int abilityPoints) {
        if (maxed) {
            return false;
        }
        final int apCost = skillRankPolicy.apCost(rank).orElseThrow();
        if (abilityPoints < apCost) {
            return false;
        }
        if (catalog instanceof PassiveSkill) {
            return true;
        }
        final RankUpRequirement requirement =
                skillRankPolicy.requirementFor(rank, catalog.type()).orElseThrow();
        return characterSkill.getUsageCount() >= requirement.requiredUsage();
    }

    private String talentLabel(final SkillTalent talent) {
        return switch (talent) {
            case MELEE -> LABEL_MELEE;
            case ARCHERY -> LABEL_ARCHERY;
            case MAGIC -> LABEL_MAGIC;
            case COMMON -> LABEL_COMMON;
        };
    }

    private String primaryStatLabel(final Skill catalog) {
        if (catalog instanceof DefenseSkill) {
            return PRIMARY_STAT_DEFENSE;
        }
        if (catalog instanceof RecoverySkill) {
            return "회복량";
        }
        if (catalog instanceof BuffSkill) {
            return "흡수율";
        }
        if (catalog instanceof CcSkill) {
            return "성공률";
        }
        if (catalog instanceof DotSkill) {
            return "초기 배율";
        }
        if (catalog instanceof PassiveSkill) {
            return "패시브 효과";
        }
        return PRIMARY_STAT_DAMAGE;
    }

    private int primaryValue(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DamageSkill damageSkill) {
            return skillDamagePolicy.multiplier(damageSkill, rank);
        }
        if (catalog instanceof DefenseSkill defenseSkill) {
            return skillDamagePolicy.blockRate(defenseSkill, rank);
        }
        if (catalog instanceof RecoverySkill recoverySkill) {
            return recoverySkill.healAmountAt(rank);
        }
        if (catalog instanceof UltimateSkill ultimateSkill) {
            return ultimateSkill.multiplierAt(rank);
        }
        if (catalog instanceof BuffSkill buffSkill) {
            return buffSkill.absorbRateAt(rank);
        }
        if (catalog instanceof CcSkill ccSkill) {
            return ccSkill.successRateAt(rank);
        }
        if (catalog instanceof DotSkill dotSkill) {
            return dotSkill.initialMultiplierAt(rank);
        }
        return 0;
    }

    private int resourceCost(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DefenseSkill defenseSkill) {
            return defenseSkill.resourceCostAt(rank);
        }
        if (catalog instanceof RecoverySkill recoverySkill) {
            return recoverySkill.resourceCostAt(rank);
        }
        return catalog.resourceCost();
    }

    private Integer counterValue(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DefenseSkill defenseSkill) {
            final int multiplier = skillDamagePolicy.counterMultiplier(defenseSkill, rank);
            if (multiplier > 0) {
                return multiplier;
            }
        }
        return null;
    }

    private Integer critBonusValue(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DefenseSkill defenseSkill
                && defenseSkill.critBonusByRank() != null
                && !defenseSkill.critBonusByRank().isEmpty()) {
            return defenseSkill.critBonusAt(rank);
        }
        return null;
    }
}
