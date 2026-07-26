package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 플레이어 캐릭터 엔티티.
 *
 * <p>캐릭터의 기본 스탯, 레벨, 경험치, 골드 등 핵심 상태를 관리한다.
 */
@Entity
@Table(name = "rpg_player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int hp;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(nullable = false)
    private int mp;

    @Column(name = "max_mp", nullable = false)
    private int maxMp;

    @Column(nullable = false)
    private int attack;

    @Column(nullable = false)
    private int defense;

    @Column(nullable = false)
    private int speed;

    @Column(nullable = false)
    private int critical;

    @Column(nullable = false)
    private int gold;

    /**
     * JPA 전용 기본 생성자.
     */
    protected Player() {
    }

    /**
     * 플레이어를 생성한다.
     *
     * @param name     캐릭터명
     * @param level    레벨
     * @param exp      현재 경험치
     * @param hp       현재 HP
     * @param maxHp    최대 HP
     * @param mp       현재 MP
     * @param maxMp    최대 MP
     * @param attack   기본 공격력
     * @param defense  기본 방어력
     * @param speed    기본 속도
     * @param critical 기본 치명타
     * @param gold     보유 골드
     */
    public Player(final String name, final int level, final int exp,
                  final int hp, final int maxHp, final int mp, final int maxMp,
                  final int attack, final int defense, final int speed,
                  final int critical, final int gold) {
        this.name = name;
        this.level = level;
        this.exp = exp;
        this.hp = hp;
        this.maxHp = maxHp;
        this.mp = mp;
        this.maxMp = maxMp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.critical = critical;
        this.gold = gold;
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
     * 캐릭터명을 반환한다.
     *
     * @return 캐릭터명
     */
    public String getName() {
        return name;
    }

    /**
     * 레벨을 반환한다.
     *
     * @return 레벨
     */
    public int getLevel() {
        return level;
    }

    /**
     * 현재 경험치를 반환한다.
     *
     * @return 현재 경험치
     */
    public int getExp() {
        return exp;
    }

    /**
     * 현재 HP를 반환한다.
     *
     * @return 현재 HP
     */
    public int getHp() {
        return hp;
    }

    /**
     * 최대 HP를 반환한다.
     *
     * @return 최대 HP
     */
    public int getMaxHp() {
        return maxHp;
    }

    /**
     * 현재 MP를 반환한다.
     *
     * @return 현재 MP
     */
    public int getMp() {
        return mp;
    }

    /**
     * 최대 MP를 반환한다.
     *
     * @return 최대 MP
     */
    public int getMaxMp() {
        return maxMp;
    }

    /**
     * 기본 공격력을 반환한다.
     *
     * @return 기본 공격력
     */
    public int getAttack() {
        return attack;
    }

    /**
     * 기본 방어력을 반환한다.
     *
     * @return 기본 방어력
     */
    public int getDefense() {
        return defense;
    }

    /**
     * 기본 속도를 반환한다.
     *
     * @return 기본 속도
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * 기본 치명타를 반환한다.
     *
     * @return 기본 치명타
     */
    public int getCritical() {
        return critical;
    }

    /**
     * 보유 골드를 반환한다.
     *
     * @return 보유 골드
     */
    public int getGold() {
        return gold;
    }

    /**
     * 레벨을 변경한다.
     *
     * @param level 새 레벨
     */
    public void changeLevel(final int level) {
        this.level = level;
    }

    /**
     * 경험치를 변경한다.
     *
     * @param exp 새 경험치
     */
    public void changeExp(final int exp) {
        this.exp = exp;
    }

    /**
     * 현재 HP를 변경한다.
     *
     * @param hp 새 HP
     */
    public void changeHp(final int hp) {
        this.hp = hp;
    }

    /**
     * 최대 HP를 변경한다.
     *
     * @param maxHp 새 최대 HP
     */
    public void changeMaxHp(final int maxHp) {
        this.maxHp = maxHp;
    }

    /**
     * 현재 MP를 변경한다.
     *
     * @param mp 새 MP
     */
    public void changeMp(final int mp) {
        this.mp = mp;
    }

    /**
     * 최대 MP를 변경한다.
     *
     * @param maxMp 새 최대 MP
     */
    public void changeMaxMp(final int maxMp) {
        this.maxMp = maxMp;
    }

    /**
     * 기본 공격력을 변경한다.
     *
     * @param attack 새 공격력
     */
    public void changeAttack(final int attack) {
        this.attack = attack;
    }

    /**
     * 기본 방어력을 변경한다.
     *
     * @param defense 새 방어력
     */
    public void changeDefense(final int defense) {
        this.defense = defense;
    }

    /**
     * 기본 속도를 변경한다.
     *
     * @param speed 새 속도
     */
    public void changeSpeed(final int speed) {
        this.speed = speed;
    }

    /**
     * 기본 치명타를 변경한다.
     *
     * @param critical 새 치명타
     */
    public void changeCritical(final int critical) {
        this.critical = critical;
    }

    /**
     * 보유 골드를 변경한다.
     *
     * @param gold 새 골드
     */
    public void changeGold(final int gold) {
        this.gold = gold;
    }
}
