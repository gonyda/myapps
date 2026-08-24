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

    @Enumerated(EnumType.STRING)
    @Column(name = "preemptive_party")
    private PreemptiveParty preemptiveParty;

    @Column(name = "next_attack_amp_percent", nullable = false, columnDefinition = "int default 0")
    private int nextAttackAmpPercent;

    @Column(name = "mana_shield_turns_left", nullable = false, columnDefinition = "int default 0")
    private int manaShieldTurnsLeft;

    @Column(name = "mana_shield_absorb_rate", nullable = false, columnDefinition = "int default 0")
    private int manaShieldAbsorbRate;

    @Column(name = "monster_stunned_turns", nullable = false, columnDefinition = "int default 0")
    private int monsterStunnedTurns;

    @Column(name = "dot_damage_per_turn", nullable = false, columnDefinition = "int default 0")
    private int dotDamagePerTurn;

    @Column(name = "dot_turns_left", nullable = false, columnDefinition = "int default 0")
    private int dotTurnsLeft;

    /** JPA 전용 기본 생성자. */
    protected BattleState() {}

    /**
     * 전투 시작 시 사용하는 생성자.
     *
     * <p>{@code turnCount}는 1로, {@code active}는 {@code true}, {@code standby}는 {@code true}, {@code
     * preemptiveParty}는 {@link PreemptiveParty#NONE}으로 초기화된다.
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
        this.preemptiveParty = PreemptiveParty.NONE;
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

    /**
     * 다음 턴 확정 선제공격 권한을 가진 주체를 반환한다.
     *
     * @return 선제공격 권한 주체, 없으면 {@link PreemptiveParty#NONE}
     */
    public PreemptiveParty getPreemptiveParty() {
        return preemptiveParty != null ? preemptiveParty : PreemptiveParty.NONE;
    }

    /**
     * 다음 턴 확정 선제공격 권한 주체를 설정한다.
     *
     * @param preemptiveParty 설정할 선제공격 권한 주체
     */
    public void setPreemptiveParty(final PreemptiveParty preemptiveParty) {
        this.preemptiveParty = preemptiveParty != null ? preemptiveParty : PreemptiveParty.NONE;
    }

    /**
     * 다음 공격 피해 증폭율(%)을 반환한다 (레이지 임팩트 디버프).
     *
     * @return 피해 증폭율 (기본 0, 발동 시 30)
     */
    public int getNextAttackAmpPercent() {
        return nextAttackAmpPercent;
    }

    /**
     * 다음 공격 피해 증폭율(%)을 설정한다.
     *
     * @param nextAttackAmpPercent 증폭율
     */
    public void setNextAttackAmpPercent(final int nextAttackAmpPercent) {
        this.nextAttackAmpPercent = nextAttackAmpPercent;
    }

    /**
     * 마나 실드 남은 지속 턴 수를 반환한다.
     *
     * @return 남은 턴 수 (0이면 비활성)
     */
    public int getManaShieldTurnsLeft() {
        return manaShieldTurnsLeft;
    }

    /**
     * 마나 실드 남은 지속 턴 수를 설정한다.
     *
     * @param manaShieldTurnsLeft 남은 턴 수
     */
    public void setManaShieldTurnsLeft(final int manaShieldTurnsLeft) {
        this.manaShieldTurnsLeft = Math.max(0, manaShieldTurnsLeft);
    }

    /** 마나 실드 지속 턴 수를 1 감소시킨다 (하한 0). */
    public void decrementManaShieldTurns() {
        if (this.manaShieldTurnsLeft > 0) {
            this.manaShieldTurnsLeft--;
        }
    }

    /**
     * 마나 실드가 활성화되어 있는지 여부를 반환한다.
     *
     * @return 활성화 시 {@code true}
     */
    public boolean hasActiveManaShield() {
        return this.manaShieldTurnsLeft > 0;
    }

    /**
     * 마나 실드 피해 감쇄율(%)을 반환한다.
     *
     * @return 감쇄율 (0~100)
     */
    public int getManaShieldAbsorbRate() {
        return manaShieldAbsorbRate;
    }

    /**
     * 마나 실드 피해 감쇄율(%)을 설정한다.
     *
     * @param manaShieldAbsorbRate 감쇄율
     */
    public void setManaShieldAbsorbRate(final int manaShieldAbsorbRate) {
        this.manaShieldAbsorbRate = manaShieldAbsorbRate;
    }

    /**
     * 몬스터 기절/속박 남은 턴 수를 반환한다.
     *
     * @return 기절 남은 턴 수 (0이면 정상)
     */
    public int getMonsterStunnedTurns() {
        return monsterStunnedTurns;
    }

    /**
     * 몬스터 기절/속박 남은 턴 수를 설정한다.
     *
     * @param monsterStunnedTurns 기절 턴 수
     */
    public void setMonsterStunnedTurns(final int monsterStunnedTurns) {
        this.monsterStunnedTurns = Math.max(0, monsterStunnedTurns);
    }

    /** 몬스터 기절 턴 수를 1 감소시킨다 (하한 0). */
    public void decrementMonsterStunnedTurns() {
        if (this.monsterStunnedTurns > 0) {
            this.monsterStunnedTurns--;
        }
    }

    /**
     * 몬스터가 기절/속박 상태인지 여부를 반환한다.
     *
     * @return 기절 상태이면 {@code true}
     */
    public boolean isMonsterStunned() {
        return this.monsterStunnedTurns > 0;
    }

    /**
     * 턴당 독 도트 피해량을 반환한다.
     *
     * @return 턴당 독 피해량
     */
    public int getDotDamagePerTurn() {
        return dotDamagePerTurn;
    }

    /**
     * 턴당 독 도트 피해량을 설정한다.
     *
     * @param dotDamagePerTurn 독 피해량
     */
    public void setDotDamagePerTurn(final int dotDamagePerTurn) {
        this.dotDamagePerTurn = dotDamagePerTurn;
    }

    /**
     * 독 지속 피해 남은 턴 수를 반환한다.
     *
     * @return 독 남은 턴 수
     */
    public int getDotTurnsLeft() {
        return dotTurnsLeft;
    }

    /**
     * 독 지속 피해 남은 턴 수를 설정한다.
     *
     * @param dotTurnsLeft 독 턴 수
     */
    public void setDotTurnsLeft(final int dotTurnsLeft) {
        this.dotTurnsLeft = Math.max(0, dotTurnsLeft);
    }

    /** 독 지속 턴 수를 1 감소시킨다 (하한 0). */
    public void decrementDotTurns() {
        if (this.dotTurnsLeft > 0) {
            this.dotTurnsLeft--;
        }
    }

    /**
     * 독 지속 피해가 활성화되어 있는지 여부를 반환한다.
     *
     * @return 독 활성 시 {@code true}
     */
    public boolean hasActiveDot() {
        return this.dotTurnsLeft > 0 && this.dotDamagePerTurn > 0;
    }
}
