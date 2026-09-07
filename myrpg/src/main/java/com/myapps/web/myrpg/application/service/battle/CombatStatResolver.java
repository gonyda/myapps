package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillDamagePolicy;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 전투 스탯 및 자원 계산/해석을 담당하는 협력 클래스.
 *
 * <p>공격력, 방어력, 크리티컬, 자원 소모, 마법 캐스팅 실패 판정 등의 전투 스탯 연산을 처리합니다.
 */
public final class CombatStatResolver {

    private static final int PERCENT_DIVISOR = 100;
    private static final int CRITICAL_ROLL_MAX = 1000;

    private final StatProgression statProgression;
    private final SkillService skillService;
    private final InventoryService inventoryService;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillDamagePolicy skillDamagePolicy;
    private final GameProperties gameProperties;
    private final GameMessageService gameMessageService;
    private final Random random;

    /**
     * CombatStatResolver 인스턴스를 생성한다.
     *
     * @param statProgression 레벨별 기본 스탯 제공자
     * @param skillService 스킬 서비스
     * @param inventoryService 인벤토리 및 장비 서비스
     * @param characterSkillRepository 캐릭터 스킬 레포지토리
     * @param skillDamagePolicy 스킬 대미지 정책
     * @param gameProperties 게임 설정 프로퍼티
     * @param gameMessageService 게임 메시지 서비스
     * @param random 난수 생성기
     */
    public CombatStatResolver(
            final StatProgression statProgression,
            final SkillService skillService,
            final InventoryService inventoryService,
            final CharacterSkillRepository characterSkillRepository,
            final SkillDamagePolicy skillDamagePolicy,
            final GameProperties gameProperties,
            final GameMessageService gameMessageService,
            final Random random) {
        this.statProgression = statProgression;
        this.skillService = skillService;
        this.inventoryService = inventoryService;
        this.characterSkillRepository = characterSkillRepository;
        this.skillDamagePolicy = skillDamagePolicy;
        this.gameProperties = gameProperties;
        this.gameMessageService = gameMessageService;
        this.random = random;
    }

    /**
     * 캐릭터가 보유한 스킬 정보를 조회한다 (중복 통합 헬퍼).
     *
     * @param progress 캐릭터 진행 상태
     * @param skill 스킬 도메인 모델
     * @return 캐릭터 보유 스킬 정보 (미보유 시 empty)
     */
    public Optional<CharacterSkill> findCharacterSkill(
            final CharacterProgress progress, final Skill skill) {
        if (progress == null || skill == null) {
            return Optional.empty();
        }
        return characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id());
    }

    /**
     * 캐릭터의 기본 스탯(레벨)과 장비 보너스, 스킬 랭크업 보너스를 합산한 총 스탯을 계산한다 (중복 통합 헬퍼).
     *
     * @param progress 캐릭터 진행 상태
     * @return 합산된 총 스탯
     */
    public Stats aggregatedStats(final CharacterProgress progress) {
        final Stats levelStats =
                statProgression.levelStatsFor(progress.getCurrentLevel(), progress.getTalent());
        final EquippedBonusResult equipBonus = resolveEquippedBonus(progress);
        final Stats skillBonus = skillService.rankupBonus(progress.getId());

        return new Stats(
                levelStats.str() + equipBonus.statBonus().str() + skillBonus.str(),
                levelStats.dex() + equipBonus.statBonus().dex() + skillBonus.dex(),
                levelStats.intelligence()
                        + equipBonus.statBonus().intelligence()
                        + skillBonus.intelligence(),
                levelStats.critical() + equipBonus.statBonus().critical() + skillBonus.critical(),
                levelStats.defense() + equipBonus.statBonus().defense() + skillBonus.defense());
    }

    /**
     * 착용 무기 재능·주스탯·장비/스킬 보너스로 공격력을 산출한다.
     *
     * <p>공격력 = round(주스탯 × 재능계수). 주스탯은 레벨 스탯 + 장비 보너스 + 스킬 랭크업 보너스에서 재능별 대응 필드를 추출한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param equippedTalent 착용 무기 재능
     * @return 계산된 공격력
     */
    public int attackPower(final CharacterProgress progress, final SkillTalent equippedTalent) {
        final Stats aggregated = aggregatedStats(progress);
        final int primaryStat =
                resolvePrimaryStat(
                        equippedTalent,
                        aggregated.str(),
                        aggregated.dex(),
                        aggregated.intelligence());
        final double coefficient = resolveCoefficient(equippedTalent);

        return (int) Math.round(primaryStat * coefficient);
    }

    /**
     * 재능에 대응하는 주스탯 수치를 반환한다.
     *
     * @param talent 재능 종류
     * @param str 힘
     * @param dex 민첩
     * @param intelligence 지능
     * @return 해당 재능의 주스탯 수치
     */
    public int resolvePrimaryStat(
            final SkillTalent talent, final int str, final int dex, final int intelligence) {
        return switch (talent) {
            case MELEE, COMMON -> str;
            case ARCHERY -> dex;
            case MAGIC -> intelligence;
        };
    }

    /**
     * 재능에 대응하는 공격력 보정 계수를 반환한다.
     *
     * @param talent 재능 종류
     * @return 공격력 보정 계수
     */
    public double resolveCoefficient(final SkillTalent talent) {
        final GameProperties.BattleProperties battle = gameProperties.battle();
        return switch (talent) {
            case MELEE, COMMON -> battle.meleeCoef();
            case ARCHERY -> battle.archeryCoef();
            case MAGIC -> battle.magicCoef();
        };
    }

    /**
     * 캐릭터가 착용한 장비의 보너스 능력치를 산출한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 장비 보너스 결과
     */
    public EquippedBonusResult resolveEquippedBonus(final CharacterProgress progress) {
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
     * 캐릭터의 총 방어력을 산출한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 총 방어력
     */
    public int resolvePlayerDefense(final CharacterProgress progress) {
        return aggregatedStats(progress).defense();
    }

    /**
     * 캐릭터의 기본 치명타 확률(0.1% 단위)을 산출한다.
     *
     * @param progress 캐릭터 진행 상태
     * @return 기본 치명타 확률 (0.1% 단위)
     */
    public int resolvePlayerCritical(final CharacterProgress progress) {
        return aggregatedStats(progress).critical();
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
    public int resolveEffectivePlayerCritical(final Skill skill, final CharacterProgress progress) {
        final int baseCritical = resolvePlayerCritical(progress);
        final int bonus =
                switch (skill) {
                    case DamageSkill ds -> ds.critBonus();
                    case DefenseSkill defSkill -> {
                        final Optional<CharacterSkill> csOpt = findCharacterSkill(progress, skill);
                        yield csOpt.map(cs -> defSkill.critBonusAt(cs.getRank())).orElse(0);
                    }
                    default -> 0;
                };
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
    public int resolvePlayerHitCount(final Skill skill) {
        return switch (skill) {
            case DamageSkill ds -> ds.hitCount();
            default -> 1;
        };
    }

    /**
     * 캐릭터의 스킬 랭크에 대응하는 대미지 배율을 산출한다.
     *
     * @param skill 플레이어가 사용하는 스킬
     * @param progress 캐릭터 진행 상태
     * @return 대미지 배율 (백분율)
     */
    public int resolvePlayerMultiplier(final Skill skill, final CharacterProgress progress) {
        final Optional<CharacterSkill> csOpt = findCharacterSkill(progress, skill);
        if (csOpt.isEmpty()) {
            return gameProperties.battle().monsterNormalMultiplier();
        }
        final CharacterSkill cs = csOpt.get();
        return switch (skill) {
            case DamageSkill damageSkill -> skillDamagePolicy.multiplier(damageSkill, cs.getRank());
            case DotSkill dotSkill -> dotSkill.initialMultiplierAt(cs.getRank());
            default -> gameProperties.battle().monsterNormalMultiplier();
        };
    }

    /**
     * 방어 스킬 사용 시 블록 확률을 산출한다.
     *
     * @param skill 플레이어가 사용하는 스킬
     * @param progress 캐릭터 진행 상태
     * @return 블록 확률
     */
    public int resolvePlayerBlockRate(final Skill skill, final CharacterProgress progress) {
        return switch (skill) {
            case DefenseSkill defenseSkill -> {
                final Optional<CharacterSkill> csOpt = findCharacterSkill(progress, skill);
                yield csOpt.map(cs -> skillDamagePolicy.blockRate(defenseSkill, cs.getRank()))
                        .orElse(0);
            }
            default -> 0;
        };
    }

    /**
     * 카운터 스킬 사용 시 카운터 피해 퍼센트를 산출한다.
     *
     * @param skill 플레이어가 사용하는 스킬
     * @param progress 캐릭터 진행 상태
     * @return 카운터 배율
     */
    public int resolvePlayerCounterPercent(final Skill skill, final CharacterProgress progress) {
        return switch (skill) {
            case DefenseSkill defenseSkill -> {
                final Optional<CharacterSkill> csOpt = findCharacterSkill(progress, skill);
                yield csOpt.map(
                                cs ->
                                        skillDamagePolicy.counterMultiplier(
                                                defenseSkill, cs.getRank()))
                        .orElse(0);
            }
            default -> 0;
        };
    }

    /**
     * 캐릭터의 스킬 랭크를 반영한 자원 소모량을 계산한다.
     *
     * @param skill 대상 스킬
     * @param progress 캐릭터 진행 상태
     * @return 계산된 자원 소모량
     */
    public int resolveResourceCost(final Skill skill, final CharacterProgress progress) {
        final Optional<CharacterSkill> csOpt = findCharacterSkill(progress, skill);
        if (csOpt.isPresent()) {
            return skill.resourceCostAt(csOpt.get().getRank());
        }
        return skill.resourceCost();
    }

    /**
     * 캐릭터가 스킬 사용에 필요한 자원을 충분히 보유하고 있는지 확인한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param kind 자원 종류 (STAMINA 또는 MP)
     * @param cost 필요 자원량
     * @return 자원 충족 여부
     */
    public boolean hasEnoughResource(
            final CharacterProgress progress, final ResourceKind kind, final int cost) {
        return switch (kind) {
            case STAMINA -> progress.getStaminaCurrent() >= cost;
            case MP -> progress.getMpCurrent() >= cost;
        };
    }

    /**
     * 캐릭터의 자원을 소모한다.
     *
     * @param progress 캐릭터 진행 상태
     * @param kind 자원 종류 (STAMINA 또는 MP)
     * @param cost 소모할 자원량
     */
    public void deductResource(
            final CharacterProgress progress, final ResourceKind kind, final int cost) {
        if (kind == ResourceKind.STAMINA) {
            progress.spendStamina(cost);
        } else {
            progress.spendMp(cost);
        }
    }

    /**
     * 마법 스킬의 캐스팅 실패 여부를 판정하고, 실패 시 로그에 메시지를 추가한다.
     *
     * @param skill 사용하려는 스킬
     * @param logLines 전투 로그 라인 리스트
     * @return 캐스팅 실패 여부
     */
    public boolean checkMagicCastFailure(final Skill skill, final List<String> logLines) {
        if (skill.talent() != SkillTalent.MAGIC) {
            return false;
        }
        if (skill.type() == SkillType.DEFENSE) {
            return false;
        }
        final int failRate = gameProperties.battle().magicFailRate();
        final boolean failed = random.nextInt(PERCENT_DIVISOR) < failRate;
        if (failed) {
            final String failMsg = gameMessageService.get("battle.cast_fail", skill.label());
            logLines.add(failMsg);
        }
        return failed;
    }
}
