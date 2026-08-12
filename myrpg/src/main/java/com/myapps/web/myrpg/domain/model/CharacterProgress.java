package com.myapps.web.myrpg.domain.model;

import java.time.LocalDateTime;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 캐릭터 진행상황을 영속 저장하는 유일한 JPA 엔티티.
 *
 * <p>닉네임, 현재 레벨, 누적 레벨, 경험치, 재능, 마지막 환생 시각,
 * HP/MP/Stamina 현재값, 현재 맵 노드 id, 보유 AP(어빌리티 포인트)를 보관한다.
 * 스탯과 바이탈 최대값은 저장하지 않으며, 레벨·재능 등으로부터 매번 계산한다.
 *
 * <p>안정적인 기본 키({@code id})를 통해 향후 인벤토리, 장착 장비,
 * 스킬 목록 등 별도 연관 엔티티를 확장할 수 있다 (Req 10.1).
 */
@Entity
@Table(name = "character_progress")
public class CharacterProgress {

    private static final String DEFAULT_NICKNAME = "고니";
    private static final int DEFAULT_LEVEL = 1;
    private static final long DEFAULT_EXPERIENCE = 0L;
    private static final int DEFAULT_VITAL_CURRENT = 100;
    private static final String DEFAULT_START_NODE = "tir-chonaill";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "accumulated_level", nullable = false)
    private int accumulatedLevel;

    @Column(nullable = false)
    private long experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TalentType talent;

    @Column(name = "last_rebirth_at")
    private LocalDateTime lastRebirthAt;

    @Column(name = "hp_current", nullable = false)
    private int hpCurrent;

    @Column(name = "mp_current", nullable = false)
    private int mpCurrent;

    @Column(name = "stamina_current", nullable = false)
    private int staminaCurrent;

    @Column(name = "current_node_id", nullable = false)
    private String currentNodeId;

    @Column(name = "ability_points", nullable = false)
    private int abilityPoints;

    @Column(nullable = false)
    private long gold;

    /**
     * JPA 전용 기본 생성자.
     */
    protected CharacterProgress() {
    }

    /**
     * 모든 필드를 받아 캐릭터 진행상황을 생성한다.
     *
     * @param nickname         닉네임
     * @param currentLevel     현재 레벨
     * @param accumulatedLevel 누적 레벨
     * @param experience       경험치
     * @param talent           재능 유형
     * @param lastRebirthAt    마지막 환생 시각 (환생 이력 없으면 null)
     * @param hpCurrent        HP 현재값
     * @param mpCurrent        MP 현재값
     * @param staminaCurrent   Stamina 현재값
     * @param currentNodeId    현재 맵 노드 id
     * @param abilityPoints    보유 어빌리티 포인트
     * @param gold             보유 골드
     */
    public CharacterProgress(final String nickname,
                             final int currentLevel,
                             final int accumulatedLevel,
                             final long experience,
                             final TalentType talent,
                             final LocalDateTime lastRebirthAt,
                             final int hpCurrent,
                             final int mpCurrent,
                             final int staminaCurrent,
                             final String currentNodeId,
                             final int abilityPoints,
                             final long gold) {
        this.nickname = nickname;
        this.currentLevel = currentLevel;
        this.accumulatedLevel = accumulatedLevel;
        this.experience = experience;
        this.talent = talent;
        this.lastRebirthAt = lastRebirthAt;
        this.hpCurrent = hpCurrent;
        this.mpCurrent = mpCurrent;
        this.staminaCurrent = staminaCurrent;
        this.currentNodeId = currentNodeId;
        this.abilityPoints = abilityPoints;
        this.gold = gold;
    }

    /**
     * 신규 캐릭터용 기본 진행상황을 생성한다.
     *
     * <p>닉네임 "고니", Lv1, 누적 Lv1, EXP 0, 재능 MELEE,
     * lastRebirthAt null, HP/MP/Stamina 현재값 100, 시작 노드 "tir-chonaill",
     * AP 0.
     *
     * @return 기본값이 설정된 CharacterProgress 인스턴스
     */
    public static CharacterProgress createDefault() {
        return new CharacterProgress(
                DEFAULT_NICKNAME,
                DEFAULT_LEVEL,
                DEFAULT_LEVEL,
                DEFAULT_EXPERIENCE,
                TalentType.MELEE,
                null,
                DEFAULT_VITAL_CURRENT,
                DEFAULT_VITAL_CURRENT,
                DEFAULT_VITAL_CURRENT,
                DEFAULT_START_NODE,
                0,
                0L
        );
    }

    // ─── Getters ────────────────────────────────────────────────────────────

    /**
     * 엔티티 식별자를 반환한다.
     *
     * @return 기본 키 (향후 연관 엔티티 확장 지점)
     */
    public Long getId() {
        return id;
    }

    /**
     * 닉네임을 반환한다.
     *
     * @return 닉네임
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 현재 레벨을 반환한다.
     *
     * @return 현재 레벨
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 누적 레벨을 반환한다.
     *
     * @return 누적 레벨
     */
    public int getAccumulatedLevel() {
        return accumulatedLevel;
    }

    /**
     * 경험치를 반환한다.
     *
     * @return 경험치
     */
    public long getExperience() {
        return experience;
    }

    /**
     * 재능 유형을 반환한다.
     *
     * @return 재능 유형
     */
    public TalentType getTalent() {
        return talent;
    }

    /**
     * 마지막 환생 시각을 반환한다.
     *
     * @return 마지막 환생 시각 (환생 이력 없으면 null)
     */
    public LocalDateTime getLastRebirthAt() {
        return lastRebirthAt;
    }

    /**
     * HP 현재값을 반환한다.
     *
     * @return HP 현재값
     */
    public int getHpCurrent() {
        return hpCurrent;
    }

    /**
     * MP 현재값을 반환한다.
     *
     * @return MP 현재값
     */
    public int getMpCurrent() {
        return mpCurrent;
    }

    /**
     * Stamina 현재값을 반환한다.
     *
     * @return Stamina 현재값
     */
    public int getStaminaCurrent() {
        return staminaCurrent;
    }

    /**
     * 현재 맵 노드 id를 반환한다.
     *
     * @return 현재 노드 id
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    /**
     * 보유 어빌리티 포인트(AP)를 반환한다.
     *
     * @return 보유 AP 잔량
     */
    public int getAbilityPoints() {
        return abilityPoints;
    }

    /**
     * 보유 골드를 반환한다.
     *
     * @return 보유 골드 (0 이상)
     */
    public long getGold() {
        return gold;
    }

    // ─── Mutators ───────────────────────────────────────────────────────────

    /**
     * 현재 레벨을 설정한다.
     *
     * @param currentLevel 새 현재 레벨
     */
    public void setCurrentLevel(final int currentLevel) {
        this.currentLevel = currentLevel;
    }

    /**
     * 누적 레벨을 지정된 양만큼 증가시킨다.
     *
     * @param amount 증가시킬 양
     */
    public void increaseAccumulatedLevel(final int amount) {
        this.accumulatedLevel += amount;
    }

    /**
     * 경험치를 설정한다.
     *
     * @param experience 새 경험치 값
     */
    public void setExperience(final long experience) {
        this.experience = experience;
    }

    /**
     * 재능 유형을 설정한다.
     *
     * @param talent 새 재능 유형
     */
    public void setTalent(final TalentType talent) {
        this.talent = talent;
    }

    /**
     * 마지막 환생 시각을 설정한다.
     *
     * @param lastRebirthAt 환생 시각
     */
    public void setLastRebirthAt(final LocalDateTime lastRebirthAt) {
        this.lastRebirthAt = lastRebirthAt;
    }

    /**
     * HP, MP, Stamina 현재값을 최대값으로 완전 회복한다.
     *
     * @param max 회복할 최대값 (HP/MP/Stamina 모두 동일하게 적용)
     * @deprecated 바이탈별 최대치를 지원하는 {@link #fullRecover(VitalMax)}를 사용할 것
     */
    public void fullRecover(final int max) {
        this.hpCurrent = max;
        this.mpCurrent = max;
        this.staminaCurrent = max;
    }

    /**
     * HP, MP, Stamina 현재값을 바이탈별 최대치로 완전 회복한다.
     *
     * <p>재능에 따라 각 바이탈의 최대치가 다를 수 있으므로,
     * {@link VitalMax}의 각 필드를 대응하는 현재값에 대입한다.
     *
     * @param vitalMax 바이탈별 최대치 (hp, mp, stamina 각각)
     */
    public void fullRecover(final VitalMax vitalMax) {
        this.hpCurrent = vitalMax.hp();
        this.mpCurrent = vitalMax.mp();
        this.staminaCurrent = vitalMax.stamina();
    }

    /**
     * 보유 어빌리티 포인트를 지정된 양만큼 증가시킨다.
     *
     * <p>레벨업 시 또는 환생 시 AP 지급에 사용된다.
     *
     * @param amount 증가시킬 양 (양수)
     */
    public void increaseAbilityPoints(final int amount) {
        this.abilityPoints += amount;
    }

    /**
     * 보유 어빌리티 포인트를 지정된 양만큼 소모한다.
     *
     * <p>스킬 랭크업 등의 AP 소모에 사용된다.
     * 소모량이 보유량을 초과하면 선행조건 위반으로 예외를 던진다.
     *
     * @param amount 소모할 양 (양수)
     * @throws IllegalArgumentException 소모량이 보유 AP를 초과할 경우
     */
    public void spendAbilityPoints(final int amount) {
        if (amount > this.abilityPoints) {
            throw new IllegalArgumentException(
                    "AP 부족: 소모 요청 " + amount + ", 보유 " + this.abilityPoints);
        }
        this.abilityPoints -= amount;
    }

    /**
     * 현재 맵 노드 id를 갱신한다.
     *
     * @param currentNodeId 새 맵 노드 id
     */
    public void updateCurrentNodeId(final String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    /**
     * 소지금을 지정된 양만큼 증가시킨다.
     *
     * <p>몬스터 드랍, 아이템 판매, 은행 출금 등으로 골드를 획득할 때 사용한다.
     *
     * @param amount 증가시킬 양 (양수)
     * @throws IllegalArgumentException amount가 0 이하일 경우
     */
    public void gainGold(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("골드 획득량은 양수여야 합니다: " + amount);
        }
        this.gold += amount;
    }

    /**
     * 소지금을 지정된 양만큼 차감한다.
     *
     * <p>상점 구매, 수리, 은행 입금 등으로 골드를 소모할 때 사용한다.
     * 소모량이 보유 골드를 초과하면 소지금을 변경하지 않고 예외를 던진다.
     *
     * @param amount 소모할 양 (양수)
     * @throws IllegalArgumentException      amount가 0 이하일 경우
     * @throws InsufficientGoldException     소모량이 보유 골드를 초과할 경우
     */
    public void spendGold(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("골드 소모량은 양수여야 합니다: " + amount);
        }
        if (amount > this.gold) {
            throw new InsufficientGoldException(
                    "골드 부족: 소모 요청 " + amount + ", 보유 " + this.gold);
        }
        this.gold -= amount;
    }

    /**
     * HP를 지정된 양만큼 감소시킨다.
     *
     * <p>전투 중 피해를 받았을 때 사용한다.
     * HP는 0 미만으로 내려가지 않으며, 0에 도달하면 사망 상태({@link #isDead()})가 된다.
     *
     * @param amount 감소시킬 피해량 (양수)
     */
    public void damageHp(final int amount) {
        this.hpCurrent = Math.max(0, this.hpCurrent - amount);
    }

    /**
     * 캐릭터가 사망 상태인지 확인한다.
     *
     * <p>HP 현재값이 0이면 사망으로 판정한다.
     *
     * @return HP가 0이면 {@code true}, 아니면 {@code false}
     */
    public boolean isDead() {
        return this.hpCurrent == 0;
    }
}
