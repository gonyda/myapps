package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.NpcActionButton;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.Vital;

/**
 * 플레이 화면 뷰 모델 조립 및 표현 계산을 담당하는 헬퍼 컴포넌트.
 *
 * <p>게이지 퍼센트/오버레이 계산, 상단바 뷰 모델 조립, 플레이 화면 전체 뷰 집계 등
 * 컨트롤러에서 Thymeleaf 템플릿으로 전달할 뷰 데이터를 생성한다.
 * {@code CalendarViewHelper}와 동일한 패턴으로 순수 계산 로직만 포함한다.
 */
@Component
public class PlayScreenViewHelper {

    private static final int PERCENT_MULTIPLIER = 100;
    private static final int PERCENT_MIN = 0;
    private static final int PERCENT_MAX = 100;

    private final ExperiencePolicy experiencePolicy;

    /**
     * PlayScreenViewHelper를 생성한다.
     *
     * @param experiencePolicy 경험치 정책 (EXP 게이지 최대값 산출용)
     */
    public PlayScreenViewHelper(final ExperiencePolicy experiencePolicy) {
        this.experiencePolicy = experiencePolicy;
    }

    /**
     * 현재값과 최대값으로 게이지 뷰 모델을 생성한다.
     *
     * <p>퍼센트는 {@code max > 0}이면 {@code clamp(round(current*100/max), 0, 100)},
     * 아니면 0이다. 오버레이는 {@code "current / max"} 형식이다.
     *
     * @param current 현재값
     * @param max     최대값
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
     * @param progress 캐릭터 진행상황
     * @return 상단바 뷰 모델
     */
    public TopBarView buildTopBar(final CharacterProgress progress) {
        final int level = progress.getCurrentLevel();
        final long requiredExp = experiencePolicy.requiredForNext(level);
        final GaugeView exp = buildGauge((int) progress.getExperience(), (int) requiredExp);
        final GaugeView hp = buildVitalGauge(progress.getHp());
        final GaugeView mp = buildVitalGauge(progress.getMp());
        final GaugeView stamina = buildVitalGauge(progress.getStamina());
        return new TopBarView(progress.getNickname(), level, exp, hp, mp, stamina);
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 (하위 호환 오버로드).
     *
     * <p>상호작용 목록·NPC 대사·행동 버튼 없이 뷰를 조립한다.
     * 내부적으로 확장 메서드에 {@code null} 인자를 전달한다.
     *
     * @param progress 캐릭터 진행상황
     * @param minimap  미니맵 뷰 모델
     * @param fullMap  전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param logs     행동 로그 항목 목록
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(final CharacterProgress progress,
                                          final MinimapView minimap,
                                          final FullMapView fullMap,
                                          final String ambience,
                                          final List<ActionLogEntry> logs) {
        return buildPlayScreen(progress, minimap, fullMap, ambience, null, null, null, logs);
    }

    /**
     * 플레이 화면 전체 뷰를 집계한다 (상호작용·NPC 대사·행동 버튼 포함).
     *
     * <p>{@code talkingNpc}가 {@code null}이면 NPC 이름·대사·행동 버튼을 모두 비운다.
     * {@code talkingNpc}가 존재하면 이름·대사를 채우고, 해당 타입의 행동 라벨을
     * 정의 순서대로 {@link NpcActionButton}으로 변환한다.
     *
     * @param progress     캐릭터 진행상황
     * @param minimap      미니맵 뷰 모델
     * @param fullMap      전체지도 뷰 모델
     * @param ambience     상황 멘트 텍스트
     * @param interactions 상호작용 대상 목록 (NPC 버튼, 정의 순서)
     * @param talkingNpc   대사 대상 NPC (없으면 {@code null})
     * @param dialogue     선택된 대사 텍스트 (없으면 {@code null})
     * @param logs         행동 로그 항목 목록
     * @return 플레이 화면 전체 뷰 모델
     */
    public PlayScreenView buildPlayScreen(final CharacterProgress progress,
                                          final MinimapView minimap,
                                          final FullMapView fullMap,
                                          final String ambience,
                                          final List<InteractionItem> interactions,
                                          final Npc talkingNpc,
                                          final String dialogue,
                                          final List<ActionLogEntry> logs) {
        final TopBarView topBar = buildTopBar(progress);
        final String npcName = talkingNpc != null ? talkingNpc.name() : null;
        final String npcDialogue = talkingNpc != null ? dialogue : null;
        final List<NpcActionButton> npcActions = buildNpcActions(talkingNpc);
        return new PlayScreenView(topBar, minimap, fullMap, ambience, npcName, npcDialogue, interactions, npcActions, logs);
    }

    /**
     * NPC 목록을 상호작용 항목 목록으로 변환한다.
     *
     * <p>각 NPC의 라벨은 {@code "name (type.label())"} 형식이며, {@code npc=true}로 표시된다.
     * 반환 목록은 입력 NPC 목록의 정의 순서를 보존한다.
     *
     * @param npcs NPC 목록 (정의 순서)
     * @return 상호작용 항목 목록
     */
    public List<InteractionItem> buildInteractions(final List<Npc> npcs) {
        return npcs.stream()
                .map(this::toInteractionItem)
                .toList();
    }

    private GaugeView buildVitalGauge(final Vital vital) {
        return buildGauge(vital.current(), vital.max());
    }

    private List<NpcActionButton> buildNpcActions(final Npc talkingNpc) {
        if (talkingNpc == null) {
            return null;
        }
        return talkingNpc.type().actionLabels().stream()
                .map(NpcActionButton::new)
                .toList();
    }

    private InteractionItem toInteractionItem(final Npc npc) {
        final String label = npc.name() + " (" + npc.type().label() + ")";
        return new InteractionItem(npc.id(), label, true);
    }

    private int calculatePercent(final int current, final int max) {
        if (max <= 0) {
            return PERCENT_MIN;
        }
        final long raw = Math.round((double) current * PERCENT_MULTIPLIER / max);
        return (int) Math.max(PERCENT_MIN, Math.min(PERCENT_MAX, raw));
    }
}
