package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 무기에 장착된 스킬 엔티티.
 *
 * <p>무기 인스턴스의 특정 슬롯에 귀속된 스킬 정보를 저장한다.
 */
@Entity
@Table(name = "rpg_player_weapon_skill")
public class PlayerWeaponSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_weapon_id", nullable = false)
    private Long playerWeaponId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerWeaponSkill() {
    }

    /**
     * 무기 스킬 장착 정보를 생성한다.
     *
     * @param playerWeaponId 무기 인스턴스 식별자
     * @param skillId        JSON 스킬 ID
     * @param slotIndex      슬롯 위치 (0부터 시작)
     */
    public PlayerWeaponSkill(final Long playerWeaponId, final Long skillId,
                             final int slotIndex) {
        this.playerWeaponId = playerWeaponId;
        this.skillId = skillId;
        this.slotIndex = slotIndex;
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
     * 무기 인스턴스 식별자를 반환한다.
     *
     * @return 무기 인스턴스 ID
     */
    public Long getPlayerWeaponId() {
        return playerWeaponId;
    }

    /**
     * 스킬 ID를 반환한다.
     *
     * @return JSON 스킬 ID
     */
    public Long getSkillId() {
        return skillId;
    }

    /**
     * 슬롯 위치를 반환한다.
     *
     * @return 슬롯 인덱스 (0부터 시작)
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * 스킬 ID를 변경한다 (덮어쓰기 시 사용).
     *
     * @param skillId 새 스킬 ID
     */
    public void changeSkillId(final Long skillId) {
        this.skillId = skillId;
    }
}
