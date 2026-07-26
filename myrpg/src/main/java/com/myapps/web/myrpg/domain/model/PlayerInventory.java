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
 * 플레이어 인벤토리(소모품/스킬북) 엔티티.
 *
 * <p>포션 및 스킬북의 보유 수량을 관리한다.
 */
@Entity
@Table(name = "rpg_player_inventory")
public class PlayerInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(name = "item_ref_id", nullable = false)
    private Long itemRefId;

    @Column(nullable = false)
    private int quantity;

    /**
     * JPA 전용 기본 생성자.
     */
    protected PlayerInventory() {
    }

    /**
     * 인벤토리 항목을 생성한다.
     *
     * @param playerId  소유자 식별자
     * @param itemType  아이템 종류 (POTION / SKILL_BOOK)
     * @param itemRefId 참조 ID (skill_id 또는 items.json id)
     * @param quantity  수량
     */
    public PlayerInventory(final Long playerId, final ItemType itemType,
                           final Long itemRefId, final int quantity) {
        this.playerId = playerId;
        this.itemType = itemType;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
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
     * 아이템 종류를 반환한다.
     *
     * @return 아이템 종류
     */
    public ItemType getItemType() {
        return itemType;
    }

    /**
     * 참조 ID를 반환한다.
     *
     * @return 참조 ID (skill_id 또는 items.json id)
     */
    public Long getItemRefId() {
        return itemRefId;
    }

    /**
     * 수량을 반환한다.
     *
     * @return 보유 수량
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 수량을 변경한다.
     *
     * @param quantity 새 수량
     */
    public void changeQuantity(final int quantity) {
        this.quantity = quantity;
    }
}
