package com.myapps.web.myrpg.domain.model;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 통합 은행 금고를 영속 저장하는 JPA 엔티티.
 *
 * <p>싱글 플레이어 구조에서 하나의 행만 유지하며,
 * 캐릭터가 NPC 은행을 통해 골드를 입금하거나 출금할 때 사용된다.
 * 은행 골드는 사망 패널티·환생에 영향받지 않는 안전 보관소 역할을 한다.
 */
@Entity
@Table(name = "bank")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long gold;

    /**
     * JPA 전용 기본 생성자.
     */
    protected Bank() {
    }

    /**
     * 지정된 골드량으로 은행을 생성한다.
     *
     * @param gold 초기 보관 골드
     */
    private Bank(final long gold) {
        this.gold = gold;
    }

    /**
     * 기본 은행 인스턴스를 생성한다 (골드 0).
     *
     * <p>최초 은행 조회 시 저장된 행이 없으면 이 메서드로 기본 행을 생성한다.
     *
     * @return 골드 0인 Bank 인스턴스
     */
    public static Bank createDefault() {
        return new Bank(0L);
    }

    /**
     * 엔티티 식별자를 반환한다.
     *
     * @return 기본 키
     */
    public Long getId() {
        return id;
    }

    /**
     * 보관 중인 골드를 반환한다.
     *
     * @return 은행 보관 골드 (0 이상)
     */
    public long getGold() {
        return gold;
    }

    /**
     * 지정된 금액을 은행에 입금한다.
     *
     * <p>캐릭터 소지금에서 차감된 골드를 은행으로 이전할 때 사용한다.
     * 금액이 0 이하이면 예외를 던진다.
     *
     * @param amount 입금할 금액 (양수)
     * @throws IllegalArgumentException amount가 0 이하일 경우
     */
    public void deposit(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금액은 양수여야 합니다: " + amount);
        }
        this.gold += amount;
    }

    /**
     * 지정된 금액을 은행에서 출금한다.
     *
     * <p>은행 보관 골드를 캐릭터 소지금으로 이전할 때 사용한다.
     * 금액이 0 이하이면 {@link IllegalArgumentException}을,
     * 보관 골드를 초과하면 {@link InsufficientGoldException}을 던지고 잔액을 변경하지 않는다.
     *
     * @param amount 출금할 금액 (양수)
     * @throws IllegalArgumentException  amount가 0 이하일 경우
     * @throws InsufficientGoldException 출금액이 보관 골드를 초과할 경우
     */
    public void withdraw(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("출금액은 양수여야 합니다: " + amount);
        }
        if (amount > this.gold) {
            throw new InsufficientGoldException(
                    "은행 잔액 부족: 출금 요청 " + amount + ", 보관 " + this.gold);
        }
        this.gold -= amount;
    }
}
