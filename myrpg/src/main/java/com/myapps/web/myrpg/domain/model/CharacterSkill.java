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
 * <p>{@code character_skill} 테이블에 매핑되며, 스킬 카탈로그({@code skill.json})의
 * id를 문자열로 참조한다. 랭크업 시 카운트(사용 횟수·막타 처치)는 0으로 리셋된다.
 *
 * <p>랭크업 영구 스탯 보너스와 랭크별 수치는 별도 컬럼으로 저장하지 않으며,
 * 랭크·카탈로그에서 매번 계산한다(Req 10.2).
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

    /**
     * JPA 전용 기본 생성자.
     */
    protected CharacterSkill() {
    }

    /**
     * 전체 필드를 지정하는 생성자.
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId     스킬 카탈로그 ID (skill.json 참조)
     * @param rank        현재 스킬 랭크
     * @param usageCount  현재 랭크 사용 횟수
     * @param killCount   현재 랭크 막타 처치 수
     */
    public CharacterSkill(final Long characterId, final String skillId,
                          final SkillRank rank, final int usageCount, final int killCount) {
        this.characterId = characterId;
        this.skillId = skillId;
        this.rank = rank;
        this.usageCount = usageCount;
        this.killCount = killCount;
    }

    /**
     * 신규 스킬을 F 랭크·카운트 0으로 생성한다.
     *
     * @param characterId 소유 캐릭터 ID
     * @param skillId     스킬 카탈로그 ID
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

    /**
     * 사용 횟수를 1 증가시킨다.
     */
    public void increaseUsage() {
        this.usageCount++;
    }

    /**
     * 막타 처치 수를 1 증가시킨다.
     */
    public void increaseKill() {
        this.killCount++;
    }

    /**
     * 사용 횟수를 지정 값으로 설정한다 (임시 드라이버용).
     *
     * <p>전투(7순위)의 실제 사용 이벤트({@code onSkillUsed})가 구현되면
     * 이 메서드를 호출하는 임시 드라이버({@code dev/fill-usage})는 제거된다.
     *
     * @param usageCount 설정할 사용 횟수
     */
    public void setUsageCount(final int usageCount) {
        this.usageCount = usageCount;
    }

    /**
     * 막타 처치 수를 지정 값으로 설정한다 (임시 드라이버용).
     *
     * <p>전투(7순위)의 실제 막타 이벤트({@code onSkillKill})가 구현되면
     * 이 메서드를 호출하는 임시 드라이버({@code dev/fill-kill})는 제거된다.
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
}
