package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link PlayScreenViewHelper}의 게이지 계산 및 뷰 조립 단위 테스트. */
class PlayScreenViewHelperTest {

    private PlayScreenViewHelper helper;

    @BeforeEach
    void setUp() {
        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);
        helper =
                new PlayScreenViewHelper(
                        new ExperiencePolicy(),
                        new StatProgression(),
                        skillService,
                        inventoryService);
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
        // EXP: current=0, max=requiredForNext(1)=65
        assertThat(topBar.exp().current()).isEqualTo(0);
        assertThat(topBar.exp().max()).isEqualTo(65);
        assertThat(topBar.exp().percent()).isEqualTo(0);
        assertThat(topBar.exp().overlay()).isEqualTo("0 / 65");
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
        final List<ActionLogEntry> logs =
                List.of(new ActionLogEntry("2024-01-01 12:00:00", "이동했습니다.", "move"));

        final PlayScreenView view =
                helper.buildPlayScreen(progress, minimap, fullMap, ambience, logs);

        assertThat(view.topBar()).isNotNull();
        assertThat(view.topBar().nickname()).isEqualTo("고니");
        assertThat(view.minimap()).isEqualTo(minimap);
        assertThat(view.fullMap()).isEqualTo(fullMap);
        assertThat(view.ambience()).isEqualTo(ambience);
        assertThat(view.logs()).hasSize(1);
    }

    @Test
    void should_buildPlayScreen_withNullNpc_when_noTalkingNpc() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MinimapView minimap = new MinimapView("티르 코네일", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 5, 5);
        final List<InteractionItem> interactions =
                List.of(new InteractionItem("neris", "네리스 (대장간)", true));
        final List<ActionLogEntry> logs = List.of();

        final PlayScreenView view =
                helper.buildPlayScreen(
                        progress, minimap, fullMap, "평화로운 마을", interactions, null, null, logs);

        assertThat(view.npcName()).isNull();
        assertThat(view.npcDialogue()).isNull();
        assertThat(view.npcActions()).isNull();
        assertThat(view.interactions()).isEqualTo(interactions);
    }

    @Test
    void should_buildPlayScreen_withNpcActions_when_talkingNpcProvided() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MinimapView minimap = new MinimapView("티르 코네일", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 5, 5);
        final NpcLines lines = new NpcLines(List.of("안녕"), Map.of());
        final Npc npc =
                new Npc("neris", "네리스", NpcType.BLACKSMITH, "tir-chonaill", "무뚝뚝한 대장장이", lines);
        final List<ActionLogEntry> logs = List.of();

        final PlayScreenView view =
                helper.buildPlayScreen(
                        progress, minimap, fullMap, "평화로운 마을", null, npc, "반갑다.", logs);

        assertThat(view.npcName()).isEqualTo("네리스");
        assertThat(view.npcDialogue()).isEqualTo("반갑다.");
        assertThat(view.npcActions()).hasSize(2);
        assertThat(view.npcActions().get(0).label()).isEqualTo("상점");
        assertThat(view.npcActions().get(1).label()).isEqualTo("수리");
    }

    @Test
    void should_buildInteractions_when_npcListProvided() {
        final NpcLines lines = new NpcLines(List.of("안녕"), Map.of());
        final Npc neris =
                new Npc("neris", "네리스", NpcType.BLACKSMITH, "tir-chonaill", "무뚝뚝한 대장장이", lines);
        final Npc duncan = new Npc("duncan", "던컨", NpcType.CHIEF, "tir-chonaill", "마을의 촌장", lines);

        final List<InteractionItem> items = helper.buildInteractions(List.of(neris, duncan));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).id()).isEqualTo("neris");
        assertThat(items.get(0).name()).isEqualTo("네리스 ⚒️");
        assertThat(items.get(0).npc()).isTrue();
        assertThat(items.get(1).id()).isEqualTo("duncan");
        assertThat(items.get(1).name()).isEqualTo("던컨 🏡");
        assertThat(items.get(1).npc()).isTrue();
    }

    @Test
    void should_buildInteractions_when_emptyList() {
        final List<InteractionItem> items = helper.buildInteractions(List.of());

        assertThat(items).isEmpty();
    }

    @Test
    void should_populateAmbienceEmojiAndMonsterBoss_when_buildPlayScreen() {
        final java.time.Instant fixedInstant = java.time.Instant.parse("2026-08-27T12:00:00Z");
        final java.time.Clock fixedClock =
                java.time.Clock.fixed(fixedInstant, java.time.ZoneId.of("UTC"));
        final SkillService skillService = mock(SkillService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final PlayScreenViewHelper clockHelper =
                new PlayScreenViewHelper(
                        new ExperiencePolicy(),
                        new StatProgression(),
                        skillService,
                        inventoryService,
                        fixedClock);

        final CharacterProgress progress = CharacterProgress.createDefault();
        final MinimapView minimap = mock(MinimapView.class);
        final FullMapView fullMap = mock(FullMapView.class);

        final PlayScreenView view =
                clockHelper.buildPlayScreen(progress, minimap, fullMap, "테스트 상황", List.of());

        assertThat(view.ambienceEmoji()).isEqualTo("☀️");
        assertThat(view.timeOfDayKey()).isEqualTo("afternoon");
    }
}
