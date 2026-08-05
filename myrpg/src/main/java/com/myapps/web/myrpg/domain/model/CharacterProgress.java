package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 캐릭터 진행상황을 영속 저장하는 유일한 JPA 엔티티.
 *
 * <p>닉네임, 레벨, 경험치, 스탯(STR/DEX/INT/Critical/DEF),
 * HP/MP/Stamina, 현재 맵 노드 id를 보관한다.
 * 안정적인 기본 키({@code id})를 통해 향후 인벤토리, 장착 장비,
 * 스킬 목록 등 별도 연관 엔티티를 확장할 수 있다 (Req 10.1).
 */
@Entity
@Table(name = "character_progress")
public class CharacterProgress {

    private static final String DEFAULT_NICKNAME = "고니";
    private static final int DEFAULT_LEVEL = 1;
    private static final long DEFAULT_EXPERIENCE = 0L;
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

    @Embedded
    private Stats stats;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "current", column = @Column(name = "hp_current", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "hp_max", nullable = false))
    })
    private Vital hp;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "current", column = @Column(name = "mp_current", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "mp_max", nullable = false))
    })
    private Vital mp;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "current", column = @Column(name = "stamina_current", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "stamina_max", nullable = false))
    })
    private Vital stamina;

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
     * @param stats            기본 스탯 (STR/DEX/INT/Critical/DEF)
     * @param hp               HP (현재/최대)
     * @param mp               MP (현재/최대)
     * @param stamina          Stamina (현재/최대)
     * @param currentNodeId    현재 맵 노드 id
     */
    public CharacterProgress(final String nickname,
                             final int currentLevel,
                             final int accumulatedLevel,
                             final long experience,
                             final Stats stats,
                             final Vital hp,
                             final Vital mp,
                             final Vital stamina,
                             final String currentNodeId) {
        this.nickname = nickname;
        this.currentLevel = currentLevel;
        this.accumulatedLevel = accumulatedLevel;
        this.experience = experience;
        this.stats = stats;
        this.hp = hp;
        this.mp = mp;
        this.stamina = stamina;
        this.currentNodeId = currentNodeId;
    }

    /**
     * 신규 캐릭터용 기본 진행상황을 생성한다.
     *
     * <p>닉네임 "고니", Lv1, 누적 Lv1, EXP 0, 기본 스탯,
     * HP/MP/Stamina 100/100, 시작 노드 "tir-chonaill".
     *
     * @return 기본값이 설정된 CharacterProgress 인스턴스
     */
    public static CharacterProgress createDefault() {
        return new CharacterProgress(
                DEFAULT_NICKNAME,
                DEFAULT_LEVEL,
                DEFAULT_LEVEL,
                DEFAULT_EXPERIENCE,
                Stats.createDefault(),
                Vital.createDefault(),
                Vital.createDefault(),
                Vital.createDefault(),
                DEFAULT_START_NODE
        );
    }

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
     * 기본 스탯을 반환한다.
     *
     * @return Stats (STR/DEX/INT/Critical/DEF)
     */
    public Stats getStats() {
        return stats;
    }

    /**
     * HP를 반환한다.
     *
     * @return HP (현재/최대)
     */
    public Vital getHp() {
        return hp;
    }

    /**
     * MP를 반환한다.
     *
     * @return MP (현재/최대)
     */
    public Vital getMp() {
        return mp;
    }

    /**
     * Stamina를 반환한다.
     *
     * @return Stamina (현재/최대)
     */
    public Vital getStamina() {
        return stamina;
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
     * 현재 맵 노드 id를 갱신한다.
     *
     * @param currentNodeId 새 맵 노드 id
     */
    public void updateCurrentNodeId(final String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
}
