package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.ActionButton;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.StatLine;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.TimeOfDay;
import com.myapps.web.myrpg.domain.model.VitalMax;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 플레이 화면 뷰 모델 조립 및 표현 계산을 담당하는 헬퍼 컴포넌트.
 *
 * <p>게이지 퍼센트/오버레이 계산, 상단바 뷰 모델 조립, 플레이 화면 전체 뷰 집계 등 컨트롤러에서 Thymeleaf 템플릿으로 전달할 뷰 데이터를 생성한다. {@code
 * CalendarViewHelper}와 동일한 패턴으로 순수 계산 로직만 포함한다.
 */
@Component
public class PlayScreenViewHelper {

    private static final int PERCENT_MULTIPLIER = 100;
    private static final int PERCENT_MIN = 0;
    private static final int PERCENT_MAX = 100;
    private static final int MAX_LEVEL = 100;
    private static final String MAX_LEVEL_OVERLAY = "MAX";
    private static final int CRITICAL_DIVISOR = 10;
    private static final int CRITICAL_MOD = 10;
    private static final long MINUTES_PER_HOUR = 60;

    private final ExperiencePolicy experiencePolicy;
    private final StatProgression statProgression;
    private final SkillService skillService;
    private final InventoryService inventoryService;
    private final Clock clock;

    /**
     * PlayScreenViewHelper를 생성한다.
     *
     * @param experiencePolicy 경험치 정책 (EXP 게이지 최대값 산출용)
     * @param statProgression 스탯 진행 정책 (HP/MP/Stamina 최대값 산출용)
     * @param skillService 스킬 서비스 (랭크업 영구 보너스 합산용)
     * @param inventoryService 인벤토리 서비스 (장비 보너스 합산용)
     * @param clock 시간 산출용 Clock
     */
    @org.springframework.beans.factory.annotation.Autowired
    public PlayScreenViewHelper(
            final ExperiencePolicy experiencePolicy,
            final StatProgression statProgression,
            final SkillService skillService,
            final InventoryService inventoryService,
            final Clock clock) {
        this.experiencePolicy = experiencePolicy;
        this.statProgression = statProgression;
        this.skillService = skillService;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    /** 4인자 하위 호환 생성자 (기본 Clock 사용). */
    public PlayScreenViewHelper(
            final ExperiencePolicy experiencePolicy,
            final StatProgression statProgression,
            final SkillService skillService,
            final InventoryService inventoryService) {
        this(
                experiencePolicy,
                statProgression,
                skillService,
                inventoryService,
                Clock.system(ZoneId.of("Asia/Seoul")));
    }

    /**
     * 현재값과 최대값으로 게이지 뷰 모델을 생성한다.
     *
     * <p>퍼센트는 {@code max > 0}이면 {@code clamp(round(current*100/max), 0, 100)}, 아니면 0이다. 오버레이는
     * {@code "current / max"} 형식이다.
     *
     * @param current 현재값
     * @param max 최대값
     * @return 게이지 뷰 모델
     */
    public GaugeView buildGauge(final int current, final int max) {
        final int percent = calculatePercent(current, max);
        final String overlay = current + " / " + max;
        return new GaugeView(current, max, percent, overlay);
    }

    /**
     * 캐릭터 진행상황으로부터 상단바 뷰 모델을 조립한다.
     *
     * <p>EXP 게이지는 최대 레벨(100)이면 퍼센트 100, 오버레이 "MAX"를 표시하고, 미만이면 현재 경험치 / 다음 레벨 필요 경험치로 산출한다.
     * HP/MP/Stamina 게이지는 현재값(저장) / vitalMaxFor(level, talent)의 각 대응 필드로 조립한다.
     *
     * @param progress 캐릭터 진행상황
     * @return 상단바 뷰 모델
     */
    public TopBarView buildTopBar(final CharacterProgress progress) {
        final int level = progress.getCurrentLevel();
        final GaugeView exp = buildExpGauge(progress, level);
        final VitalGauges gauges = buildVitalGauges(progress);
        return new TopBarView(
                progress.getNickname(), level, exp, gauges.hp(), gauges.mp(), gauges.stamina());
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 (하위 호환 오버로드).
     *
     * <p>상호작용 목록·NPC 대사·행동 버튼 없이 뷰를 조립한다. 내부적으로 확장 메서드에 {@code null} 인자를 전달한다.
     *
     * @param progress 캐릭터 진행상황
     * @param minimap 미니맵 뷰 모델
     * @param fullMap 전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param logs 행동 로그 항목 목록
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(
            final CharacterProgress progress,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final List<ActionLogEntry> logs) {
        return buildPlayScreen(progress, minimap, fullMap, ambience, null, null, null, logs);
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 (상호작용·NPC 대사·행동 버튼 포함).
     *
     * <p>{@code talkingNpc}가 {@code null}이면 NPC 이름·대사·행동 버튼을 모두 비운다. {@code talkingNpc}가 존재하면
     * 이름·대사를 채우고, 해당 타입의 행동 라벨을 정의 순서대로 {@link ActionButton}으로 변환한다.
     *
     * @param progress 캐릭터 진행상황
     * @param minimap 미니맵 뷰 모델
     * @param fullMap 전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param interactions 상호작용 대상 목록 (NPC 버튼, 정의 순서)
     * @param talkingNpc 대사 대상 NPC (없으면 {@code null})
     * @param dialogue 선택된 대사 텍스트 (없으면 {@code null})
     * @param logs 행동 로그 항목 목록
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(
            final CharacterProgress progress,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final List<InteractionItem> interactions,
            final Npc talkingNpc,
            final String dialogue,
            final List<ActionLogEntry> logs) {
        return buildPlayScreen(
                progress,
                minimap,
                fullMap,
                ambience,
                interactions,
                talkingNpc,
                dialogue,
                logs,
                null);
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 (상호작용·NPC 대사·행동 버튼·정보 팝업 포함).
     *
     * <p>{@code talkingNpc}가 {@code null}이면 NPC 이름·대사·행동 버튼을 모두 비운다. {@code talkingNpc}가 존재하면
     * 이름·대사를 채우고, 해당 타입의 행동 라벨을 정의 순서대로 {@link ActionButton}으로 변환한다. 내부적으로 {@link TalkTarget} 기반
     * 오버로드에 위임한다.
     *
     * @param progress 캐릭터 진행상황
     * @param minimap 미니맵 뷰 모델
     * @param fullMap 전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param interactions 상호작용 대상 목록 (NPC 버튼, 정의 순서)
     * @param talkingNpc 대사 대상 NPC (없으면 {@code null})
     * @param dialogue 선택된 대사 텍스트 (없으면 {@code null})
     * @param logs 행동 로그 항목 목록
     * @param info 정보 팝업 뷰 모델 (없으면 {@code null})
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(
            final CharacterProgress progress,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final List<InteractionItem> interactions,
            final Npc talkingNpc,
            final String dialogue,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        final TalkTarget talkTarget =
                talkingNpc != null ? TalkTarget.ofNpc(talkingNpc, dialogue) : TalkTarget.EMPTY;
        return buildPlayScreen(
                progress, minimap, fullMap, ambience, interactions, talkTarget, logs, info);
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 ({@link TalkTarget} 기반 통합 오버로드).
     *
     * <p>{@code talkTarget}의 NPC가 존재하면 NPC 슬롯(이름·대사·행동 버튼)을 채우고 몬스터 슬롯을 비운다. 몬스터가 존재하면 몬스터
     * 슬롯(이름·대사·레벨·최대HP·행동 버튼)을 채우고 NPC 슬롯을 비운다. 대상이 없으면({@link TalkTarget#EMPTY}) 양쪽 모두 비운다.
     *
     * @param progress 캐릭터 진행상황
     * @param minimap 미니맵 뷰 모델
     * @param fullMap 전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param interactions 상호작용 대상 목록
     * @param talkTarget 대사 대상 묶음 (NPC 또는 몬스터 또는 없음)
     * @param logs 행동 로그 항목 목록
     * @param info 정보 팝업 뷰 모델 (없으면 {@code null})
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(
            final CharacterProgress progress,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final List<InteractionItem> interactions,
            final TalkTarget talkTarget,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        final TopBarView topBar = buildTopBar(progress);

        String npcName = null;
        String npcDialogue = null;
        List<ActionButton> npcActions = null;
        String monsterName = null;
        String monsterDialogue = null;
        Integer monsterLevel = null;
        Integer monsterMaxHp = null;
        List<ActionButton> monsterActions = null;

        if (talkTarget.npc() != null) {
            npcName = talkTarget.npc().name();
            npcDialogue = talkTarget.dialogue();
            npcActions = buildNpcActions(talkTarget.npc());
        } else if (talkTarget.monster() != null) {
            monsterName = talkTarget.monster().name();
            monsterDialogue = talkTarget.dialogue();
            monsterLevel = talkTarget.monster().level();
            monsterMaxHp = talkTarget.monster().maxHp();
            monsterActions = buildMonsterActions(talkTarget.monster());
        }

        final String ambienceEmoji = TimeOfDay.fromHour(LocalDateTime.now(clock).getHour()).emoji();
        final boolean monsterBoss =
                talkTarget.monster() != null && talkTarget.monster().type() == MonsterType.BOSS;

        return new PlayScreenView(
                topBar,
                minimap,
                fullMap,
                ambience,
                ambienceEmoji,
                npcName,
                npcDialogue,
                interactions,
                npcActions,
                monsterName,
                monsterDialogue,
                monsterLevel,
                monsterMaxHp,
                monsterActions,
                monsterBoss,
                logs,
                info);
    }

    /**
     * NPC 목록을 상호작용 항목 목록으로 변환한다.
     *
     * <p>각 NPC의 라벨은 {@code "name emoji"} 형식이며, {@code npc=true}로 표시된다. 반환 목록은 입력 NPC 목록의 정의 순서를
     * 보존한다. 내부적으로 몬스터 빈 목록과 함께 2인자 버전에 위임한다.
     *
     * @param npcs NPC 목록 (정의 순서)
     * @return 상호작용 항목 목록
     */
    public List<InteractionItem> buildInteractions(final List<Npc> npcs) {
        return buildInteractions(npcs, List.of());
    }

    /**
     * NPC와 몬스터 목록을 하나의 상호작용 항목 목록으로 합친다.
     *
     * <p>NPC 항목이 먼저 배치되고({@code npc=true}), 이어서 몬스터 항목({@code npc=false})이 각 입력 목록의 정의 순서를 보존하며
     * 합류한다.
     *
     * @param npcs NPC 목록 (정의 순서)
     * @param monsters 몬스터 목록 (정의 순서)
     * @return NPC + 몬스터 상호작용 항목 합류 목록
     */
    public List<InteractionItem> buildInteractions(
            final List<Npc> npcs, final List<Monster> monsters) {
        final List<InteractionItem> npcItems = npcs.stream().map(this::toInteractionItem).toList();
        final List<InteractionItem> monsterItems =
                monsters.stream().map(this::toMonsterInteractionItem).toList();
        final ArrayList<InteractionItem> combined =
                new ArrayList<>(npcItems.size() + monsterItems.size());
        combined.addAll(npcItems);
        combined.addAll(monsterItems);
        return List.copyOf(combined);
    }

    /**
     * 정보 팝업 뷰 모델을 조립한다.
     *
     * <p>상단: 닉네임, 현재 레벨, 누적 레벨, 재능 라벨, 보유 AP, 재능 효과 요약, HP·MP·Stamina 게이지. 중앙: {@code
     * levelStatsFor(level, talent)} 본체 + {@code skillService.rankupBonus(characterId)}로 StatLine
     * 목록. 하단: 환생 가능 여부, 경과 텍스트.
     *
     * @param progress 캐릭터 진행상황
     * @param rebirthStatus 환생 상태 정보
     * @return 정보 팝업 뷰 모델
     */
    public InfoPopupView buildInfo(
            final CharacterProgress progress, final RebirthStatus rebirthStatus) {
        final int level = progress.getCurrentLevel();
        final TalentType talent = progress.getTalent();
        final VitalGauges gauges = buildVitalGauges(progress);
        EquippedBonusResult equipBonus = null;
        if (progress.getId() != null) {
            equipBonus = inventoryService.equippedBonus(progress.getId());
        }
        if (equipBonus == null) {
            equipBonus = inventoryService.equippedBonus();
        }

        final Stats levelStats = statProgression.levelStatsFor(level, talent);
        final Stats skillBonus = skillService.rankupBonus(progress.getId());
        final Stats equipStatBonus = equipBonus != null ? equipBonus.statBonus() : Stats.ZERO;
        final List<StatLine> stats = buildStatLines(levelStats, skillBonus, equipStatBonus);

        final String elapsedText = rebirthElapsedText(rebirthStatus);

        return new InfoPopupView(
                progress.getNickname(),
                progress.getCurrentLevel(),
                progress.getAccumulatedLevel(),
                talent.label(),
                progress.getAbilityPoints(),
                talent.effectSummary(),
                gauges.hp(),
                gauges.mp(),
                gauges.stamina(),
                stats,
                rebirthStatus.available(),
                elapsedText);
    }

    /**
     * Critical 0.1% 단위 정수를 표시 문자열로 변환한다.
     *
     * <p>예: {@code 50} → {@code "5.0%"}, {@code 347} → {@code "34.7%"}.
     *
     * @param tenths 0.1% 단위 정수
     * @return 포맷된 Critical 문자열
     */
    public String formatCritical(final int tenths) {
        final int whole = tenths / CRITICAL_DIVISOR;
        final int fraction = tenths % CRITICAL_MOD;
        return whole + "." + fraction + "%";
    }

    /**
     * Critical 보너스 0.1% 단위 정수를 델타 표시 문자열로 변환한다.
     *
     * <p>예: {@code 0} → {@code "+0.0%"}, {@code 15} → {@code "+1.5%"}.
     *
     * @param tenths 0.1% 단위 보너스 정수
     * @return 포맷된 Critical 델타 문자열
     */
    public String formatCriticalDelta(final int tenths) {
        final int whole = tenths / CRITICAL_DIVISOR;
        final int fraction = tenths % CRITICAL_MOD;
        return "+" + whole + "." + fraction + "%";
    }

    /**
     * 환생 경과 텍스트를 생성한다.
     *
     * <p>환생한 적이 있으면 {@code "환생 후 {H}시간 {M}분 경과"}, 환생한 적이 없으면 {@code "환생 기록 없음"}을 반환한다.
     *
     * @param rebirthStatus 환생 상태 정보
     * @return 환생 경과 텍스트
     */
    public String rebirthElapsedText(final RebirthStatus rebirthStatus) {
        if (!rebirthStatus.everRebirthed()) {
            return "환생 기록 없음";
        }
        final long totalMinutes = rebirthStatus.elapsed().toMinutes();
        final long hours = totalMinutes / MINUTES_PER_HOUR;
        final long minutes = totalMinutes % MINUTES_PER_HOUR;
        return "환생 후 " + hours + "시간 " + minutes + "분 경과";
    }

    private record VitalGauges(GaugeView hp, GaugeView mp, GaugeView stamina) {}

    private VitalGauges buildVitalGauges(final CharacterProgress progress) {
        final int level = progress.getCurrentLevel();
        final VitalMax baseVitalMax = statProgression.vitalMaxFor(level, progress.getTalent());
        EquippedBonusResult equipBonus = null;
        if (progress.getId() != null) {
            equipBonus = inventoryService.equippedBonus(progress.getId());
        }
        if (equipBonus == null) {
            equipBonus = inventoryService.equippedBonus();
        }
        final VitalMax equipVitalBonus =
                equipBonus != null ? equipBonus.vitalBonus() : new VitalMax(0, 0, 0);
        final VitalMax skillVitalBonus =
                Optional.ofNullable(skillService.rankupVitalBonus(progress.getId()))
                        .orElse(new VitalMax(0, 0, 0));
        final VitalMax vitalMax =
                baseVitalMax
                        .withHpDelta(equipVitalBonus.hp() + skillVitalBonus.hp())
                        .withMpDelta(equipVitalBonus.mp() + skillVitalBonus.mp())
                        .withStaminaDelta(equipVitalBonus.stamina() + skillVitalBonus.stamina());
        final GaugeView hp = buildGauge(progress.getHpCurrent(), vitalMax.hp());
        final GaugeView mp = buildGauge(progress.getMpCurrent(), vitalMax.mp());
        final GaugeView stamina = buildGauge(progress.getStaminaCurrent(), vitalMax.stamina());
        return new VitalGauges(hp, mp, stamina);
    }

    private GaugeView buildExpGauge(final CharacterProgress progress, final int level) {
        if (level == MAX_LEVEL) {
            return new GaugeView(0, 0, PERCENT_MAX, MAX_LEVEL_OVERLAY);
        }
        final long requiredExp = experiencePolicy.requiredForNext(level);
        return buildGauge((int) progress.getExperience(), (int) requiredExp);
    }

    private List<StatLine> buildStatLines(
            final Stats levelStats, final Stats skillBonus, final Stats equipStatBonus) {
        final Stats totalBonus =
                new Stats(
                        skillBonus.str() + equipStatBonus.str(),
                        skillBonus.dex() + equipStatBonus.dex(),
                        skillBonus.intelligence() + equipStatBonus.intelligence(),
                        skillBonus.critical() + equipStatBonus.critical(),
                        skillBonus.defense() + equipStatBonus.defense());
        return List.of(
                new StatLine("STR", String.valueOf(levelStats.str()), "+" + totalBonus.str()),
                new StatLine("DEX", String.valueOf(levelStats.dex()), "+" + totalBonus.dex()),
                new StatLine(
                        "INT",
                        String.valueOf(levelStats.intelligence()),
                        "+" + totalBonus.intelligence()),
                new StatLine(
                        "CRIT",
                        formatCritical(levelStats.critical()),
                        formatCriticalDelta(totalBonus.critical())),
                new StatLine(
                        "DEF", String.valueOf(levelStats.defense()), "+" + totalBonus.defense()));
    }

    private List<ActionButton> buildNpcActions(final Npc talkingNpc) {
        if (talkingNpc == null) {
            return null;
        }
        return talkingNpc.type().actionLabels().stream().map(ActionButton::new).toList();
    }

    private InteractionItem toInteractionItem(final Npc npc) {
        final String label = npc.name() + " " + npc.type().emoji();
        return new InteractionItem(npc.id(), label, true);
    }

    private InteractionItem toMonsterInteractionItem(final Monster monster) {
        return new InteractionItem(monster.id(), monster.buttonLabel(), false);
    }

    private List<ActionButton> buildMonsterActions(final Monster monster) {
        if (monster == null) {
            return null;
        }
        return monster.type().actionLabels().stream().map(ActionButton::new).toList();
    }

    private int calculatePercent(final int current, final int max) {
        if (max <= 0) {
            return PERCENT_MIN;
        }
        final long raw = Math.round((double) current * PERCENT_MULTIPLIER / max);
        return (int) Math.max(PERCENT_MIN, Math.min(PERCENT_MAX, raw));
    }
}
