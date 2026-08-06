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

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 비인접 이동 거부 프로퍼티 테스트.
 *
 * <p>미연결 방향(좌표에 노드가 없거나, 노드가 있어도 링크되지 않은 경우) 요청 시
 * {@code Blocked}를 반환하고 현재 노드 id가 변하지 않는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 9: 비인접 이동 거부
 *
 * <p><b>Validates: Requirements 5.4</b>
 */
// Feature: 001-character-progress-and-map-movement, Property 9: 비인접 이동 거부
class MovementServiceBlockedMovePropertyTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    /**
     * 오프셋 좌표에 노드가 존재하지 않는 경우(Case A) 이동이 거부되고
     * 현재 노드 id가 변하지 않는지 검증한다.
     *
     * @param scenario 맵 그래프, 출발 노드 ID, 빈 좌표를 가리키는 (dx, dy) 오프셋 튜플
     */
    @Property(tries = 100)
    void should_returnBlocked_when_noNodeAtTargetCoordinates(
            @ForAll("noNeighborAtOffsetScenario") final Tuple.Tuple3<MapGraph, String, Tuple.Tuple2<Integer, Integer>> scenario) {

        final MapGraph graph = scenario.get1();
        final String currentNodeId = scenario.get2();
        final Tuple.Tuple2<Integer, Integer> offset = scenario.get3();
        final int dx = offset.get1();
        final int dy = offset.get2();

        final MapNode currentNode = graph.byId(currentNodeId).orElseThrow();

        final MapService mockMapService = mock(MapService.class);
        when(mockMapService.node(currentNodeId)).thenReturn(currentNode);
        when(mockMapService.graph()).thenReturn(graph);

        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);
        final MovementService movementService = new MovementService(mockMapService, actionLog);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100,
                currentNodeId);

        // When
        final MovementResult result = movementService.move(progress, dx, dy);

        // Then: 결과가 Blocked
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);

        // Then: 현재 노드 id 불변
        assertThat(progress.getCurrentNodeId()).isEqualTo(currentNodeId);
    }

    /**
     * 오프셋 좌표에 노드가 존재하지만 현재 노드와 링크되지 않은 경우(Case B)
     * 이동이 거부되고 현재 노드 id가 변하지 않는지 검증한다.
     *
     * @param scenario 맵 그래프, 출발 노드 ID, 링크 없는 이웃 좌표를 가리키는 (dx, dy) 오프셋 튜플
     */
    @Property(tries = 100)
    void should_returnBlocked_when_nodeExistsButNotLinked(
            @ForAll("unlinkedNeighborScenario") final Tuple.Tuple3<MapGraph, String, Tuple.Tuple2<Integer, Integer>> scenario) {

        final MapGraph graph = scenario.get1();
        final String currentNodeId = scenario.get2();
        final Tuple.Tuple2<Integer, Integer> offset = scenario.get3();
        final int dx = offset.get1();
        final int dy = offset.get2();

        final MapNode currentNode = graph.byId(currentNodeId).orElseThrow();

        final MapService mockMapService = mock(MapService.class);
        when(mockMapService.node(currentNodeId)).thenReturn(currentNode);
        when(mockMapService.graph()).thenReturn(graph);

        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);
        final MovementService movementService = new MovementService(mockMapService, actionLog);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100,
                currentNodeId);

        // When
        final MovementResult result = movementService.move(progress, dx, dy);

        // Then: 결과가 Blocked
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);

        // Then: 현재 노드 id 불변
        assertThat(progress.getCurrentNodeId()).isEqualTo(currentNodeId);
    }

    /**
     * Case A: 오프셋 좌표에 노드가 존재하지 않는 시나리오를 생성하는 Arbitrary.
     *
     * <p>최소 1개 노드를 가진 그래프를 생성하고, 어떤 노드도 존재하지 않는
     * 좌표를 가리키는 오프셋을 선택한다.
     *
     * @return (MapGraph, 출발 노드 ID, (dx, dy)) 3-튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<MapGraph, String, Tuple.Tuple2<Integer, Integer>>> noNeighborAtOffsetScenario() {
        return Arbitraries.integers().between(1, 5).flatMap(nodeCount ->
                Arbitraries.integers().between(0, 9).flatMap(baseX ->
                        Arbitraries.integers().between(0, 9).flatMap(baseY ->
                                Arbitraries.integers().between(0, nodeCount - 1).flatMap(sourceIndex ->
                                        Arbitraries.integers().between(2, 10).flatMap(emptyOffsetMagnitude ->
                                                Arbitraries.of(0, 1, 2, 3).map(direction -> {
                                                    final List<MapNode> nodes = generateLinearGraph(
                                                            nodeCount, baseX, baseY);
                                                    final MapGraph graph = new MapGraph(
                                                            nodes, List.of(), nodes.getFirst().id());
                                                    final MapNode sourceNode = nodes.get(sourceIndex);

                                                    final int dx = computeEmptyOffsetDx(
                                                            direction, emptyOffsetMagnitude, nodeCount);
                                                    final int dy = computeEmptyOffsetDy(
                                                            direction, emptyOffsetMagnitude, nodeCount);

                                                    return Tuple.of(
                                                            graph,
                                                            sourceNode.id(),
                                                            Tuple.of(dx, dy));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Case B: 오프셋 좌표에 노드가 존재하지만 링크되지 않은 시나리오를 생성하는 Arbitrary.
     *
     * <p>최소 3개 노드를 가진 체인 그래프를 생성하고, 출발 노드에서 좌표상 이웃이지만
     * links에 포함되지 않은 노드를 찾아 오프셋을 계산한다.
     *
     * @return (MapGraph, 출발 노드 ID, (dx, dy)) 3-튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<MapGraph, String, Tuple.Tuple2<Integer, Integer>>> unlinkedNeighborScenario() {
        return Arbitraries.integers().between(3, 6).flatMap(nodeCount ->
                Arbitraries.integers().between(0, 9).flatMap(baseX ->
                        Arbitraries.integers().between(0, 9).map(baseY -> {
                            final List<MapNode> nodes = generateGridGraphWithGaps(
                                    nodeCount, baseX, baseY);
                            final MapGraph graph = new MapGraph(
                                    nodes, List.of(), nodes.getFirst().id());

                            final MapNode sourceNode = nodes.getFirst();
                            final MapNode unlinkedNeighbor = findUnlinkedCoordNeighbor(
                                    sourceNode, nodes);

                            final int dx = unlinkedNeighbor.x() - sourceNode.x();
                            final int dy = unlinkedNeighbor.y() - sourceNode.y();

                            return Tuple.of(
                                    graph,
                                    sourceNode.id(),
                                    Tuple.of(dx, dy));
                        })
                )
        );
    }

    /**
     * 수평 배치 체인 그래프를 생성한다.
     *
     * <p>각 노드는 인덱스 순으로 x 좌표가 1씩 증가하며 양방향 링크로 연결된다.
     *
     * @param nodeCount 노드 수
     * @param baseX     기준 X 좌표
     * @param baseY     기준 Y 좌표
     * @return 생성된 노드 목록
     */
    private List<MapNode> generateLinearGraph(final int nodeCount,
                                              final int baseX,
                                              final int baseY) {
        final List<String> ids = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            ids.add("node-" + i);
        }

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
            nodes.add(new MapNode(
                    ids.get(i),
                    "장소" + i,
                    "field",
                    NodeType.FIELD,
                    baseX + i,
                    baseY,
                    null,
                    null,
                    List.copyOf(linkLists.get(i))));
        }
        return List.copyOf(nodes);
    }

    /**
     * 격자형 그래프에서 첫 번째 노드와 좌표상 인접하지만 링크되지 않은 노드를 포함하는
     * 그래프를 생성한다.
     *
     * <p>노드 0 → 노드 1은 수평 인접+링크, 노드 2는 노드 0과 수직 인접하지만 링크 없음.
     * 나머지 노드는 노드 1과 체인 연결한다.
     *
     * @param nodeCount 노드 수 (최소 3)
     * @param baseX     기준 X 좌표
     * @param baseY     기준 Y 좌표
     * @return 생성된 노드 목록
     */
    private List<MapNode> generateGridGraphWithGaps(final int nodeCount,
                                                    final int baseX,
                                                    final int baseY) {
        final List<String> ids = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            ids.add("grid-" + i);
        }

        // 좌표: node0=(baseX, baseY), node1=(baseX+1, baseY), node2=(baseX, baseY+1)
        // 나머지는 node1에서 수평 확장
        final List<int[]> coords = new ArrayList<>();
        coords.add(new int[]{baseX, baseY});
        coords.add(new int[]{baseX + 1, baseY});
        coords.add(new int[]{baseX, baseY + 1});
        for (int i = 3; i < nodeCount; i++) {
            coords.add(new int[]{baseX + i, baseY});
        }

        // 링크: node0 ↔ node1만 연결, node2는 어떤 노드와도 링크되지 않음
        final List<List<String>> linkLists = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            linkLists.add(new ArrayList<>());
        }
        linkLists.get(0).add(ids.get(1));
        linkLists.get(1).add(ids.get(0));
        // node1과 나머지 체인 연결
        for (int i = 2; i < nodeCount - 1; i++) {
            linkLists.get(i).add(ids.get(i + 1));
            linkLists.get(i + 1).add(ids.get(i));
        }
        // node2는 의도적으로 node0과 링크하지 않음

        final List<MapNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new MapNode(
                    ids.get(i),
                    "격자" + i,
                    "field",
                    NodeType.FIELD,
                    coords.get(i)[0],
                    coords.get(i)[1],
                    null,
                    null,
                    List.copyOf(linkLists.get(i))));
        }
        return List.copyOf(nodes);
    }

    /**
     * Case A에서 빈 좌표를 가리키는 dx 오프셋을 계산한다.
     *
     * <p>수평 배치 그래프에서 노드가 없는 방향으로의 오프셋을 생성한다.
     *
     * @param direction      방향 (0=위, 1=아래, 2=왼쪽 먼곳, 3=오른쪽 먼곳)
     * @param magnitude      이동 크기
     * @param nodeCount      그래프 노드 수
     * @return dx 오프셋
     */
    private int computeEmptyOffsetDx(final int direction,
                                     final int magnitude,
                                     final int nodeCount) {
        return switch (direction) {
            case 0, 1 -> 0;                     // 위/아래는 x 변화 없음
            case 2 -> -(magnitude);             // 왼쪽 먼 곳
            default -> nodeCount + magnitude;   // 오른쪽 먼 곳
        };
    }

    /**
     * Case A에서 빈 좌표를 가리키는 dy 오프셋을 계산한다.
     *
     * @param direction      방향 (0=위, 1=아래, 2=왼쪽 먼곳, 3=오른쪽 먼곳)
     * @param magnitude      이동 크기
     * @param nodeCount      그래프 노드 수
     * @return dy 오프셋
     */
    private int computeEmptyOffsetDy(final int direction,
                                     final int magnitude,
                                     final int nodeCount) {
        return switch (direction) {
            case 0 -> -(magnitude);             // 위로 먼 곳
            case 1 -> magnitude;                // 아래로 먼 곳
            default -> 0;                       // 좌/우는 y 변화 없음
        };
    }

    /**
     * 소스 노드와 좌표상 인접하지만 links에 포함되지 않은 노드를 찾는다.
     *
     * @param source 출발 노드
     * @param nodes  전체 노드 목록
     * @return 링크되지 않은 좌표 이웃 노드
     */
    private MapNode findUnlinkedCoordNeighbor(final MapNode source,
                                              final List<MapNode> nodes) {
        return nodes.stream()
                .filter(n -> !n.id().equals(source.id()))
                .filter(n -> isCoordinateAdjacent(source, n))
                .filter(n -> !source.links().contains(n.id()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 두 노드가 좌표상 인접한지 판단한다 (맨해튼 거리 1).
     *
     * @param a 첫 번째 노드
     * @param b 두 번째 노드
     * @return 좌표상 인접 여부
     */
    private boolean isCoordinateAdjacent(final MapNode a, final MapNode b) {
        final int manhattanDistance = Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
        return manhattanDistance == 1;
    }
}
