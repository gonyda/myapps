package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;

/**
 * 스킬 시스템 핵심 애플리케이션 서비스.
 *
 * <p>스킬 랭크업·보너스 합산·습득·시드·카운팅 훅·임시 드라이버·뷰 조립을 오케스트레이션한다.
 * 카탈로그 조회({@link SkillCatalogService}), 영속({@link CharacterSkillRepository}),
 * 순수 정책({@link SkillRankPolicy}, {@link SkillRankupBonus}, {@link SkillDamagePolicy})을 조합한다.
 *
 * <p>랭크업 트랜잭션 흐름: rankable 판정 → AP 사전검증(부족 시 예외) →
 * {@code spendAbilityPoints} → {@code rankUpTo(next)} → 저장.
 */
@Service
public class SkillService {

    private static final String DEFAULT_SEED_SKILL_ID = "windmill";
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
     * <p>{@link SkillRankPolicy}, {@link SkillRankupBonus}, {@link SkillDamagePolicy}는
     * 순수 정책 객체이므로 직접 인스턴스를 생성한다(Spring 빈 등록 불필요).
     *
     * @param characterSkillRepository    캐릭터 보유 스킬 리포지토리
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리 (AP 조회용)
     * @param skillCatalogService         스킬 카탈로그 로드·조회 서비스
     */
    public SkillService(final CharacterSkillRepository characterSkillRepository,
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
     * <ol>
     *   <li>MASTER이면 승급 불가 → {@code false} 반환</li>
     *   <li>사용 횟수·막타 처치 조건 미충족 → {@code false} 반환</li>
     *   <li>AP 부족 → {@link InsufficientAbilityPointsException} 발생</li>
     * </ol>
     *
     * <p>승급 성공 시 (a) AP 소모, (b) 랭크 +1, (c) 카운트 리셋, (d) 저장을 수행한다.
     *
     * @param progress 캐릭터 진행상황 (AP 소모 대상)
     * @param skillId  승급 대상 스킬 ID
     * @return 승급 성공 여부 ({@code true}=성공, {@code false}=조건 미충족 또는 MASTER)
     * @throws InsufficientAbilityPointsException AP가 소모 비용 미만일 경우
     */
    @Transactional
    public boolean rankUp(final CharacterProgress progress, final String skillId) {
        final CharacterSkill skill = findSkill(progress.getId(), skillId);

        if (skill.getRank().isMax()) {
            return false;
        }

        final RankUpRequirement requirement = skillRankPolicy.requirement(skill.getRank()).orElseThrow();
        final int apCost = skillRankPolicy.apCost(skill.getRank()).orElseThrow();

        if (skill.getUsageCount() < requirement.requiredUsage()
                || skill.getKillCount() < requirement.requiredKills()) {
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
     * <p>보유 스킬을 탭 필터로 걸러 행(SkillRowView)을 구성한다.
     * 진행바는 동일가중 평균({@code (min(usage/req,1)+min(kill/req,1))/2×100})이고,
     * rankable은 조건+AP+MASTER 아님을 모두 충족해야 {@code true}이다.
     *
     * @param characterId 캐릭터 ID
     * @param activeTab   활성 탭 ("all"/"melee"/"archery"/"magic"/"common")
     * @return 스킬 목록 뷰 모델
     */
    public SkillListView buildListView(final Long characterId, final String activeTab) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        final CharacterProgress progress = characterProgressRepository.findById(characterId).orElseThrow();
        final int abilityPoints = progress.getAbilityPoints();

        final List<SkillRowView> rows = new ArrayList<>();
        for (final CharacterSkill characterSkill : owned) {
            final Optional<Skill> catalogOpt = skillCatalogService.byId(characterSkill.getSkillId());
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
     * <p>현재 랭크의 수치와 다음 랭크의 수치, 요구치, AP 비용 등을 모달 표시용으로 구성한다.
     * MASTER 도달 시 다음 랭크 정보는 현재 랭크 정보와 동일하게 채우고
     * {@code maxed=true}, {@code nextRankLabel=null}로 표시한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     승급 대상 스킬 ID
     * @return 승급 모달 뷰 모델
     */
    public SkillRankUpView buildRankUpView(final Long characterId, final String skillId) {
        final CharacterSkill characterSkill = findSkill(characterId, skillId);
        final Skill catalog = skillCatalogService.byId(skillId).orElseThrow();
        final CharacterProgress progress = characterProgressRepository.findById(characterId).orElseThrow();

        final SkillRank currentRank = characterSkill.getRank();
        final boolean maxed = currentRank.isMax();

        final String currentRankLabel = currentRank.label();
        final String nextRankLabel = maxed ? null : currentRank.next().orElseThrow().label();

        final int currentValue = primaryValue(catalog, currentRank);
        final int nextValue = maxed ? currentValue : primaryValue(catalog, currentRank.next().orElseThrow());

        final Integer currentCounterValue = counterValue(catalog, currentRank);
        final Integer nextCounterValue = maxed ? currentCounterValue
                : counterValue(catalog, currentRank.next().orElseThrow());

        final String primaryStatLabel = (catalog instanceof DefenseSkill)
                ? PRIMARY_STAT_DEFENSE : PRIMARY_STAT_DAMAGE;

        final String resourceKindLabel = catalog.talent().resourceKind().label();
        final int resourceCost = catalog.resourceCost();

        final int usageCurrent = characterSkill.getUsageCount();
        final int killCurrent = characterSkill.getKillCount();

        final int usageRequired;
        final int killRequired;
        final int apCost;
        if (maxed) {
            usageRequired = 0;
            killRequired = 0;
            apCost = 0;
        } else {
            final RankUpRequirement requirement = skillRankPolicy.requirement(currentRank).orElseThrow();
            usageRequired = requirement.requiredUsage();
            killRequired = requirement.requiredKills();
            apCost = skillRankPolicy.apCost(currentRank).orElseThrow();
        }

        final int apOwned = progress.getAbilityPoints();
        final boolean rankable = !maxed
                && usageCurrent >= usageRequired
                && killCurrent >= killRequired
                && apOwned >= apCost;

        return new SkillRankUpView(
                catalog.id(), catalog.label(),
                catalog.description(),
                currentRankLabel, nextRankLabel,
                primaryStatLabel, currentValue, nextValue,
                currentCounterValue, nextCounterValue,
                resourceKindLabel, resourceCost,
                usageCurrent, usageRequired,
                killCurrent, killRequired,
                apCost, apOwned,
                rankable, maxed
        );
    }

    /**
     * 캐릭터의 스킬 랭크업 영구 스탯 보너스를 합산한다.
     *
     * <p>{@link SkillRankupBonus#sum(List, java.util.function.Function)}을 이용하여
     * 보유 스킬 목록과 카탈로그에서 합산된 스탯 보너스를 계산한다.
     * 정보 팝업({@code PlayScreenViewHelper})이 사용한다.
     *
     * @param characterId 캐릭터 ID
     * @return 합산된 스킬 랭크업 스탯 보너스
     */
    public Stats rankupBonus(final Long characterId) {
        final List<CharacterSkill> owned = characterSkillRepository.findByCharacterId(characterId);
        return skillRankupBonus.sum(owned, skillCatalogService::byId);
    }

    /**
     * 스킬을 습득한다 (F 랭크·카운트 0으로 추가).
     *
     * <p>이미 보유한 스킬은 중복 추가하지 않으며, 카탈로그에 없는 스킬은 거부한다.
     * 향후 스킬북 판매/구매(아이템 5순위·NPC상점 8순위)가 이 진입점을 호출한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     습득할 스킬 카탈로그 ID
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
     * <p>현재 기본 시드 스킬은 {@code windmill}(NORMAL, MELEE) 1개이며 F 랭크로 추가된다.
     * 캐릭터 생성 시({@code CharacterService.loadOrCreateDefault}) 호출된다.
     *
     * @param characterId 시드 대상 캐릭터 ID
     */
    @Transactional
    public void seedDefault(final Long characterId) {
        learnSkill(characterId, DEFAULT_SEED_SKILL_ID);
    }

    /**
     * 스킬 사용 횟수를 현재 랭크의 요구치까지 즉시 설정한다 (임시 드라이버).
     *
     * <p>전투(7순위)의 실제 사용 이벤트({@code onSkillUsed})가 구현되면
     * 이 메서드를 호출하는 임시 엔드포인트({@code POST /skills/{id}/dev/fill-usage})는
     * 제거된다. 전투 스펙이 실제 카운팅 이벤트로 교체한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     대상 스킬 ID
     */
    @Transactional
    public void fillUsageToRequirement(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        if (skill.getRank().isMax()) {
            return;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(skill.getRank()).orElseThrow();
        skill.setUsageCount(requirement.requiredUsage());
        characterSkillRepository.save(skill);
    }

    /**
     * 막타 처치 수를 현재 랭크의 요구치까지 즉시 설정한다 (임시 드라이버).
     *
     * <p>전투(7순위)의 실제 막타 이벤트({@code onSkillKill})가 구현되면
     * 이 메서드를 호출하는 임시 엔드포인트({@code POST /skills/{id}/dev/fill-kill})는
     * 제거된다. 전투 스펙이 실제 카운팅 이벤트로 교체한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     대상 스킬 ID
     */
    @Transactional
    public void fillKillToRequirement(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        if (skill.getRank().isMax()) {
            return;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(skill.getRank()).orElseThrow();
        skill.setKillCount(requirement.requiredKills());
        characterSkillRepository.save(skill);
    }

    /**
     * 스킬 사용 이벤트를 처리한다 (사용 횟수 +1).
     *
     * <p>전투(7순위)가 호출하는 진입점. 현재는 임시 드라이버({@code dev/fill-usage})가
     * 대체 역할을 하며, 전투 스펙 구현 시 실제 전투 이벤트에서 호출된다.
     * 전투 스펙 구현 후 임시 드라이버는 제거된다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     사용된 스킬 ID
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
     * <p>전투(7순위)가 호출하는 진입점. 현재는 임시 드라이버({@code dev/fill-kill})가
     * 대체 역할을 하며, 전투 스펙 구현 시 실제 전투 이벤트에서 호출된다.
     * 전투 스펙 구현 후 임시 드라이버는 제거된다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     막타 처치된 스킬 ID
     */
    @Transactional
    public void onSkillKill(final Long characterId, final String skillId) {
        final CharacterSkill skill = findSkill(characterId, skillId);
        skill.increaseKill();
        characterSkillRepository.save(skill);
    }

    private CharacterSkill findSkill(final Long characterId, final String skillId) {
        return characterSkillRepository.findByCharacterIdAndSkillId(characterId, skillId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "캐릭터 " + characterId + "의 스킬 '" + skillId + "'을(를) 찾을 수 없습니다."));
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

    private SkillRowView buildRow(final CharacterSkill characterSkill,
                                  final Skill catalog,
                                  final int abilityPoints) {
        final SkillRank rank = characterSkill.getRank();
        final boolean maxed = rank.isMax();
        final int progressPercent = calculateProgressPercent(characterSkill, rank, maxed);
        final boolean rankable = calculateRankable(characterSkill, rank, maxed, abilityPoints);
        final String talentLabel = talentLabel(catalog.talent());

        return new SkillRowView(
                catalog.id(), catalog.label(),
                talentLabel, rank.label(),
                progressPercent, rankable, maxed
        );
    }

    private int calculateProgressPercent(final CharacterSkill characterSkill,
                                         final SkillRank rank,
                                         final boolean maxed) {
        if (maxed) {
            return FULL_PROGRESS_PERCENT;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
        final double usageRatio = Math.min(
                (double) characterSkill.getUsageCount() / requirement.requiredUsage(), 1.0);
        final double killRatio = Math.min(
                (double) characterSkill.getKillCount() / requirement.requiredKills(), 1.0);
        return (int) ((usageRatio + killRatio) / PROGRESS_DIVISOR * FULL_PROGRESS_PERCENT);
    }

    private boolean calculateRankable(final CharacterSkill characterSkill,
                                      final SkillRank rank,
                                      final boolean maxed,
                                      final int abilityPoints) {
        if (maxed) {
            return false;
        }
        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
        final int apCost = skillRankPolicy.apCost(rank).orElseThrow();
        return characterSkill.getUsageCount() >= requirement.requiredUsage()
                && characterSkill.getKillCount() >= requirement.requiredKills()
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

    private Integer counterValue(final Skill catalog, final SkillRank rank) {
        if (catalog instanceof DefenseSkill defenseSkill) {
            return skillDamagePolicy.counterMultiplier(defenseSkill, rank);
        }
        return null;
    }
}
