package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import org.mockito.ArgumentCaptor;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 턴 종료 저장 반영 프로퍼티 테스트.
 *
 * <p>성공적 인접 이동(턴 종료) 시 {@code CharacterService.saveTurn()}을 통해
 * {@code CharacterProgressRepository}에 저장되는 진행상황이 변경된 현재 노드 id를 담는지 검증한다.
 * {@code Mockito.mock()} 리포지토리로 저장 인자를 캡처하여 확인한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 11: 턴 종료 저장 반영
 *
 * <p><b>Validates: Requirements 3.3, 5.2</b>
 */
// Feature: 001-character-progress-and-map-movement, Property 11: 턴 종료 저장 반영
class CharacterServiceTurnSavePropertyTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    /**
     * 성공적 인접 이동 후 턴 종료 저장 시, 리포지토리에 전달되는
     * {@code CharacterProgress}의 {@code currentNodeId}가 이동 대상 노드 id와 일치하는지 검증한다.
     *
     * @param scenario 임의 생성된 맵 그래프, 출발 노드 ID, 이동 오프셋(dx, dy), 대상 노드 튜플
     */
    @Property(tries = 100)
    void should_saveProgressWithTargetNodeId_when_adjacentMoveSucceeds(
            @ForAll("connectedNeighborScenario")
            final Tuple.Tuple4<MapGraph, String, Tuple.Tuple2<Integer, Integer>, MapNode> scenario) {

        final MapGraph graph = scenario.get1();
        final String currentNodeId = scenario.get2();
        final Tuple.Tuple2<Integer, Integer> offset = scenario.get3();
        final MapNode targetNode = scenario.get4();
        final int dx = offset.get1();
        final int dy = offset.get2();

        final MapNode currentNode = graph.byId(currentNodeId).orElseThrow();

        // Mock MapService for MovementService
        final MapService mockMapService = mock(MapService.class);
        when(mockMapService.node(currentNodeId)).thenReturn(currentNode);
        when(mockMapService.graph()).thenReturn(graph);

        // Mock CharacterProgressRepository for CharacterService
        final CharacterProgressRepository mockRepository = mock(CharacterProgressRepository.class);
        when(mockRepository.save(org.mockito.ArgumentMatchers.any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);
        final MovementService movementService = new MovementService(mockMapService, actionLog);
        final SkillService mockSkillService = mock(SkillService.class);
        final CharacterService characterService = new CharacterService(mockRepository, mockSkillService);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100,
                currentNodeId, 0);

        // When: 인접 이동 실행
        final MovementResult result = movementService.move(progress, dx, dy);

        // 이동 성공 확인 (전제 조건)
        assertThat(result).isInstanceOf(MovementResult.Moved.class);

        // When: 턴 종료 저장 (컨트롤러가 수행하는 흐름 시뮬레이션)
        characterService.saveTurn(progress);

        // Then: 리포지토리 save 호출 인자 캡처
        final ArgumentCaptor<CharacterProgress> captor = ArgumentCaptor.forClass(CharacterProgress.class);
        verify(mockRepository).save(captor.capture());

        // Then: 저장된 진행상황의 currentNodeId가 대상 노드 id와 일치
        final CharacterProgress savedProgress = captor.getValue();
        assertThat(savedProgress.getCurrentNodeId()).isEqualTo(targetNode.id());
    }

    /**
     * 유효한 맵 그래프에서 인접·연결된 이웃 노드 쌍을 생성하는 Arbitrary.
     *
     * <p>생성된 시나리오는 (MapGraph, 출발 노드 ID, (dx, dy) 오프셋, 대상 MapNode) 4-튜플이다.
     * 각 그래프는 최소 2개 노드로 구성되며, TOWN·FIELD·DUNGEON 타입이 골고루 분배된다.
     *
     * @return 인접 이동 시나리오 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple4<MapGraph, String, Tuple.Tuple2<Integer, Integer>, MapNode>> connectedNeighborScenario() {
        return Arbitraries.integers().between(2, 6).flatMap(nodeCount ->
                Arbitraries.integers().between(-10, 10).flatMap(baseX ->
                        Arbitraries.integers().between(-10, 10).flatMap(baseY ->
                                Arbitraries.integers().between(0, 3).flatMap(layoutType ->
                                        Arbitraries.integers().between(0, nodeCount - 2).map(sourceIndex -> {
                                            final List<MapNode> nodes = generateConnectedGraph(
                                                    nodeCount, baseX, baseY, layoutType);
                                            final MapGraph graph = new MapGraph(
                                                    nodes, List.of(), nodes.getFirst().id());

                                            final MapNode sourceNode = nodes.get(sourceIndex);
                                            final MapNode targetNode = findLinkedNeighbor(sourceNode, nodes);

                                            final int dx = targetNode.x() - sourceNode.x();
                                            final int dy = targetNode.y() - sourceNode.y();

                                            return Tuple.of(
                                                    graph,
                                                    sourceNode.id(),
                                                    Tuple.of(dx, dy),
                                                    targetNode);
                                        })
                                )
                        )
                )
        );
    }

    /**
     * 연결된 그래프를 생성한다.
     *
     * <p>각 노드는 다음 노드와 양방향으로 연결되며, 다양한 NodeType을 포함한다.
     * layoutType에 따라 노드 배치 패턴이 달라진다(수평, 수직, 대각선, 혼합).
     *
     * @param nodeCount  노드 수
     * @param baseX      기준 X 좌표
     * @param baseY      기준 Y 좌표
     * @param layoutType 배치 유형 (0=수평, 1=수직, 2=대각선, 3=혼합)
     * @return 생성된 노드 목록
     */
    private List<MapNode> generateConnectedGraph(final int nodeCount,
                                                  final int baseX,
                                                  final int baseY,
                                                  final int layoutType) {
        final NodeType[] types = {NodeType.TOWN, NodeType.FIELD, NodeType.DUNGEON, NodeType.FIELD};
        final String[] typeStrings = {"town", "field", "dungeon", "field"};
        final String[] nameKorean = {"마을", "들판", "던전입구", "평원"};

        final List<String> ids = new ArrayList<>();
        final List<int[]> coords = new ArrayList<>();

        for (int i = 0; i < nodeCount; i++) {
            ids.add("node-" + i);
            coords.add(computeCoordinate(i, baseX, baseY, layoutType));
        }

        // 체인 형태로 인접 노드끼리 양방향 연결
        final List<List<String>> linkLists = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            linkLists.add(new ArrayList<>());
        }

        for (int i = 0; i < nodeCount - 1; i++) {
            linkLists.get(i).add(ids.get(i + 1));
            linkLists.get(i + 1).add(ids.get(i));
        }

        final List<MapNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            final int typeIndex = i % types.length;
            final String dungeonId = (types[typeIndex] == NodeType.DUNGEON) ? "dungeon-" + i : null;
            nodes.add(new MapNode(
                    ids.get(i),
                    nameKorean[typeIndex] + i,
                    typeStrings[typeIndex],
                    types[typeIndex],
                    coords.get(i)[0],
                    coords.get(i)[1],
                    dungeonId,
                    null,
                    List.copyOf(linkLists.get(i))
            ));
        }

        return List.copyOf(nodes);
    }

    /**
     * 레이아웃 유형에 따라 노드 좌표를 계산한다.
     *
     * @param index      노드 인덱스
     * @param baseX      기준 X 좌표
     * @param baseY      기준 Y 좌표
     * @param layoutType 배치 유형
     * @return [x, y] 좌표 배열
     */
    private int[] computeCoordinate(final int index,
                                     final int baseX,
                                     final int baseY,
                                     final int layoutType) {
        return switch (layoutType) {
            case 0 -> new int[]{baseX + index, baseY};          // 수평
            case 1 -> new int[]{baseX, baseY + index};          // 수직
            case 2 -> new int[]{baseX + index, baseY + index};  // 대각선
            default -> new int[]{baseX + (index % 2 == 0 ? index / 2 : index / 2 + 1),
                    baseY + (index % 2 == 0 ? 0 : 1)};         // 지그재그
        };
    }

    /**
     * 소스 노드의 links 목록에서 좌표상 인접한 노드를 찾아 반환한다.
     *
     * @param source 출발 노드
     * @param nodes  전체 노드 목록
     * @return 링크되어 있는 이웃 노드 (첫 번째 발견)
     */
    private MapNode findLinkedNeighbor(final MapNode source, final List<MapNode> nodes) {
        final String firstLinkedId = source.links().getFirst();
        return nodes.stream()
                .filter(n -> n.id().equals(firstLinkedId))
                .findFirst()
                .orElseThrow();
    }
}
