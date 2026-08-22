package com.myapps.web.myrpg.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 활성화된 인스턴스 던전의 런타임 도메인 집계(Aggregate).
 *
 * <p>플레이어별 던전 진행 상황(방 그래프, 현재 위치, 각 방의 클리어/안개 발견 상태 및 잔여 몬스터 목록)을 관리합니다.
 */
public final class DungeonInstance {

    private final Long characterId;
    private final String dungeonId;
    private final String entranceNodeId;
    private final String startRoomId;
    private final String bossRoomId;
    private String currentRoomId;
    private final MapGraph dungeonGraph;
    private final Map<String, DungeonRoomState> roomStates;

    /**
     * 던전 인스턴스를 생성한다.
     *
     * @param characterId 캐릭터 고유 ID
     * @param dungeonId 던전 메타데이터 ID (예: "alby")
     * @param entranceNodeId 필드의 던전 입구 노드 ID (예: "alby-entrance")
     * @param startRoomId 던전 시작방 ID (예: "room-0-0")
     * @param bossRoomId 보스방 ID
     * @param currentRoomId 현재 플레이어가 위치한 방 ID
     * @param dungeonGraph 던전 맵 그래프
     * @param roomStates 각 방별 동적 상태 맵
     */
    public DungeonInstance(
            final Long characterId,
            final String dungeonId,
            final String entranceNodeId,
            final String startRoomId,
            final String bossRoomId,
            final String currentRoomId,
            final MapGraph dungeonGraph,
            final Map<String, DungeonRoomState> roomStates) {
        this.characterId = characterId;
        this.dungeonId = dungeonId;
        this.entranceNodeId = entranceNodeId;
        this.startRoomId = startRoomId;
        this.bossRoomId = bossRoomId;
        this.currentRoomId = currentRoomId;
        this.dungeonGraph = dungeonGraph;
        this.roomStates = new LinkedHashMap<>(roomStates != null ? roomStates : Map.of());
    }

    /**
     * 캐릭터 고유 ID를 반환한다.
     *
     * @return 캐릭터 ID
     */
    public Long characterId() {
        return characterId;
    }

    /**
     * 던전 메타데이터 ID를 반환한다.
     *
     * @return 던전 ID
     */
    public String dungeonId() {
        return dungeonId;
    }

    /**
     * 필드의 던전 입구 노드 ID를 반환한다.
     *
     * @return 입구 노드 ID
     */
    public String entranceNodeId() {
        return entranceNodeId;
    }

    /**
     * 던전 시작방 ID를 반환한다.
     *
     * @return 시작방 ID
     */
    public String startRoomId() {
        return startRoomId;
    }

    /**
     * 보스방 ID를 반환한다.
     *
     * @return 보스방 ID
     */
    public String bossRoomId() {
        return bossRoomId;
    }

    /**
     * 현재 플레이어가 위치한 방 ID를 반환한다.
     *
     * @return 현재 방 ID
     */
    public String currentRoomId() {
        return currentRoomId;
    }

    /**
     * 던전 맵 그래프를 반환한다.
     *
     * @return 맵 그래프
     */
    public MapGraph dungeonGraph() {
        return dungeonGraph;
    }

    /**
     * 모든 방의 상태 맵을 불변 뷰로 반환한다.
     *
     * @return 방 ID → {@link DungeonRoomState} 불변 맵
     */
    public Map<String, DungeonRoomState> roomStates() {
        return Collections.unmodifiableMap(roomStates);
    }

    /**
     * 특정 방의 상태를 조회한다.
     *
     * @param roomId 방 ID
     * @return 방 상태 객체, 미존재 시 {@code null}
     */
    public DungeonRoomState getRoomState(final String roomId) {
        return roomStates.get(roomId);
    }

    /**
     * 특정 방이 클리어 상태인지 확인한다.
     *
     * @param roomId 방 ID
     * @return 클리어되었으면 {@code true}, 미존재하거나 미클리어면 {@code false}
     */
    public boolean isRoomCleared(final String roomId) {
        final DungeonRoomState state = roomStates.get(roomId);
        return state != null && state.cleared();
    }

    /**
     * 특정 방이 안개에서 발견된 상태인지 확인한다.
     *
     * @param roomId 방 ID
     * @return 발견되었으면 {@code true}, 미존재하거나 미발견이면 {@code false}
     */
    public boolean isRoomDiscovered(final String roomId) {
        final DungeonRoomState state = roomStates.get(roomId);
        return state != null && state.discovered();
    }

    /**
     * 특정 방이 보스방과 바로 인접(통로로 연결)해 있는지 확인한다.
     *
     * @param roomId 검사할 방 ID
     * @return 보스방과 인접하면 {@code true}
     */
    public boolean isAdjacentToBoss(final String roomId) {
        if (dungeonGraph == null || bossRoomId == null) {
            return false;
        }
        final MapNode node = dungeonGraph.byId(roomId).orElse(null);
        return node != null && node.links().contains(bossRoomId);
    }

    /**
     * 플레이어를 대상 방으로 이동시키고, 대상 방 및 그 이웃 방들의 안개를 해제(발견)한다.
     *
     * @param targetRoomId 이동할 대상 방 ID
     */
    public void moveTo(final String targetRoomId) {
        this.currentRoomId = targetRoomId;
        revealAdjacent(targetRoomId);
    }

    /**
     * 지정된 방과 그 방에 연결된 모든 이웃 방을 발견 상태로 전환한다.
     *
     * @param roomId 기준 방 ID
     */
    public void revealAdjacent(final String roomId) {
        markDiscovered(roomId);
        if (dungeonGraph != null) {
            dungeonGraph
                    .byId(roomId)
                    .ifPresent(
                            node -> {
                                for (final String neighborId : node.links()) {
                                    markDiscovered(neighborId);
                                }
                            });
        }
    }

    /**
     * 지정된 방을 발견 상태로 표시한다.
     *
     * @param roomId 방 ID
     */
    public void markDiscovered(final String roomId) {
        final DungeonRoomState current = roomStates.get(roomId);
        if (current != null && !current.discovered()) {
            roomStates.put(roomId, current.withDiscovered(true));
        }
    }

    /**
     * 지정된 방을 클리어 상태로 전환하고 남은 몬스터 목록을 비운다.
     *
     * @param roomId 방 ID
     */
    public void markCleared(final String roomId) {
        final DungeonRoomState current = roomStates.get(roomId);
        if (current != null) {
            roomStates.put(roomId, current.withCleared(true).withRemainingMonsters(List.of()));
        }
    }

    /**
     * 지정된 방에서 쓰러진 몬스터를 제거한다.
     *
     * <p>방 안의 모든 몬스터가 제거되면 자동으로 해당 방을 클리어 상태({@code cleared = true})로 전환합니다.
     *
     * @param roomId 방 ID
     * @param monsterId 격파된 몬스터 식별자
     */
    public void removeMonster(final String roomId, final String monsterId) {
        final DungeonRoomState current = roomStates.get(roomId);
        if (current != null) {
            final List<String> updated = new ArrayList<>(current.remainingMonsters());
            updated.remove(monsterId);
            final boolean cleared = updated.isEmpty();
            roomStates.put(
                    roomId, new DungeonRoomState(roomId, cleared, true, List.copyOf(updated)));
        }
    }

    /**
     * 현재 위치한 방의 맵 노드를 반환한다.
     *
     * @return 현재 노드 Optional
     */
    public Optional<MapNode> currentRoomNode() {
        if (dungeonGraph == null || currentRoomId == null) {
            return Optional.empty();
        }
        return dungeonGraph.byId(currentRoomId);
    }
}
