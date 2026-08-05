package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.exception.MapViewGenerationException;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;

/**
 * 미니맵 및 전체지도 격자 데이터를 생성하는 순수 로직 팩토리.
 *
 * <p>외부 의존성 없이 {@link MapGraph}와 현재 노드 정보만으로
 * 뷰 렌더링에 필요한 격자 셀과 간선 정보를 산출한다.
 * 현재 노드 id가 그래프에 존재하지 않으면 {@link MapViewGenerationException}을 던진다.
 */
public class MapViewFactory {

    /** 미니맵 중심 노드의 grid-column 값. */
    private static final int MINIMAP_CENTER_COLUMN = 5;

    /** 미니맵 중심 노드의 grid-row 값. */
    private static final int MINIMAP_CENTER_ROW = 3;

    /** 미니맵 X 오프셋 최소값 (왼쪽 방향). */
    private static final int MINIMAP_DX_MIN = -4;

    /** 미니맵 X 오프셋 최대값 (오른쪽 방향). */
    private static final int MINIMAP_DX_MAX = 4;

    /** 미니맵 Y 오프셋 최소값 (위쪽 방향). */
    private static final int MINIMAP_DY_MIN = -2;

    /** 미니맵 Y 오프셋 최대값 (아래쪽 방향). */
    private static final int MINIMAP_DY_MAX = 2;

    /**
     * 미니맵 뷰를 생성한다.
     *
     * <p>현재 노드를 중심(grid-column=5, grid-row=3)으로
     * dx∈[-4,4], dy∈[-2,2] 범위의 노드를 격자에 배치한다.
     * 간선은 두 노드가 모두 창 안에 있고 links로 실제 연결된 경우에만
     * linkRight/linkDown 플래그로 표현한다.
     *
     * @param graph         맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @return 미니맵 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public MinimapView createMinimap(final MapGraph graph, final String currentNodeId) {
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final List<MinimapCell> cells = buildMinimapCells(graph, currentNode);
        return new MinimapView(currentNode.name(), cells);
    }

    /**
     * 전체지도 뷰를 생성한다.
     *
     * <p>모든 노드를 바운딩박스 기준 gridColumn=x-minX+1, gridRow=y-minY+1에 배치한다.
     * 간선은 좌표상 오른쪽/아래 이웃이면서 links로 실제 연결된 경우에만
     * linkRight/linkDown 플래그로 표현한다.
     *
     * @param graph         맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @return 전체지도 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public FullMapView createFullMap(final MapGraph graph, final String currentNodeId) {
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final List<MapNode> allNodes = graph.nodes();

        final int[] bounds = computeBoundingBox(allNodes);
        final int minX = bounds[0];
        final int minY = bounds[1];
        final int maxX = bounds[2];
        final int maxY = bounds[3];

        final List<FullMapCell> cells = buildFullMapCells(graph, allNodes, currentNode, minX, minY);
        final int columns = maxX - minX + 1;
        final int rows = maxY - minY + 1;

        return new FullMapView(cells, columns, rows);
    }

    private MapNode resolveCurrentNode(final MapGraph graph, final String currentNodeId) {
        final Optional<MapNode> nodeOpt = graph.byId(currentNodeId);
        if (nodeOpt.isEmpty()) {
            throw new MapViewGenerationException(
                    "현재 노드를 확인할 수 없습니다: " + currentNodeId);
        }
        return nodeOpt.get();
    }

    private List<MinimapCell> buildMinimapCells(final MapGraph graph, final MapNode currentNode) {
        final List<MinimapCell> cells = new ArrayList<>();

        for (int dx = MINIMAP_DX_MIN; dx <= MINIMAP_DX_MAX; dx++) {
            for (int dy = MINIMAP_DY_MIN; dy <= MINIMAP_DY_MAX; dy++) {
                final Optional<MapNode> neighborOpt = graph.neighborByOffset(currentNode, dx, dy);
                if (neighborOpt.isPresent()) {
                    final MapNode node = neighborOpt.get();
                    final int gridColumn = MINIMAP_CENTER_COLUMN + dx;
                    final int gridRow = MINIMAP_CENTER_ROW + dy;
                    final boolean isCurrent = node.id().equals(currentNode.id());
                    final boolean linkRight = hasMinimapLink(graph, currentNode, node, 1, 0);
                    final boolean linkDown = hasMinimapLink(graph, currentNode, node, 0, 1);

                    cells.add(new MinimapCell(
                            node.id(), gridColumn, gridRow,
                            node.type(), isCurrent, linkRight, linkDown));
                }
            }
        }
        return List.copyOf(cells);
    }

    private boolean hasMinimapLink(final MapGraph graph, final MapNode center,
                                   final MapNode node, final int edgeDx, final int edgeDy) {
        final int neighborX = node.x() + edgeDx;
        final int neighborY = node.y() + edgeDy;
        final int offsetFromCenter = neighborX - center.x();
        final int offsetYFromCenter = neighborY - center.y();

        if (offsetFromCenter < MINIMAP_DX_MIN || offsetFromCenter > MINIMAP_DX_MAX
                || offsetYFromCenter < MINIMAP_DY_MIN || offsetYFromCenter > MINIMAP_DY_MAX) {
            return false;
        }

        final Optional<MapNode> neighborOpt = graph.byCoord(MapGraph.coordKey(neighborX, neighborY));
        if (neighborOpt.isEmpty()) {
            return false;
        }

        return node.links().contains(neighborOpt.get().id());
    }

    private int[] computeBoundingBox(final List<MapNode> nodes) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final MapNode node : nodes) {
            minX = Math.min(minX, node.x());
            minY = Math.min(minY, node.y());
            maxX = Math.max(maxX, node.x());
            maxY = Math.max(maxY, node.y());
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    private List<FullMapCell> buildFullMapCells(final MapGraph graph,
                                                final List<MapNode> allNodes,
                                                final MapNode currentNode,
                                                final int minX,
                                                final int minY) {
        final List<FullMapCell> cells = new ArrayList<>();

        for (final MapNode node : allNodes) {
            final int gridColumn = node.x() - minX + 1;
            final int gridRow = node.y() - minY + 1;
            final boolean isCurrent = node.id().equals(currentNode.id());
            final boolean linkRight = hasFullMapLink(graph, node, 1, 0);
            final boolean linkDown = hasFullMapLink(graph, node, 0, 1);

            cells.add(new FullMapCell(
                    node.id(), node.name(), gridColumn, gridRow,
                    node.type(), isCurrent, linkRight, linkDown,
                    node.links()));
        }
        return List.copyOf(cells);
    }

    private boolean hasFullMapLink(final MapGraph graph, final MapNode node,
                                   final int edgeDx, final int edgeDy) {
        final Optional<MapNode> neighborOpt = graph.neighborByOffset(node, edgeDx, edgeDy);
        if (neighborOpt.isEmpty()) {
            return false;
        }
        return node.links().contains(neighborOpt.get().id());
    }
}
