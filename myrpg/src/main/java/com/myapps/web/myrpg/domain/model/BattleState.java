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
 * 진행 중인 전투의 상태를 영속 저장하는 JPA 엔티티.
 *
 * <p>캐릭터당 활성 전투는 최대 1건이며, 매 턴마다 몬스터 현재 HP와 턴 수가 갱신된다. 전투 종료(승/패/도망) 시 {@code active}를 {@code
 * false}로 설정하여 비활성화한다. 브라우저 종료 후에도 활성 전투를 재개할 수 있도록 전투 상태를 DB에 저장한다.
 */
@Entity
@Table(name = "battle_state")
public class BattleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private long characterId;

    @Column(name = "monster_id", nullable = false)
    private String monsterId;

    @Column(name = "monster_current_hp", nullable = false)
    private int monsterCurrentHp;

    @Column(name = "turn_count", nullable = false)
    private int turnCount;

    @Column(nullable = false)
    private boolean ambush;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_monster_intent")
    private SkillType currentMonsterIntent;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean standby;

    @Column(
            name = "dungeon_monster_deducted",
            nullable = false,
            columnDefinition = "boolean default false")
    private boolean dungeonMonsterDeducted;

    /** JPA 전용 기본 생성자. */
    protected BattleState() {}

    /**
     * 전투 시작 시 사용하는 생성자.
     *
     * <p>{@code turnCount}는 1로, {@code active}는 {@code true}, {@code standby}는 {@code true}로 초기화된다.
     *
     * @param characterId 전투에 참여하는 캐릭터 ID
     * @param monsterId 전투 대상 몬스터 식별자
     * @param monsterCurrentHp 몬스터의 현재(최대) HP
     * @param ambush 기습 여부 ({@code true}이면 기습 전투)
     */
    public BattleState(
            final long characterId,
            final String monsterId,
            final int monsterCurrentHp,
            final boolean ambush) {
        this.characterId = characterId;
        this.monsterId = monsterId;
        this.monsterCurrentHp = monsterCurrentHp;
        this.turnCount = 1;
        this.ambush = ambush;
        this.active = true;
        this.standby = true;
        this.currentMonsterIntent = null;
    }

    /**
     * 엔티티의 기본 키를 반환한다.
     *
     * @return 자동 생성된 고유 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 전투에 참여하는 캐릭터 ID를 반환한다.
     *
     * @return 캐릭터 ID
     */
    public long getCharacterId() {
        return characterId;
    }

    /**
     * 전투 대상 몬스터 식별자를 반환한다.
     *
     * @return 몬스터 ID 문자열
     */
    public String getMonsterId() {
        return monsterId;
    }

    /**
     * 몬스터의 현재 HP를 반환한다.
     *
     * @return 몬스터 현재 HP (0 이상)
     */
    public int getMonsterCurrentHp() {
        return monsterCurrentHp;
    }

    /**
     * 몬스터의 현재 HP를 갱신한다.
     *
     * @param monsterCurrentHp 갱신할 몬스터 HP 값
     */
    public void setMonsterCurrentHp(final int monsterCurrentHp) {
        this.monsterCurrentHp = monsterCurrentHp;
    }

    /**
     * 현재 전투 턴 수를 반환한다.
     *
     * @return 턴 수 (시작 시 1)
     */
    public int getTurnCount() {
        return turnCount;
    }

    /**
     * 전투 턴 수를 갱신한다.
     *
     * @param turnCount 갱신할 턴 수
     */
    public void setTurnCount(final int turnCount) {
        this.turnCount = turnCount;
    }

    /**
     * 기습 전투 여부를 반환한다.
     *
     * @return 기습이면 {@code true}
     */
    public boolean isAmbush() {
        return ambush;
    }

    /**
     * 전투 활성 여부를 반환한다.
     *
     * @return 전투가 진행 중이면 {@code true}
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 전투 활성 상태를 갱신한다.
     *
     * <p>전투 종료(승/패/도망) 시 {@code false}로 설정한다.
     *
     * @param active 활성 상태 값
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * 현재 턴에 몬스터가 준비한 의도(스킬 유형)를 반환한다.
     *
     * @return 몬스터 의도, 대치 페이즈이거나 미결정 시 {@code null}
     */
    public SkillType getCurrentMonsterIntent() {
        return currentMonsterIntent;
    }

    /**
     * 현재 턴의 몬스터 의도를 설정한다.
     *
     * @param currentMonsterIntent 설정할 몬스터 의도
     */
    public void setCurrentMonsterIntent(final SkillType currentMonsterIntent) {
        this.currentMonsterIntent = currentMonsterIntent;
    }

    /**
     * 대치(시간 정지) 페이즈 여부를 반환한다.
     *
     * @return 대치 페이즈이면 {@code true}, 공방 페이즈이면 {@code false}
     */
    public boolean isStandby() {
        return standby;
    }

    /**
     * 대치 페이즈 여부를 설정한다.
     *
     * @param standby 대치 여부
     */
    public void setStandby(final boolean standby) {
        this.standby = standby;
    }

    /**
     * 던전 방 몬스터 차감 처리가 이미 수행되었는지 여부를 반환한다.
     *
     * @return 이미 차감되었으면 {@code true}
     */
    public boolean isDungeonMonsterDeducted() {
        return dungeonMonsterDeducted;
    }

    /**
     * 던전 방 몬스터 차감 처리 여부를 설정한다.
     *
     * @param dungeonMonsterDeducted 차감 여부
     */
    public void setDungeonMonsterDeducted(final boolean dungeonMonsterDeducted) {
        this.dungeonMonsterDeducted = dungeonMonsterDeducted;
    }
}
