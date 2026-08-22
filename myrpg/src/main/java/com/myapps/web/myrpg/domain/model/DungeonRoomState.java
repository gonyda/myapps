package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * 던전 내 개별 방의 동적 상태를 나타내는 불변 레코드.
 *
 * <p>방의 클리어 여부, 안개(Fog of War) 발견 여부, 현재 방에 남아있는 몬스터 목록을 관리합니다.
 *
 * @param roomId 방 고유 식별자 (예: "room-0-0")
 * @param cleared 방 안의 모든 몬스터가 소탕되어 클리어되었는지 여부
 * @param discovered 플레이어가 방문했거나 인접하여 안개가 걷힌 방인지 여부
 * @param remainingMonsters 현재 방에 남아있는 몬스터 ID 목록 (불변 리스트)
 */
public record DungeonRoomState(
        String roomId, boolean cleared, boolean discovered, List<String> remainingMonsters) {

    /**
     * 컴팩트 생성자. {@code remainingMonsters}가 {@code null}인 경우 빈 불변 리스트로 대체하고, 방어적 복사를 수행하여 불변성을 보장합니다.
     */
    public DungeonRoomState {
        remainingMonsters = remainingMonsters == null ? List.of() : List.copyOf(remainingMonsters);
    }

    /**
     * 발견(안개 노출) 상태가 변경된 새 {@code DungeonRoomState} 인스턴스를 반환합니다.
     *
     * @param discovered 새 발견 상태
     * @return 갱신된 불변 레코드
     */
    public DungeonRoomState withDiscovered(final boolean discovered) {
        return new DungeonRoomState(this.roomId, this.cleared, discovered, this.remainingMonsters);
    }

    /**
     * 클리어 상태가 변경된 새 {@code DungeonRoomState} 인스턴스를 반환합니다.
     *
     * @param cleared 새 클리어 상태
     * @return 갱신된 불변 레코드
     */
    public DungeonRoomState withCleared(final boolean cleared) {
        return new DungeonRoomState(this.roomId, cleared, this.discovered, this.remainingMonsters);
    }

    /**
     * 남은 몬스터 목록이 변경된 새 {@code DungeonRoomState} 인스턴스를 반환합니다.
     *
     * @param monsters 새 남은 몬스터 ID 목록
     * @return 갱신된 불변 레코드
     */
    public DungeonRoomState withRemainingMonsters(final List<String> monsters) {
        return new DungeonRoomState(this.roomId, this.cleared, this.discovered, monsters);
    }
}
