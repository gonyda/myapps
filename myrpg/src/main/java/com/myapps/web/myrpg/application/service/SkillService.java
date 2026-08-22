package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.application.dto.SkillRowView;
import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillDamagePolicy;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.SkillRankupBonus;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.Stats;
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
    private static final int PROGRESS_DIVISOR = 2;
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
    }

    /**
     * 스킬 랭크업을 시도한다.
     *
     * <p>선행조건 판정 순서:
     *
     * <ol>
     *   <li>MASTER이면 승급 불가 → {@code false} 반환
     *   <li>사용 횟수 미충족 → {@code false} 반환
     *   <li>DEFENSE 타입이 아닌 경우 막타 처치 조건 미충족 → {@code false} 반환
     *   <li>AP 부족 → {@link InsufficientAbilityPointsException} 발생
     * </ol>
     *
     * <p>DEFENSE 타입 스킬(디펜스, 카운터 어택)은 반격 피해가 낮아 막타를 달성하기 어려우므로 kills 조건을 면제한다.
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

        final RankUpRequirement requirement =
                skillRankPolicy.requirement(skill.getRank()).orElseThrow();
        final int apCost = skillRankPolicy.apCost(skill.getRank()).orElseThrow();
        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();
        final boolean killExempt = catalog instanceof DefenseSkill;

        if (skill.getUsageCount() < requirement.requiredUsage()) {
            return false;
        }

        if (!killExempt && skill.getKillCount() < requirement.requiredKills()) {
            return false;
        }

        if (progress.getAbilityPoints() < apCost) {
            throw new InsufficientAbilityPointsException(
                    "AP 부족: 필요 " + apCost + ", 보유 " + progress.getAbilityPoints());
        }

        progress.spendAbilityPoints(apCost);

        final SkillRank nextRank = skill.getRank().next().orElseThrow();
        skill.rankUpTo(nextRank);

        characterSkillRepository.save(skill);
        return true;
    }

    /**
     * 스킬 목록 팝업 뷰를 조립한다.
     *
     * <p>보유 스킬을 탭 필터로 걸러 행(SkillRowView)을 구성한다. 진행바는 동일가중 평균({@code
     * (min(usage/req,1)+min(kill/req,1))/2×100})이고, rankable은 조건+AP+MASTER 아님을 모두 충족해야 {@code
     * true}이다.
     *
     * @param characterId 캐릭터 ID
     * @param activeTab 활성 탭 ("all"/"melee"/"archery"/"magic"/"common")
     * @return 스킬 목록 뷰 모델
     */
    public SkillListView buildListView(final Long characterId, final String activeTab) {
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

            if (!matchesTab(catalog.talent(), activeTab)) {
                continue;
            }

            final SkillRowView row = buildRow(characterSkill, catalog, abilityPoints);
            rows.add(row);
        }

        return new SkillListView(activeTab, List.copyOf(rows));
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
        final String nextRankLabel = maxed ? null : currentRank.next().orElseThrow().label();

        final int currentValue = primaryValue(catalog, currentRank);
        final int nextValue =
                maxed ? currentValue : primaryValue(catalog, currentRank.next().orElseThrow());

        final Integer currentCounterValue = counterValue(catalog, currentRank);
        final Integer nextCounterValue =
                maxed
                        ? currentCounterValue
                        : counterValue(catalog, currentRank.next().orElseThrow());

        final String primaryStatLabel =
                (catalog instanceof DefenseSkill) ? PRIMARY_STAT_DEFENSE : PRIMARY_STAT_DAMAGE;

        final String resourceKindLabel = catalog.talent().resourceKind().label();
        final int resourceCost = resourceCost(catalog, currentRank);
        final Integer nextResourceCost =
                maxed ? null : resourceCost(catalog, currentRank.next().orElseThrow());

        final Integer currentCritBonus = critBonusValue(catalog, currentRank);
        final Integer nextCritBonus =
                maxed ? null : critBonusValue(catalog, currentRank.next().orElseThrow());

        final String rankupBonusText = rankupBonusText(catalog);

        final int usageCurrent = characterSkill.getUsageCount();
        final int killCurrent = characterSkill.getKillCount();

        final boolean killExempt = catalog instanceof DefenseSkill;

        final int usageRequired;
        final int killRequired;
        final int apCost;
        if (maxed) {
            usageRequired = 0;
            killRequired = 0;
            apCost = 0;
        } else {
            final RankUpRequirement requirement =
                    skillRankPolicy.requirement(currentRank).orElseThrow();
            usageRequired = requirement.requiredUsage();
            killRequired = killExempt ? 0 : requirement.requiredKills();
            apCost = skillRankPolicy.apCost(currentRank).orElseThrow();
        }

        final int apOwned = progress.getAbilityPoints();
        final boolean rankable =
                !maxed
                        && usageCurrent >= usageRequired
                        && (killExempt || killCurrent >= killRequired)
                        && apOwned >= apCost;

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
                usageRequired,
                killCurrent,
                killRequired,
                apCost,
                apOwned,
                rankable,
                maxed);
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
     * 스킬 막타 처치 이벤트를 처리한다 (막타 처치 수 +1).
     *
     * <p>전투에서 스킬로 몬스터를 처치할 때 호출되며, 막타 처치 수를 1 증가시킨다. 승급 조건의 막타 처치 요구치 달성에 기여한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId 막타 처치된 스킬 ID
     */
    @Transactional
    public void onSkillKill(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        skill.increaseKill();
        characterSkillRepository.save(skill);
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

    private boolean matchesTab(final SkillTalent talent, final String tab) {
        return switch (tab) {
            case TAB_ALL -> true;
            case TAB_MELEE -> talent == SkillTalent.MELEE;
            case TAB_ARCHERY -> talent == SkillTalent.ARCHERY;
            case TAB_MAGIC -> talent == SkillTalent.MAGIC;
            case TAB_COMMON -> talent == SkillTalent.COMMON;
            default -> true;
        };
    }

    private SkillRowView buildRow(
            final CharacterSkill characterSkill, final Skill catalog, final int abilityPoints) {
        final SkillRank rank = characterSkill.getRank();
        final boolean maxed = rank.isMax();
        final boolean killExempt = catalog instanceof DefenseSkill;
        final int progressPercent =
                calculateProgressPercent(characterSkill, rank, maxed, killExempt);
        final boolean rankable =
                calculateRankable(characterSkill, rank, maxed, abilityPoints, killExempt);
        final String talentLabel = talentLabel(catalog.talent());

        return new SkillRowView(
                catalog.id(),
                catalog.label(),
                talentLabel,
                rank.label(),
                progressPercent,
                rankable,
                maxed);
    }

    private int calculateProgressPercent(
            final CharacterSkill characterSkill,
            final SkillRank rank,
            final boolean maxed,
            final boolean killExempt) {
        if (maxed) {
            return FULL_PROGRESS_PERCENT;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
        final double usageRatio =
                Math.min(
                        (double) characterSkill.getUsageCount() / requirement.requiredUsage(), 1.0);
        if (killExempt) {
            return (int) (usageRatio * FULL_PROGRESS_PERCENT);
        }
        final double killRatio =
                Math.min((double) characterSkill.getKillCount() / requirement.requiredKills(), 1.0);
        return (int) ((usageRatio + killRatio) / PROGRESS_DIVISOR * FULL_PROGRESS_PERCENT);
    }

    private boolean calculateRankable(
            final CharacterSkill characterSkill,
            final SkillRank rank,
            final boolean maxed,
            final int abilityPoints,
            final boolean killExempt) {
        if (maxed) {
            return false;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
        final int apCost = skillRankPolicy.apCost(rank).orElseThrow();
        return characterSkill.getUsageCount() >= requirement.requiredUsage()
                && (killExempt || characterSkill.getKillCount() >= requirement.requiredKills())
                && abilityPoints >= apCost;
    }

    private String talentLabel(final SkillTalent talent) {
        return switch (talent) {
            case MELEE -> LABEL_MELEE;
            case ARCHERY -> LABEL_ARCHERY;
            case MAGIC -> LABEL_MAGIC;
            case COMMON -> LABEL_COMMON;
        };
    }

    private int primaryValue(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DamageSkill damageSkill) {
            return skillDamagePolicy.multiplier(damageSkill, rank);
        }
        final DefenseSkill defenseSkill = (DefenseSkill) catalog;
        return skillDamagePolicy.blockRate(defenseSkill, rank);
    }

    private int resourceCost(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DefenseSkill defenseSkill) {
            return defenseSkill.resourceCostAt(rank);
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

    private String rankupBonusText(final Skill catalog) {
        if ("defense".equals(catalog.id())) {
            return "HP +5, DEF +1";
        }
        if ("counter_attack".equals(catalog.id())) {
            return null;
        }
        return switch (catalog.talent()) {
            case MELEE -> "STR +1";
            case ARCHERY -> "DEX +1";
            case MAGIC -> "INT +1";
            case COMMON -> "DEF +1";
        };
    }
}
