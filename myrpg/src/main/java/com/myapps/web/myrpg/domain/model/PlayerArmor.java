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
 * 플레이어 보유 방어구 인스턴스 엔티티.
 *
 * <p>드랍 시 생성되며 등급·랜덤 능력치가 개별적으로 롤된다.
 */
@Entity
@Table(name = "rpg_player_armor")
public class PlayerArmor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "armor_template_id", nullable = false)
    private Long armorTemplateId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "armor_slot", nullable = false)
    private ArmorSlot armorSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Column(name = "base_defense", nullable = false)
    private int baseDefense;

    @Column(name = "item_level", nullable = false)
    private int itemLevel;

    @Column(name = "is_equipped", nullable = false)
    private boolean equipped;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerArmor() {
    }

    /**
     * 방어구 인스턴스를 생성한다.
     *
     * @param playerId        소유자 식별자
     * @param armorTemplateId JSON 방어구 템플릿 ID
     * @param displayName     표시명 ([등급] 템플릿명)
     * @param armorSlot       방어구 부위
     * @param grade           롤된 등급
     * @param baseDefense     기본 방어력
     * @param itemLevel       드랍 던전 권장레벨
     * @param equipped        착용 여부
     */
    public PlayerArmor(final Long playerId, final Long armorTemplateId,
                       final String displayName, final ArmorSlot armorSlot,
                       final Grade grade, final int baseDefense,
                       final int itemLevel, final boolean equipped) {
        this.playerId = playerId;
        this.armorTemplateId = armorTemplateId;
        this.displayName = displayName;
        this.armorSlot = armorSlot;
        this.grade = grade;
        this.baseDefense = baseDefense;
        this.itemLevel = itemLevel;
        this.equipped = equipped;
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
     * 방어구 템플릿 ID를 반환한다.
     *
     * @return JSON 방어구 ID
     */
    public Long getArmorTemplateId() {
        return armorTemplateId;
    }

    /**
     * 표시명을 반환한다.
     *
     * @return 표시명 ([등급] 템플릿명)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 방어구 부위를 반환한다.
     *
     * @return 방어구 부위
     */
    public ArmorSlot getArmorSlot() {
        return armorSlot;
    }

    /**
     * 등급을 반환한다.
     *
     * @return 등급
     */
    public Grade getGrade() {
        return grade;
    }

    /**
     * 기본 방어력을 반환한다.
     *
     * @return 기본 방어력
     */
    public int getBaseDefense() {
        return baseDefense;
    }

    /**
     * 아이템 레벨을 반환한다.
     *
     * @return 드랍 던전 권장레벨
     */
    public int getItemLevel() {
        return itemLevel;
    }

    /**
     * 착용 여부를 반환한다.
     *
     * @return 착용 중이면 true
     */
    public boolean isEquipped() {
        return equipped;
    }

    /**
     * 착용 상태를 변경한다.
     *
     * @param equipped 새 착용 상태
     */
    public void changeEquipped(final boolean equipped) {
        this.equipped = equipped;
    }
}
