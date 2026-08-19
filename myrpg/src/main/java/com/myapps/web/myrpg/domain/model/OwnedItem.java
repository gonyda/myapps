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
 * 캐릭터의 보유 아이템 인스턴스를 영속 저장하는 JPA 엔티티.
 *
 * <p>{@code owned_item} 테이블에 매핑되며, 아이템 카탈로그({@code item.json})의
 * id를 문자열로 참조한다. 소비형(포션)은 같은 {@code itemId}+{@code storage}가
 * 한 행으로 스택 누적되고, 장비(무기/방어구)는 개별 인스턴스로 저장된다.
 *
 * <p>계정=단일 캐릭터이므로 소유자 식별자를 두지 않는다(향후 다중 캐릭터 확장 시 추가).
 */
@Entity
@Table(name = "owned_item")
public class OwnedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageKind storage;

    @Column(nullable = false)
    private boolean equipped;

    @Column(name = "current_durability", nullable = false)
    private double currentDurability;

    /**
     * JPA 전용 기본 생성자.
     */
    protected OwnedItem() {
    }

    /**
     * 전체 필드를 지정하는 생성자.
     *
     * @param itemId            아이템 카탈로그 ID (item.json 참조)
     * @param quantity          보유 수량 (소비형 스택, 장비는 1)
     * @param storage           저장 위치 (INVENTORY 또는 BANK)
     * @param equipped          장착 여부 (INVENTORY 장비만 true 가능)
     * @param currentDurability 현재 내구도 (장비만 의미, 포션은 0)
     */
    public OwnedItem(final String itemId, final int quantity, final StorageKind storage,
                     final boolean equipped, final double currentDurability) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.storage = storage;
        this.equipped = equipped;
        this.currentDurability = currentDurability;
    }

    /**
     * 엔티티 ID를 반환한다.
     *
     * @return PK (미영속 시 null)
     */
    public Long getId() {
        return id;
    }

    /**
     * 아이템 카탈로그 ID를 반환한다.
     *
     * @return 아이템 ID 문자열
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * 보유 수량을 반환한다.
     *
     * @return 수량
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 저장 위치를 반환한다.
     *
     * @return StorageKind (INVENTORY 또는 BANK)
     */
    public StorageKind getStorage() {
        return storage;
    }

    /**
     * 장착 여부를 반환한다.
     *
     * @return 장착 중이면 true
     */
    public boolean isEquipped() {
        return equipped;
    }

    /**
     * 현재 내구도를 반환한다.
     *
     * @return 현재 내구도 (소수점)
     */
    public double getCurrentDurability() {
        return currentDurability;
    }

    /**
     * 보유 수량을 지정된 양만큼 증가시킨다.
     *
     * @param n 증가량 (양수)
     */
    public void increaseQuantity(final int n) {
        this.quantity += n;
    }

    /**
     * 보유 수량을 지정된 양만큼 감소시킨다.
     *
     * <p>결과가 0 미만이 되지 않도록 0으로 제한한다.
     *
     * @param n 감소량 (양수)
     */
    public void decreaseQuantity(final int n) {
        this.quantity = Math.max(0, this.quantity - n);
    }

    /**
     * 저장 위치를 변경한다 (맡기기/찾기).
     *
     * @param s 변경할 저장 위치
     */
    public void moveTo(final StorageKind s) {
        this.storage = s;
    }

    /**
     * 장비를 장착 상태로 변경한다.
     */
    public void equip() {
        this.equipped = true;
    }

    /**
     * 장비를 장착 해제 상태로 변경한다.
     */
    public void unequip() {
        this.equipped = false;
    }

    /**
     * 내구도를 지정된 양만큼 감소시킨다.
     *
     * <p>전투에서 공격 턴당 0.05씩 감소한다(BattleService에서 호출).
     * 본 스펙(006)에서는 메서드만 정의하며 실제 호출부는 구현하지 않는다.
     * 제거 조건: 6순위 전투 스펙이 이 메서드를 호출하여 확정.
     *
     * <p>결과가 0 미만이 되지 않도록 0으로 제한한다.
     *
     * @param d 감소량 (양수)
     */
    public void reduceDurability(final double d) {
        this.currentDurability = Math.max(0.0, this.currentDurability - d);
    }

    /**
     * 내구도를 지정된 양만큼 복구한다.
     *
     * <p>대장간 수리에서 1포인트 단위로 복구할 때 사용된다.
     * 복구 결과는 최대 내구도(max)를 초과할 수 없다.
     *
     * @param amount 복구할 내구도 양 (양수)
     * @param max    내구도의 최대값
     */
    public void repairBy(final double amount, final double max) {
        this.currentDurability = Math.min(max, this.currentDurability + amount);
    }

    /**
     * 내구도를 최대값으로 복구한다.
     *
     * <p>7순위 대장간 스펙에서 수리비를 소모한 뒤 이 메서드를 호출하여 확정된다.
     * 본 스펙(006)에서는 메서드만 정의하며 실제 호출부는 구현하지 않는다.
     * 제거 조건: 7순위 대장간 스펙이 이 메서드를 호출하여 확정.
     *
     * @param max 복구할 최대 내구도 값
     */
    public void repairToMax(final double max) {
        this.currentDurability = max;
    }
}
