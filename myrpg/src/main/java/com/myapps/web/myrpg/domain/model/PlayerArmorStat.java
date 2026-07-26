package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 방어구 랜덤 능력치 엔티티.
 *
 * <p>방어구 인스턴스에 부여된 개별 랜덤 능력치(StatType + 수치)를 저장한다.
 */
@Entity
@Table(name = "rpg_player_armor_stat")
public class PlayerArmorStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_armor_id", nullable = false)
    private Long playerArmorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false)
    private StatType statType;

    @Column(name = "stat_value", nullable = false)
    private int statValue;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerArmorStat() {
    }

    /**
     * 방어구 랜덤 능력치를 생성한다.
     *
     * @param playerArmorId 방어구 인스턴스 식별자
     * @param statType      능력치 종류
     * @param statValue     능력치 수치
     */
    public PlayerArmorStat(final Long playerArmorId, final StatType statType,
                           final int statValue) {
        this.playerArmorId = playerArmorId;
        this.statType = statType;
        this.statValue = statValue;
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
     * 방어구 인스턴스 식별자를 반환한다.
     *
     * @return 방어구 인스턴스 ID
     */
    public Long getPlayerArmorId() {
        return playerArmorId;
    }

    /**
     * 능력치 종류를 반환한다.
     *
     * @return 능력치 종류
     */
    public StatType getStatType() {
        return statType;
    }

    /**
     * 능력치 수치를 반환한다.
     *
     * @return 능력치 수치
     */
    public int getStatValue() {
        return statValue;
    }
}
