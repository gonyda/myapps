package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PlayScreenViewHelper}의 게이지 계산 및 뷰 조립 단위 테스트.
 */
class PlayScreenViewHelperTest {

    private PlayScreenViewHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PlayScreenViewHelper(new ExperiencePolicy());
    }

    @Test
    void should_calculatePercent_when_currentIsZero() {
        final GaugeView gauge = helper.buildGauge(0, 100);

        assertThat(gauge.percent()).isEqualTo(0);
        assertThat(gauge.overlay()).isEqualTo("0 / 100");
    }

    @Test
    void should_calculatePercent_when_currentEqualsMax() {
        final GaugeView gauge = helper.buildGauge(100, 100);

        assertThat(gauge.percent()).isEqualTo(100);
        assertThat(gauge.overlay()).isEqualTo("100 / 100");
    }

    @Test
    void should_calculatePercent_when_currentIsHalfOfMax() {
        final GaugeView gauge = helper.buildGauge(50, 100);

        assertThat(gauge.percent()).isEqualTo(50);
        assertThat(gauge.overlay()).isEqualTo("50 / 100");
    }

    @Test
    void should_returnZeroPercent_when_maxIsZero() {
        final GaugeView gauge = helper.buildGauge(50, 0);

        assertThat(gauge.percent()).isEqualTo(0);
        assertThat(gauge.overlay()).isEqualTo("50 / 0");
    }

    @Test
    void should_roundPercent_when_resultIsNotInteger() {
        // 33 / 100 = 33%, exact
        final GaugeView gauge = helper.buildGauge(33, 100);
        assertThat(gauge.percent()).isEqualTo(33);

        // 1 / 3 = 33.33... → rounds to 33
        final GaugeView gauge2 = helper.buildGauge(1, 3);
        assertThat(gauge2.percent()).isEqualTo(33);

        // 2 / 3 = 66.66... → rounds to 67
        final GaugeView gauge3 = helper.buildGauge(2, 3);
        assertThat(gauge3.percent()).isEqualTo(67);
    }

    @Test
    void should_clampPercent_when_currentExceedsMax() {
        final GaugeView gauge = helper.buildGauge(150, 100);

        assertThat(gauge.percent()).isEqualTo(100);
    }

    @Test
    void should_buildTopBar_when_defaultCharacter() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final TopBarView topBar = helper.buildTopBar(progress);

        assertThat(topBar.nickname()).isEqualTo("고니");
        assertThat(topBar.level()).isEqualTo(1);
        // EXP: current=0, max=requiredForNext(1)=100
        assertThat(topBar.exp().current()).isEqualTo(0);
        assertThat(topBar.exp().max()).isEqualTo(100);
        assertThat(topBar.exp().percent()).isEqualTo(0);
        assertThat(topBar.exp().overlay()).isEqualTo("0 / 100");
        // HP: 100/100
        assertThat(topBar.hp().percent()).isEqualTo(100);
        assertThat(topBar.hp().overlay()).isEqualTo("100 / 100");
        // MP: 100/100
        assertThat(topBar.mp().percent()).isEqualTo(100);
        // Stamina: 100/100
        assertThat(topBar.stamina().percent()).isEqualTo(100);
    }

    @Test
    void should_buildPlayScreen_when_allDataProvided() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MinimapView minimap = new MinimapView("티르 코네일", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 5, 5);
        final String ambience = "마을 광장이 한적합니다.";
        final List<ActionLogEntry> logs = List.of(
                new ActionLogEntry("2024-01-01 12:00:00", "이동했습니다.", "move")
        );

        final PlayScreenView view = helper.buildPlayScreen(progress, minimap, fullMap, ambience, logs);

        assertThat(view.topBar()).isNotNull();
        assertThat(view.topBar().nickname()).isEqualTo("고니");
        assertThat(view.minimap()).isEqualTo(minimap);
        assertThat(view.fullMap()).isEqualTo(fullMap);
        assertThat(view.ambience()).isEqualTo(ambience);
        assertThat(view.logs()).hasSize(1);
    }
}
