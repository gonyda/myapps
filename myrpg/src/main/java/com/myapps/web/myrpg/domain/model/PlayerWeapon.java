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
 * 플레이어 보유 무기 인스턴스 엔티티.
 *
 * <p>드랍 시 생성되며 등급·능력치·스킬슬롯이 개별적으로 롤된다.
 */
@Entity
@Table(name = "rpg_player_weapon")
public class PlayerWeapon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "weapon_template_id", nullable = false)
    private Long weaponTemplateId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "weapon_type", nullable = false)
    private WeaponType weaponType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Column(name = "item_level", nullable = false)
    private int itemLevel;

    @Column(name = "base_attack", nullable = false)
    private int baseAttack;

    @Column(name = "base_speed", nullable = false)
    private int baseSpeed;

    @Column(name = "base_critical", nullable = false)
    private int baseCritical;

    @Column(name = "skill_slots", nullable = false)
    private int skillSlots;

    @Column(name = "is_equipped", nullable = false)
    private boolean equipped;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerWeapon() {
    }

    /**
     * 무기 인스턴스를 생성한다.
     *
     * @param playerId         소유자 식별자
     * @param weaponTemplateId JSON 무기 템플릿 ID
     * @param displayName      표시명 ([등급] 템플릿명)
     * @param weaponType       무기 타입
     * @param grade            롤된 등급
     * @param itemLevel        드랍 던전 권장레벨
     * @param baseAttack       롤된 기본공격력
     * @param baseSpeed        타입 고유 속도 보너스
     * @param baseCritical     타입 고유 치명타 보너스
     * @param skillSlots       스킬슬롯 수
     * @param equipped         착용 여부
     */
    public PlayerWeapon(final Long playerId, final Long weaponTemplateId,
                        final String displayName, final WeaponType weaponType,
                        final Grade grade, final int itemLevel,
                        final int baseAttack, final int baseSpeed, final int baseCritical,
                        final int skillSlots, final boolean equipped) {
        this.playerId = playerId;
        this.weaponTemplateId = weaponTemplateId;
        this.displayName = displayName;
        this.weaponType = weaponType;
        this.grade = grade;
        this.itemLevel = itemLevel;
        this.baseAttack = baseAttack;
        this.baseSpeed = baseSpeed;
        this.baseCritical = baseCritical;
        this.skillSlots = skillSlots;
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
     * 무기 템플릿 ID를 반환한다.
     *
     * @return JSON 무기 ID
     */
    public Long getWeaponTemplateId() {
        return weaponTemplateId;
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
     * 무기 타입을 반환한다.
     *
     * @return 무기 타입
     */
    public WeaponType getWeaponType() {
        return weaponType;
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
     * 아이템 레벨을 반환한다.
     *
     * @return 드랍 던전 권장레벨
     */
    public int getItemLevel() {
        return itemLevel;
    }

    /**
     * 롤된 기본공격력을 반환한다.
     *
     * @return 기본공격력
     */
    public int getBaseAttack() {
        return baseAttack;
    }

    /**
     * 무기 타입 고유 속도 보너스를 반환한다.
     *
     * @return 속도 보너스
     */
    public int getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * 무기 타입 고유 치명타 보너스를 반환한다.
     *
     * @return 치명타 보너스
     */
    public int getBaseCritical() {
        return baseCritical;
    }

    /**
     * 스킬슬롯 수를 반환한다.
     *
     * @return 스킬슬롯 수
     */
    public int getSkillSlots() {
        return skillSlots;
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
