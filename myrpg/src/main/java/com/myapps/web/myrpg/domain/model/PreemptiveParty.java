package com.myapps.web.myrpg.domain.model;

/**
 * 다음 턴 확정 선제공격(Preemptive Strike)을 행사할 주체를 정의하는 열거형.
 *
 * <p>디펜스 스킬로 적의 일반 공격을 성공적으로 막아낸 쪽이 다음 턴에 확정 선제 공격권을 획득한다.
 *
 * <ul>
 *   <li>{@link #NONE}: 선제권 없음 (통상 공방 사이클)
 *   <li>{@link #PLAYER}: 플레이어 확정 선제 공격 (몬스터 경직, 100% 일방 선제타)
 *   <li>{@link #MONSTER}: 몬스터 확정 선제 일반공격 (플레이어 자세 붕괴, 몬스터 일반공격 100% 일방 선제타)
 * </ul>
 */
public enum PreemptiveParty {

    /** 선제 공격권 없음 (통상 턴 진행). */
    NONE("없음"),

    /** 플레이어 선제 공격권 (디펜스 성공 후딜 몬스터 경직). */
    PLAYER("플레이어"),

    /** 몬스터 선제 공격권 (몬스터 디펜스에 유저 공격 튕김). */
    MONSTER("몬스터");

    private final String label;

    PreemptiveParty(final String label) {
        this.label = label;
    }

    /**
     * 선제권 주체의 한글 표시명을 반환한다.
     *
     * @return 표시용 라벨 문자열
     */
    public String label() {
        return label;
    }
}
