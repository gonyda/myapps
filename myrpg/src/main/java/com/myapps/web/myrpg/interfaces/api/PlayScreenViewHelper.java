package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
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
     * 플레이 화면 전체 뷰를 집계한다.
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
        final TopBarView topBar = buildTopBar(progress);
        return new PlayScreenView(topBar, minimap, fullMap, ambience, null, null, null, logs);
    }

    private GaugeView buildVitalGauge(final Vital vital) {
        return buildGauge(vital.current(), vital.max());
    }

    private int calculatePercent(final int current, final int max) {
        if (max <= 0) {
            return PERCENT_MIN;
        }
        final long raw = Math.round((double) current * PERCENT_MULTIPLIER / max);
        return (int) Math.max(PERCENT_MIN, Math.min(PERCENT_MAX, raw));
    }
}
