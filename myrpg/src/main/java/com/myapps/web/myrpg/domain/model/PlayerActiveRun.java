package com.myapps.web.myrpg.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 진행 중 던전 체크포인트 엔티티.
 *
 * <p>플레이어당 최대 1개(UNIQUE). 사망·명시적 도망·보스 클리어 시 행이 삭제된다.
 */
@Entity
@Table(name = "rpg_player_active_run")
public class PlayerActiveRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    @Column(name = "dungeon_id", nullable = false)
    private Long dungeonId;

    @Column(name = "cleared_stage", nullable = false)
    private int clearedStage;

    @Column(name = "checkpoint_hp", nullable = false)
    private int checkpointHp;

    @Column(name = "checkpoint_mp", nullable = false)
    private int checkpointMp;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerActiveRun() {
    }

    /**
     * 진행 중 던전 체크포인트를 생성한다.
     *
     * @param playerId     소유자 식별자 (UNIQUE)
     * @param dungeonId    진행 중인 던전 ID
     * @param clearedStage 마지막으로 완료한 스테이지 (0=없음)
     * @param checkpointHp 체크포인트 시점 HP
     * @param checkpointMp 체크포인트 시점 MP
     * @param updatedAt    마지막 스테이지 클리어 시각
     */
    public PlayerActiveRun(final Long playerId, final Long dungeonId,
                           final int clearedStage, final int checkpointHp,
                           final int checkpointMp, final LocalDateTime updatedAt) {
        this.playerId = playerId;
        this.dungeonId = dungeonId;
        this.clearedStage = clearedStage;
        this.checkpointHp = checkpointHp;
        this.checkpointMp = checkpointMp;
        this.updatedAt = updatedAt;
    }

    /**
     * 식별자를 반환한다.
     *
     * @return PK
     */
    public Long getId() {
        return id;
    }

    /**
     * 소유자 식별자를 반환한다.
     *
     * @return 플레이어 ID
     */
    public Long getPlayerId() {
        return playerId;
    }

    /**
     * 진행 중인 던전 ID를 반환한다.
     *
     * @return 던전 ID
     */
    public Long getDungeonId() {
        return dungeonId;
    }

    /**
     * 마지막으로 완료한 스테이지를 반환한다.
     *
     * @return 완료 스테이지 (0=없음)
     */
    public int getClearedStage() {
        return clearedStage;
    }

    /**
     * 체크포인트 시점 HP를 반환한다.
     *
     * @return 체크포인트 HP
     */
    public int getCheckpointHp() {
        return checkpointHp;
    }

    /**
     * 체크포인트 시점 MP를 반환한다.
     *
     * @return 체크포인트 MP
     */
    public int getCheckpointMp() {
        return checkpointMp;
    }

    /**
     * 마지막 스테이지 클리어 시각을 반환한다.
     *
     * @return 갱신 시각
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 완료 스테이지를 변경한다.
     *
     * @param clearedStage 새 완료 스테이지
     */
    public void changeClearedStage(final int clearedStage) {
        this.clearedStage = clearedStage;
    }

    /**
     * 체크포인트 HP를 변경한다.
     *
     * @param checkpointHp 새 체크포인트 HP
     */
    public void changeCheckpointHp(final int checkpointHp) {
        this.checkpointHp = checkpointHp;
    }

    /**
     * 체크포인트 MP를 변경한다.
     *
     * @param checkpointMp 새 체크포인트 MP
     */
    public void changeCheckpointMp(final int checkpointMp) {
        this.checkpointMp = checkpointMp;
    }

    /**
     * 갱신 시각을 변경한다.
     *
     * @param updatedAt 새 갱신 시각
     */
    public void changeUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
