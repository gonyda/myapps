package com.myapps.web.myrpg.interfaces.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.application.dto.ActionButton;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PlayScreenViewHelper}의 몬스터 관련 기능 단위 테스트.
 *
 * <p>NPC·몬스터 상호작용 합류 순서, TalkTarget.ofMonster 시 몬스터 슬롯 채움,
 * TalkTarget.ofNpc/EMPTY 시 몬스터 슬롯 null 동작을 검증한다.
 *
 * <p><b>Validates: Requirements 10.2, 10.3, 11.6, 11.8</b>
 */
class PlayScreenViewHelperMonsterTest {

    private PlayScreenViewHelper helper;

    @BeforeEach
    void setUp() {
        final SkillService skillService = mock(SkillService.class);
        when(skillService.rankupBonus(any())).thenReturn(Stats.ZERO);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);
        helper = new PlayScreenViewHelper(
                new ExperiencePolicy(), new StatProgression(), skillService, inventoryService);
    }

    @Test
    void should_mergeNpcFirstThenMonsters_when_buildInteractionsCalledWithBothLists() {
        // Given
        final NpcLines lines = new NpcLines(List.of("안녕"), Map.of());
        final Npc neris = new Npc("neris", "네리스", NpcType.BLACKSMITH, "tir-chonaill", "대장장이", lines);
        final Npc duncan = new Npc("duncan", "던컨", NpcType.CHIEF, "tir-chonaill", "촌장", lines);
        final Monster raccoon = createNormalMonster("raccoon", "너구리");
        final Monster wolf = createNormalMonster("wolf", "늑대");

        // When
        final List<InteractionItem> result = helper.buildInteractions(
                List.of(neris, duncan), List.of(raccoon, wolf));

        // Then
        assertThat(result).hasSize(4);
        // NPC first
        assertThat(result.get(0).id()).isEqualTo("neris");
        assertThat(result.get(0).npc()).isTrue();
        assertThat(result.get(1).id()).isEqualTo("duncan");
        assertThat(result.get(1).npc()).isTrue();
        // Monsters after
        assertThat(result.get(2).id()).isEqualTo("raccoon");
        assertThat(result.get(2).npc()).isFalse();
        assertThat(result.get(3).id()).isEqualTo("wolf");
        assertThat(result.get(3).npc()).isFalse();
    }

    @Test
    void should_haveNpcFalse_when_monsterConvertedToInteractionItem() {
        // Given
        final Monster boss = createBossMonster("boss_raccoon", "너구리왕");

        // When
        final List<InteractionItem> result = helper.buildInteractions(List.of(), List.of(boss));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("boss_raccoon");
        assertThat(result.get(0).name()).isEqualTo("너구리왕 👑");
        assertThat(result.get(0).npc()).isFalse();
    }

    @Test
    void should_useButtonLabel_when_monsterConvertedToInteractionItem() {
        // Given — normal monster has no badge
        final Monster raccoon = createNormalMonster("raccoon", "너구리");

        // When
        final List<InteractionItem> result = helper.buildInteractions(List.of(), List.of(raccoon));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("너구리");
    }

    @Test
    void should_fillMonsterSlots_when_talkTargetIsMonster() {
        // Given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MinimapView minimap = new MinimapView("더갈드 아이르", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 5, 5);
        final Monster raccoon = createNormalMonster("raccoon", "너구리");
        final TalkTarget talkTarget = TalkTarget.ofMonster(raccoon, "크르릉… 쉭, 쉭!");
        final List<ActionLogEntry> logs = List.of();

        // When
        final PlayScreenView view = helper.buildPlayScreen(
                progress, minimap, fullMap, "숲속", null, talkTarget, logs, null);

        // Then — monster slots filled
        assertThat(view.monsterName()).isEqualTo("너구리");
        assertThat(view.monsterDialogue()).isEqualTo("크르릉… 쉭, 쉭!");
        assertThat(view.monsterLevel()).isEqualTo(1);
        assertThat(view.monsterMaxHp()).isEqualTo(25);
        assertThat(view.monsterActions()).isNotNull();
        assertThat(view.monsterActions()).hasSize(1);
        assertThat(view.monsterActions().get(0).label()).isEqualTo("전투");
        // NPC slots empty
        assertThat(view.npcName()).isNull();
        assertThat(view.npcDialogue()).isNull();
        assertThat(view.npcActions()).isNull();
    }

    @Test
    void should_leaveMonsterSlotsNull_when_talkTargetIsNpc() {
        // Given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final NpcLines lines = new NpcLines(List.of("반갑다"), Map.of());
        final Npc neris = new Npc("neris", "네리스", NpcType.BLACKSMITH, "tir-chonaill", "대장장이", lines);
        final TalkTarget talkTarget = TalkTarget.ofNpc(neris, "반갑다.");
        final List<ActionLogEntry> logs = List.of();

        // When
        final PlayScreenView view = helper.buildPlayScreen(
                progress, null, null, "마을", null, talkTarget, logs, null);

        // Then — NPC slots filled
        assertThat(view.npcName()).isEqualTo("네리스");
        assertThat(view.npcDialogue()).isEqualTo("반갑다.");
        assertThat(view.npcActions()).isNotNull();
        // Monster slots null
        assertThat(view.monsterName()).isNull();
        assertThat(view.monsterDialogue()).isNull();
        assertThat(view.monsterLevel()).isNull();
        assertThat(view.monsterMaxHp()).isNull();
        assertThat(view.monsterActions()).isNull();
    }

    @Test
    void should_leaveBothSlotsNull_when_talkTargetIsEmpty() {
        // Given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final List<ActionLogEntry> logs = List.of();

        // When
        final PlayScreenView view = helper.buildPlayScreen(
                progress, null, null, "평화로운 숲", null, TalkTarget.EMPTY, logs, null);

        // Then — both NPC and Monster slots null
        assertThat(view.npcName()).isNull();
        assertThat(view.npcDialogue()).isNull();
        assertThat(view.npcActions()).isNull();
        assertThat(view.monsterName()).isNull();
        assertThat(view.monsterDialogue()).isNull();
        assertThat(view.monsterLevel()).isNull();
        assertThat(view.monsterMaxHp()).isNull();
        assertThat(view.monsterActions()).isNull();
    }

    @Test
    void should_preserveMonsterOrder_when_multipleMonsters() {
        // Given
        final Monster raccoon = createNormalMonster("raccoon", "너구리");
        final Monster wolf = createNormalMonster("wolf", "늑대");
        final Monster bear = createNormalMonster("bear", "곰");

        // When
        final List<InteractionItem> result = helper.buildInteractions(
                List.of(), List.of(raccoon, wolf, bear));

        // Then — order preserved
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo("raccoon");
        assertThat(result.get(1).id()).isEqualTo("wolf");
        assertThat(result.get(2).id()).isEqualTo("bear");
    }

    @Test
    void should_returnEmptyList_when_bothListsEmpty() {
        // When
        final List<InteractionItem> result = helper.buildInteractions(List.of(), List.of());

        // Then
        assertThat(result).isEmpty();
    }

    private Monster createNormalMonster(final String id, final String name) {
        return new Monster(id, name, MonsterType.NORMAL, 1, 25, 4, 1, 10,
                15L, new GoldDrop(3, 10), List.of(), List.of("대사1", "대사2", "대사3"));
    }

    private Monster createBossMonster(final String id, final String name) {
        return new Monster(id, name, MonsterType.BOSS, 10, 200, 30, 15, 50,
                500L, new GoldDrop(50, 100), List.of(), List.of("대사1", "대사2", "대사3"));
    }
}
