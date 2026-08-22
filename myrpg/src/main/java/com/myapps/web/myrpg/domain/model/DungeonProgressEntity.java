package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 인스턴스 던전의 진행 상태를 영속 저장하는 JPA 엔티티.
 *
 * <p>캐릭터당 최대 1개의 활성 던전이 유지되며, 던전 맵 구조 및 방 상태는 JSON 문자열로 직렬화되어 보관됩니다. 브라우저 재접속 시에도 세션 손실 없이 던전을 복원할
 * 수 있습니다.
 */
@Entity
@Table(name = "dungeon_progress")
public class DungeonProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false, unique = true)
    private Long characterId;

    @Column(name = "dungeon_id", nullable = false)
    private String dungeonId;

    @Column(name = "entrance_node_id", nullable = false)
    private String entranceNodeId;

    @Column(name = "start_room_id", nullable = false)
    private String startRoomId;

    @Column(name = "boss_room_id", nullable = false)
    private String bossRoomId;

    @Column(name = "current_room_id", nullable = false)
    private String currentRoomId;

    @Lob
    @Column(name = "dungeon_graph_json", nullable = false)
    private String dungeonGraphJson;

    @Lob
    @Column(name = "room_states_json", nullable = false)
    private String roomStatesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA 전용 기본 생성자. */
    protected DungeonProgressEntity() {}

    /**
     * 시간 필드를 포함한 전체 필드 생성자.
     *
     * @param characterId 캐릭터 고유 ID
     * @param dungeonId 던전 메타데이터 ID
     * @param entranceNodeId 필드의 던전 입구 노드 ID
     * @param startRoomId 던전 시작방 ID
     * @param bossRoomId 보스방 ID
     * @param currentRoomId 현재 방 ID
     * @param dungeonGraphJson 던전 맵 그래프 직렬화 JSON
     * @param roomStatesJson 각 방 상태 맵 직렬화 JSON
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     */
    public DungeonProgressEntity(
            final Long characterId,
            final String dungeonId,
            final String entranceNodeId,
            final String startRoomId,
            final String bossRoomId,
            final String currentRoomId,
            final String dungeonGraphJson,
            final String roomStatesJson,
            final LocalDateTime createdAt,
            final LocalDateTime updatedAt) {
        this.characterId = characterId;
        this.dungeonId = dungeonId;
        this.entranceNodeId = entranceNodeId;
        this.startRoomId = startRoomId;
        this.bossRoomId = bossRoomId;
        this.currentRoomId = currentRoomId;
        this.dungeonGraphJson = dungeonGraphJson;
        this.roomStatesJson = roomStatesJson;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * 신규 던전 진행상황 생성을 위한 보조 생성자.
     *
     * @param characterId 캐릭터 고유 ID
     * @param dungeonId 던전 메타데이터 ID
     * @param entranceNodeId 필드의 던전 입구 노드 ID
     * @param startRoomId 던전 시작방 ID
     * @param bossRoomId 보스방 ID
     * @param currentRoomId 현재 방 ID
     * @param dungeonGraphJson 던전 맵 그래프 직렬화 JSON
     * @param roomStatesJson 각 방 상태 맵 직렬화 JSON
     */
    public DungeonProgressEntity(
            final Long characterId,
            final String dungeonId,
            final String entranceNodeId,
            final String startRoomId,
            final String bossRoomId,
            final String currentRoomId,
            final String dungeonGraphJson,
            final String roomStatesJson) {
        this(
                characterId,
                dungeonId,
                entranceNodeId,
                startRoomId,
                bossRoomId,
                currentRoomId,
                dungeonGraphJson,
                roomStatesJson,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        final LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 기본 키 ID를 반환한다.
     *
     * @return 엔티티 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 캐릭터 고유 ID를 반환한다.
     *
     * @return 캐릭터 ID
     */
    public Long getCharacterId() {
        return characterId;
    }

    /**
     * 던전 메타데이터 ID를 반환한다.
     *
     * @return 던전 ID
     */
    public String getDungeonId() {
        return dungeonId;
    }

    /**
     * 던전 입구 노드 ID를 반환한다.
     *
     * @return 입구 노드 ID
     */
    public String getEntranceNodeId() {
        return entranceNodeId;
    }

    /**
     * 던전 시작방 ID를 반환한다.
     *
     * @return 시작방 ID
     */
    public String getStartRoomId() {
        return startRoomId;
    }

    /**
     * 보스방 ID를 반환한다.
     *
     * @return 보스방 ID
     */
    public String getBossRoomId() {
        return bossRoomId;
    }

    /**
     * 현재 방 ID를 반환한다.
     *
     * @return 현재 방 ID
     */
    public String getCurrentRoomId() {
        return currentRoomId;
    }

    /**
     * 현재 방 ID를 갱신한다.
     *
     * @param currentRoomId 새 현재 방 ID
     */
    public void setCurrentRoomId(final String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    /**
     * 던전 맵 그래프 JSON 문자열을 반환한다.
     *
     * @return 직렬화된 맵 그래프 JSON
     */
    public String getDungeonGraphJson() {
        return dungeonGraphJson;
    }

    /**
     * 던전 맵 그래프 JSON 문자열을 갱신한다.
     *
     * @param dungeonGraphJson 새 맵 그래프 JSON
     */
    public void setDungeonGraphJson(final String dungeonGraphJson) {
        this.dungeonGraphJson = dungeonGraphJson;
    }

    /**
     * 각 방 상태 JSON 문자열을 반환한다.
     *
     * @return 직렬화된 방 상태 JSON
     */
    public String getRoomStatesJson() {
        return roomStatesJson;
    }

    /**
     * 각 방 상태 JSON 문자열을 갱신한다.
     *
     * @param roomStatesJson 새 방 상태 JSON
     */
    public void setRoomStatesJson(final String roomStatesJson) {
        this.roomStatesJson = roomStatesJson;
    }

    /**
     * 생성 일시를 반환한다.
     *
     * @return 생성 일시
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 수정 일시를 반환한다.
     *
     * @return 수정 일시
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 수정 일시를 갱신한다.
     *
     * @param updatedAt 새 수정 일시
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
