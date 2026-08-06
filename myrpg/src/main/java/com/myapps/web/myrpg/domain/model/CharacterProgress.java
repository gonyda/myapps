package com.myapps.web.myrpg.domain.model;

import java.time.LocalDateTime;

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
 * HP/MP/Stamina 현재값, 현재 맵 노드 id를 보관한다.
 * 스탯과 바이탈 최대값은 저장하지 않으며, 레벨·장비·스킬 등으로부터 매번 계산한다.
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
                             final String currentNodeId) {
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
    }

    /**
     * 신규 캐릭터용 기본 진행상황을 생성한다.
     *
     * <p>닉네임 "고니", Lv1, 누적 Lv1, EXP 0, 재능 MELEE,
     * lastRebirthAt null, HP/MP/Stamina 현재값 100, 시작 노드 "tir-chonaill".
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
                DEFAULT_START_NODE
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
     */
    public void fullRecover(final int max) {
        this.hpCurrent = max;
        this.mpCurrent = max;
        this.staminaCurrent = max;
    }

    /**
     * 현재 맵 노드 id를 갱신한다.
     *
     * @param currentNodeId 새 맵 노드 id
     */
    public void updateCurrentNodeId(final String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
}
