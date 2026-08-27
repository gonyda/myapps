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
 * 캐릭터의 보유 스킬 진행을 영속 저장하는 JPA 엔티티.
 *
 * <p>{@code character_skill} 테이블에 매핑되며, 스킬 카탈로그({@code skill.json})의 id를 문자열로 참조한다. 랭크업 시 카운트(사용
 * 횟수·막타 처치)는 0으로 리셋된다.
 *
 * <p>랭크업 영구 스탯 보너스와 랭크별 수치는 별도 컬럼으로 저장하지 않으며, 랭크·카탈로그에서 매번 계산한다(Req 10.2).
 */
@Entity
@Table(name = "character_skill")
public class CharacterSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(name = "skill_id", nullable = false)
    private String skillId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillRank rank;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "kill_count", nullable = false)
    private int killCount;

    @Column(name = "ultimate_cooldown", nullable = false)
    private int ultimateCooldown;

    @Column(name = "slot_index")
    private Integer slotIndex;

    /** JPA 전용 기본 생성자. */
    protected CharacterSkill() {}

    /**
     * 전체 필드를 지정하는 생성자.
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID (skill.json 참조)
     * @param rank 현재 스킬 랭크
     * @param usageCount 현재 랭크 사용 횟수
     * @param killCount 현재 랭크 막타 처치 수
     * @param ultimateCooldown 궁극기 쿨타임(남은 승리 횟수)
     * @param slotIndex 핫바 슬롯 번호 (0~9, 미등록 시 null)
     */
    public CharacterSkill(
            final Long characterId,
            final String skillId,
            final SkillRank rank,
            final int usageCount,
            final int killCount,
            final int ultimateCooldown,
            final Integer slotIndex) {
        this.characterId = characterId;
        this.skillId = skillId;
        this.rank = rank;
        this.usageCount = usageCount;
        this.killCount = killCount;
        this.ultimateCooldown = ultimateCooldown;
        this.slotIndex = slotIndex;
    }

    /**
     * 하위호환 생성자 (slotIndex = null).
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID (skill.json 참조)
     * @param rank 현재 스킬 랭크
     * @param usageCount 현재 랭크 사용 횟수
     * @param killCount 현재 랭크 막타 처치 수
     * @param ultimateCooldown 궁극기 쿨타임(남은 승리 횟수)
     */
    public CharacterSkill(
            final Long characterId,
            final String skillId,
            final SkillRank rank,
            final int usageCount,
            final int killCount,
            final int ultimateCooldown) {
        this(characterId, skillId, rank, usageCount, killCount, ultimateCooldown, null);
    }

    /**
     * 하위호환 생성자 (ultimateCooldown = 0, slotIndex = null).
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID (skill.json 참조)
     * @param rank 현재 스킬 랭크
     * @param usageCount 현재 랭크 사용 횟수
     * @param killCount 현재 랭크 막타 처치 수
     */
    public CharacterSkill(
            final Long characterId,
            final String skillId,
            final SkillRank rank,
            final int usageCount,
            final int killCount) {
        this(characterId, skillId, rank, usageCount, killCount, 0, null);
    }

    /**
     * 신규 스킬을 F 랭크·카운트 0으로 생성한다.
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId 스킬 카탈로그 ID
     * @return 초기 상태의 CharacterSkill
     */
    public static CharacterSkill newSkill(final Long characterId, final String skillId) {
        return new CharacterSkill(characterId, skillId, SkillRank.first(), 0, 0);
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
     * 소유 캐릭터 ID를 반환한다.
     *
     * @return 캐릭터 ID
     */
    public Long getCharacterId() {
        return characterId;
    }

    /**
     * 스킬 카탈로그 ID를 반환한다.
     *
     * @return 스킬 ID 문자열
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 현재 스킬 랭크를 반환한다.
     *
     * @return 스킬 랭크
     */
    public SkillRank getRank() {
        return rank;
    }

    /**
     * 현재 랭크에서의 사용 횟수를 반환한다.
     *
     * @return 사용 횟수
     */
    public int getUsageCount() {
        return usageCount;
    }

    /**
     * 현재 랭크에서의 막타 처치 수를 반환한다.
     *
     * @return 막타 처치 수
     */
    public int getKillCount() {
        return killCount;
    }

    /** 사용 횟수를 1 증가시킨다. */
    public void increaseUsage() {
        this.usageCount++;
    }

    /** 막타 처치 수를 1 증가시킨다. */
    public void increaseKill() {
        this.killCount++;
    }

    /**
     * 사용 횟수를 지정 값으로 설정한다 (임시 드라이버용).
     *
     * <p>전투(7순위)의 실제 사용 이벤트({@code onSkillUsed})가 구현되면 이 메서드를 호출하는 임시 드라이버({@code dev/fill-usage})는
     * 제거된다.
     *
     * @param usageCount 설정할 사용 횟수
     */
    public void setUsageCount(final int usageCount) {
        this.usageCount = usageCount;
    }

    /**
     * 막타 처치 수를 지정 값으로 설정한다 (임시 드라이버용).
     *
     * <p>전투(7순위)의 실제 막타 이벤트({@code onSkillKill})가 구현되면 이 메서드를 호출하는 임시 드라이버({@code dev/fill-kill})는
     * 제거된다.
     *
     * @param killCount 설정할 막타 처치 수
     */
    public void setKillCount(final int killCount) {
        this.killCount = killCount;
    }

    /**
     * 지정 랭크로 승급하고 카운트를 0으로 리셋한다.
     *
     * <p>랭크업 트랜잭션의 (b)+(c) 단계에 해당한다(Req 7).
     *
     * @param next 승급 대상 랭크 (현재 랭크의 next())
     */
    public void rankUpTo(final SkillRank next) {
        this.rank = next;
        this.usageCount = 0;
        this.killCount = 0;
    }

    /**
     * 궁극기 남은 쿨타임(승리 횟수)을 반환한다.
     *
     * @return 궁극기 쿨타임 (0이면 사용 가능)
     */
    public int getUltimateCooldown() {
        return ultimateCooldown;
    }

    /**
     * 궁극기 쿨타임(승리 횟수)을 설정한다.
     *
     * @param ultimateCooldown 설정할 쿨타임 (0 이상)
     */
    public void setUltimateCooldown(final int ultimateCooldown) {
        this.ultimateCooldown = Math.max(0, ultimateCooldown);
    }

    /** 전투 승리 시 궁극기 쿨타임을 1회 차감한다 (하한 0). */
    public void decrementUltimateCooldown() {
        if (this.ultimateCooldown > 0) {
            this.ultimateCooldown--;
        }
    }

    /**
     * 핫바 슬롯 번호(0~9)를 반환한다.
     *
     * @return 슬롯 번호 (미등록 시 null)
     */
    public Integer getSlotIndex() {
        return slotIndex;
    }

    /**
     * 핫바 슬롯 번호를 설정한다.
     *
     * @param slotIndex 슬롯 번호 (0~9, 해제 시 null)
     */
    public void setSlotIndex(final Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    /**
     * 핫바 슬롯에 등록한다 (0~9 범위 검증).
     *
     * @param slotIndex 등록할 슬롯 인덱스 (0~9)
     */
    public void assignToSlot(final int slotIndex) {
        if (slotIndex < 0 || slotIndex > 9) {
            throw new IllegalArgumentException("스킬 슬롯 번호는 0~9 사이여야 합니다: " + slotIndex);
        }
        this.slotIndex = slotIndex;
    }

    /** 핫바 슬롯 등록을 해제한다. */
    public void clearSlot() {
        this.slotIndex = null;
    }
}
