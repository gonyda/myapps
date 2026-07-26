package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 던전 클리어 이력 엔티티.
 *
 * <p>플레이어별 각 던전의 클리어 여부와 최고 도달 스테이지를 기록한다.
 */
@Entity
@Table(name = "rpg_player_dungeon_progress")
public class PlayerDungeonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "dungeon_id", nullable = false)
    private Long dungeonId;

    @Column(name = "is_cleared", nullable = false)
    private boolean cleared;

    @Column(name = "best_stage", nullable = false)
    private int bestStage;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerDungeonProgress() {
    }

    /**
     * 던전 진행 이력을 생성한다.
     *
     * @param playerId  소유자 식별자
     * @param dungeonId JSON 던전 ID
     * @param cleared   클리어 여부
     * @param bestStage 최고 도달 스테이지 (1~5)
     */
    public PlayerDungeonProgress(final Long playerId, final Long dungeonId,
                                 final boolean cleared, final int bestStage) {
        this.playerId = playerId;
        this.dungeonId = dungeonId;
        this.cleared = cleared;
        this.bestStage = bestStage;
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
     * 던전 ID를 반환한다.
     *
     * @return JSON 던전 ID
     */
    public Long getDungeonId() {
        return dungeonId;
    }

    /**
     * 클리어 여부를 반환한다.
     *
     * @return 클리어했으면 true
     */
    public boolean isCleared() {
        return cleared;
    }

    /**
     * 최고 도달 스테이지를 반환한다.
     *
     * @return 최고 도달 스테이지 (1~5)
     */
    public int getBestStage() {
        return bestStage;
    }

    /**
     * 클리어 여부를 변경한다.
     *
     * @param cleared 새 클리어 상태
     */
    public void changeCleared(final boolean cleared) {
        this.cleared = cleared;
    }

    /**
     * 최고 도달 스테이지를 변경한다.
     *
     * @param bestStage 새 최고 스테이지
     */
    public void changeBestStage(final int bestStage) {
        this.bestStage = bestStage;
    }
}
