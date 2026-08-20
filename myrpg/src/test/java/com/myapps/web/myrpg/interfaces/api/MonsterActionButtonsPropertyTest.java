package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.ActionButton;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.Mockito;

/**
 * 몬스터 행동 버튼 조립 프로퍼티 테스트.
 *
 * <p>임의 Monster에 대해 {@link PlayScreenViewHelper#buildPlayScreen}이 TalkTarget.ofMonster를 통해 생성하는
 * {@code monsterActions} 라벨 목록이 {@code MonsterType.actionLabels()}와 개수·순서·값이 정확히 일치함을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 10: 몬스터 행동 버튼 조립
 *
 * <p><b>Validates: Requirements 11.5, 13.2</b>
 */
class MonsterActionButtonsPropertyTest {

    private static final int LINES_COUNT = 3;

    private final PlayScreenViewHelper helper;

    MonsterActionButtonsPropertyTest() {
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

    /**
     * 임의 Monster를 TalkTarget.ofMonster로 전달했을 때, 반환되는 {@code monsterActions} 라벨 목록이 해당 몬스터 타입의
     * {@code actionLabels()}와 개수·순서·값이 정확히 동일함을 검증한다.
     *
     * @param monster 임의 생성된 몬스터
     */
    @Property(tries = 100)
    void should_matchMonsterActionLabels_withTypeDefinition(
            @ForAll("monsters") final Monster monster) {
        // Given
        final CharacterProgress progress = mockCharacterProgress();
        final TalkTarget talkTarget = TalkTarget.ofMonster(monster, "테스트 대사");
        final List<String> expectedLabels = monster.type().actionLabels();

        // When
        final PlayScreenView view =
                helper.buildPlayScreen(
                        progress, null, null, null, null, talkTarget, List.of(), null);

        // Then
        final List<ActionButton> monsterActions = view.monsterActions();
        assertThat(monsterActions).isNotNull();
        assertThat(monsterActions).hasSize(expectedLabels.size());

        final List<String> actualLabels = monsterActions.stream().map(ActionButton::label).toList();
        assertThat(actualLabels).isEqualTo(expectedLabels);
    }

    /**
     * 임의 Monster를 생성하는 Arbitrary 제공자.
     *
     * @return Monster Arbitrary
     */
    @Provide
    Arbitrary<Monster> monsters() {
        return monsterArbitrary();
    }

    private CharacterProgress mockCharacterProgress() {
        final CharacterProgress progress = Mockito.mock(CharacterProgress.class);
        when(progress.getNickname()).thenReturn("테스트");
        when(progress.getCurrentLevel()).thenReturn(1);
        when(progress.getExperience()).thenReturn(0L);
        when(progress.getHpCurrent()).thenReturn(100);
        when(progress.getMpCurrent()).thenReturn(100);
        when(progress.getStaminaCurrent()).thenReturn(100);
        when(progress.getTalent()).thenReturn(TalentType.MELEE);
        return progress;
    }

    private Arbitrary<Monster> monsterArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        final Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(8);
        final Arbitrary<MonsterType> types = Arbitraries.of(MonsterType.values());
        final Arbitrary<Integer> levels = Arbitraries.integers().between(1, 50);
        final Arbitrary<Integer> maxHps = Arbitraries.integers().between(1, 500);
        final Arbitrary<Integer> attackPowers = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> defenses = Arbitraries.integers().between(0, 50);
        final Arbitrary<Integer> criticals = Arbitraries.integers().between(0, 200);
        final Arbitrary<Long> experiences = Arbitraries.longs().between(1L, 1000L);
        final Arbitrary<GoldDrop> goldDrops = goldDropArbitrary();
        final Arbitrary<List<String>> lines =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(1)
                        .ofMaxLength(20)
                        .list()
                        .ofSize(LINES_COUNT);

        return Combinators.combine(
                        ids, names, types, levels, maxHps, attackPowers, defenses, criticals)
                .flatAs(
                        (id, name, type, level, maxHp, attackPower, defense, critical) ->
                                Combinators.combine(experiences, goldDrops, lines)
                                        .as(
                                                (experience, goldDrop, lineList) ->
                                                        new Monster(
                                                                id,
                                                                name,
                                                                type,
                                                                level,
                                                                maxHp,
                                                                attackPower,
                                                                defense,
                                                                critical,
                                                                experience,
                                                                goldDrop,
                                                                List.of(),
                                                                lineList)));
    }

    private Arbitrary<GoldDrop> goldDropArbitrary() {
        return Arbitraries.integers()
                .between(0, 50)
                .flatMap(
                        min ->
                                Arbitraries.integers()
                                        .between(min, min + 50)
                                        .map(max -> new GoldDrop(min, max)));
    }
}
