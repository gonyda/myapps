package com.myapps.web.myrpg.application.service;

import java.io.Serial;
import java.io.Serializable;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;

/**
 * 전투 진행 상태를 나타내는 세션 객체.
 *
 * <p>HTTP 세션에 직렬화하여 저장되며, 전투 중 플레이어와 몬스터의
 * 현재 HP/MP, 턴 순서, 턴 수, 전투 결과 상태를 유지한다.
 * DB에는 영속화하지 않는다 (Req 21.2).
 */
public class BattleSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 전투 결과 상태.
     */
    public enum BattleStatus {
        ONGOING,
        PLAYER_WON,
        PLAYER_DEAD,
        PLAYER_FLED
    }

    private final long playerId;
    private final long monsterId;
    private final String monsterName;
    private final int monsterMaxHp;
    private final int monsterAttack;
    private final int monsterDefense;
    private final int monsterSpeed;
    private final DamageType monsterDamageType;
    private final TurnOrder turnOrder;
    private final int playerAttack;
    private final int playerDefense;
    private final int playerSpeed;
    private final int playerCritical;
    private final int playerMaxHp;
    private final DamageType playerDamageType;

    private int playerCurrentHp;
    private int playerCurrentMp;
    private int monsterCurrentHp;
    private int turnCount;
    private boolean playerTurn;
    private BattleStatus status;

    /**
     * BattleSession을 생성한다.
     *
     * @param playerId         플레이어 식별자
     * @param monsterId        몬스터 템플릿 ID
     * @param monsterName      몬스터 이름
     * @param monsterMaxHp     몬스터 최대 HP
     * @param monsterAttack    몬스터 공격력
     * @param monsterDefense   몬스터 방어력
     * @param monsterSpeed     몬스터 속도
     * @param monsterDamageType 몬스터 데미지 타입
     * @param turnOrder        선후공 순서
     * @param playerAttack     플레이어 유효 공격력
     * @param playerDefense    플레이어 유효 방어력
     * @param playerSpeed      플레이어 유효 속도
     * @param playerCritical   플레이어 유효 치명타
     * @param playerMaxHp      플레이어 유효 최대 HP
     * @param playerDamageType 플레이어 데미지 타입
     * @param playerCurrentHp  플레이어 현재 HP
     * @param playerCurrentMp  플레이어 현재 MP
     */
    public BattleSession(final long playerId, final long monsterId, final String monsterName,
                         final int monsterMaxHp, final int monsterAttack, final int monsterDefense,
                         final int monsterSpeed, final DamageType monsterDamageType,
                         final TurnOrder turnOrder,
                         final int playerAttack, final int playerDefense,
                         final int playerSpeed, final int playerCritical,
                         final int playerMaxHp, final DamageType playerDamageType,
                         final int playerCurrentHp, final int playerCurrentMp) {
        this.playerId = playerId;
        this.monsterId = monsterId;
        this.monsterName = monsterName;
        this.monsterMaxHp = monsterMaxHp;
        this.monsterAttack = monsterAttack;
        this.monsterDefense = monsterDefense;
        this.monsterSpeed = monsterSpeed;
        this.monsterDamageType = monsterDamageType;
        this.turnOrder = turnOrder;
        this.playerAttack = playerAttack;
        this.playerDefense = playerDefense;
        this.playerSpeed = playerSpeed;
        this.playerCritical = playerCritical;
        this.playerMaxHp = playerMaxHp;
        this.playerDamageType = playerDamageType;
        this.playerCurrentHp = playerCurrentHp;
        this.playerCurrentMp = playerCurrentMp;
        this.monsterCurrentHp = monsterMaxHp;
        this.turnCount = 1;
        this.playerTurn = (turnOrder == TurnOrder.PLAYER_FIRST);
        this.status = BattleStatus.ONGOING;
    }

    /**
     * 플레이어 식별자를 반환한다.
     *
     * @return 플레이어 ID
     */
    public long getPlayerId() {
        return playerId;
    }

    /**
     * 몬스터 템플릿 ID를 반환한다.
     *
     * @return 몬스터 ID
     */
    public long getMonsterId() {
        return monsterId;
    }

    /**
     * 몬스터 이름을 반환한다.
     *
     * @return 몬스터 이름
     */
    public String getMonsterName() {
        return monsterName;
    }

    /**
     * 몬스터 최대 HP를 반환한다.
     *
     * @return 몬스터 최대 HP
     */
    public int getMonsterMaxHp() {
        return monsterMaxHp;
    }

    /**
     * 몬스터 공격력을 반환한다.
     *
     * @return 몬스터 공격력
     */
    public int getMonsterAttack() {
        return monsterAttack;
    }

    /**
     * 몬스터 방어력을 반환한다.
     *
     * @return 몬스터 방어력
     */
    public int getMonsterDefense() {
        return monsterDefense;
    }

    /**
     * 몬스터 속도를 반환한다.
     *
     * @return 몬스터 속도
     */
    public int getMonsterSpeed() {
        return monsterSpeed;
    }

    /**
     * 몬스터 데미지 타입을 반환한다.
     *
     * @return 몬스터 데미지 타입
     */
    public DamageType getMonsterDamageType() {
        return monsterDamageType;
    }

    /**
     * 선후공 순서를 반환한다.
     *
     * @return 턴 순서
     */
    public TurnOrder getTurnOrder() {
        return turnOrder;
    }

    /**
     * 플레이어 유효 공격력을 반환한다.
     *
     * @return 플레이어 공격력
     */
    public int getPlayerAttack() {
        return playerAttack;
    }

    /**
     * 플레이어 유효 방어력을 반환한다.
     *
     * @return 플레이어 방어력
     */
    public int getPlayerDefense() {
        return playerDefense;
    }

    /**
     * 플레이어 유효 속도를 반환한다.
     *
     * @return 플레이어 속도
     */
    public int getPlayerSpeed() {
        return playerSpeed;
    }

    /**
     * 플레이어 유효 치명타를 반환한다.
     *
     * @return 플레이어 치명타
     */
    public int getPlayerCritical() {
        return playerCritical;
    }

    /**
     * 플레이어 유효 최대 HP를 반환한다.
     *
     * @return 플레이어 최대 HP
     */
    public int getPlayerMaxHp() {
        return playerMaxHp;
    }

    /**
     * 플레이어 데미지 타입을 반환한다.
     *
     * @return 플레이어 데미지 타입
     */
    public DamageType getPlayerDamageType() {
        return playerDamageType;
    }

    /**
     * 플레이어 현재 HP를 반환한다.
     *
     * @return 현재 HP
     */
    public int getPlayerCurrentHp() {
        return playerCurrentHp;
    }

    /**
     * 플레이어 현재 MP를 반환한다.
     *
     * @return 현재 MP
     */
    public int getPlayerCurrentMp() {
        return playerCurrentMp;
    }

    /**
     * 몬스터 현재 HP를 반환한다.
     *
     * @return 몬스터 현재 HP
     */
    public int getMonsterCurrentHp() {
        return monsterCurrentHp;
    }

    /**
     * 현재 턴 수를 반환한다.
     *
     * @return 턴 수
     */
    public int getTurnCount() {
        return turnCount;
    }

    /**
     * 플레이어 턴 여부를 반환한다.
     *
     * @return 플레이어 턴이면 true
     */
    public boolean isPlayerTurn() {
        return playerTurn;
    }

    /**
     * 전투 결과 상태를 반환한다.
     *
     * @return 전투 상태
     */
    public BattleStatus getStatus() {
        return status;
    }

    /**
     * 플레이어 현재 HP를 변경한다.
     *
     * @param hp 새 HP 값
     */
    public void changePlayerCurrentHp(final int hp) {
        this.playerCurrentHp = Math.max(0, hp);
    }

    /**
     * 플레이어 현재 MP를 변경한다.
     *
     * @param mp 새 MP 값
     */
    public void changePlayerCurrentMp(final int mp) {
        this.playerCurrentMp = Math.max(0, mp);
    }

    /**
     * 몬스터 현재 HP를 변경한다.
     *
     * @param hp 새 HP 값
     */
    public void changeMonsterCurrentHp(final int hp) {
        this.monsterCurrentHp = Math.max(0, hp);
    }

    /**
     * 턴 수를 증가시킨다.
     */
    public void incrementTurn() {
        this.turnCount++;
    }

    /**
     * 플레이어 턴 여부를 설정한다.
     *
     * @param playerTurn 플레이어 턴이면 true
     */
    public void setPlayerTurn(final boolean playerTurn) {
        this.playerTurn = playerTurn;
    }

    /**
     * 전투 상태를 변경한다.
     *
     * @param status 새 전투 상태
     */
    public void changeStatus(final BattleStatus status) {
        this.status = status;
    }

    /**
     * 전투가 진행 중인지 확인한다.
     *
     * @return 전투 진행 중이면 true
     */
    public boolean isOngoing() {
        return status == BattleStatus.ONGOING;
    }
}
