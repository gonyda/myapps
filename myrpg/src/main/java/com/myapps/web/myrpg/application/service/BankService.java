package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.domain.model.Bank;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.repository.BankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 은행 통합 금고 관리 서비스.
 *
 * <p>싱글 플레이어 구조에서 유일한 은행 행을 로드하거나 기본 생성하고, 캐릭터 소지금과 은행 골드 간의 입금/출금을 단일 트랜잭션으로 처리한다. 최소 1골드, 상한 없음,
 * 수수료 없음의 정책을 따른다.
 */
@Service
public class BankService {

    private final BankRepository bankRepository;

    /**
     * BankService를 생성한다.
     *
     * @param bankRepository 은행 엔티티 리포지토리
     */
    public BankService(final BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    /**
     * 은행 행을 로드하거나, 저장된 행이 없으면 기본 행(골드 0)을 생성하여 반환한다.
     *
     * <p>{@code CharacterService.loadOrCreateDefault()} 선례를 따른다. 싱글 플레이어 구조에서 은행 행은 항상 1개만 존재한다.
     *
     * @return 기존 은행 행 또는 새로 생성된 기본 은행 행
     */
    @Transactional
    public Bank loadOrCreateDefault() {
        return bankRepository
                .findFirstByOrderByIdAsc()
                .orElseGet(() -> bankRepository.save(Bank.createDefault()));
    }

    /**
     * 캐릭터 소지금에서 지정 금액을 은행으로 입금한다.
     *
     * <p>단일 트랜잭션 내에서 {@code ch.spendGold(amount)}로 소지금을 차감한 뒤 {@code bank.deposit(amount)}로 은행 골드를
     * 증가시킨다. 소지금이 부족하면 {@code InsufficientGoldException}이 던져지고 트랜잭션이 롤백되어 양쪽 모두 변경되지 않는다.
     *
     * @param ch 소지금을 차감할 캐릭터 진행상황 (JPA 관리 엔티티)
     * @param amount 입금할 금액 (최소 1, 상한 없음, 수수료 없음)
     * @throws com.myapps.web.myrpg.application.exception.InsufficientGoldException 소지금 부족 시
     * @throws IllegalArgumentException amount가 0 이하일 경우
     */
    @Transactional
    public void deposit(final CharacterProgress ch, final long amount) {
        final Bank bank = loadOrCreateDefault();
        ch.spendGold(amount);
        bank.deposit(amount);
    }

    /**
     * 은행에서 지정 금액을 출금하여 캐릭터 소지금으로 이전한다.
     *
     * <p>단일 트랜잭션 내에서 {@code bank.withdraw(amount)}로 은행 골드를 차감한 뒤 {@code ch.gainGold(amount)}로 소지금을
     * 증가시킨다. 은행 잔액이 부족하면 {@code InsufficientGoldException}이 던져지고 트랜잭션이 롤백되어 양쪽 모두 변경되지 않는다.
     *
     * @param ch 소지금을 증가시킬 캐릭터 진행상황 (JPA 관리 엔티티)
     * @param amount 출금할 금액 (최소 1, 상한 없음, 수수료 없음)
     * @throws com.myapps.web.myrpg.application.exception.InsufficientGoldException 은행 잔액 부족 시
     * @throws IllegalArgumentException amount가 0 이하일 경우
     */
    @Transactional
    public void withdraw(final CharacterProgress ch, final long amount) {
        final Bank bank = loadOrCreateDefault();
        bank.withdraw(amount);
        ch.gainGold(amount);
    }
}
