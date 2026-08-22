package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * 몬스터 카탈로그 항목을 나타내는 불변 레코드.
 *
 * <p>{@code classpath:data/monster.json}에서 로드되어 메모리에 보관되며, 몬스터의 식별자, 이름, 유형, 스탯, 경험치, 드랍 정보, 조우 대사를
 * 포함한다. 영속 대상이 아니며 전투 중 현재 HP는 본 레코드에 포함하지 않는다.
 *
 * <p>{@code defenseBlockRate}와 {@code defenseCounterRate}는 몬스터가 방어(DEFENSE) 행동을 선택했을 때 적용되는 경감률(%)과
 * 반격율(%)이다. 몬스터별 오버라이드가 가능하며, 미지정 시 전역 기본값(100% 완전 방어 / 0 반격)을 사용한다. 두 필드 모두 %이므로 레벨과 무관하게 자동
 * 스케일된다.
 *
 * @param id 몬스터 고유 식별자 (예: "raccoon")
 * @param name 몬스터 표시 이름 (예: "너구리")
 * @param type 몬스터 유형 ({@link MonsterType} 열거 상수)
 * @param level 몬스터 레벨 (1 이상)
 * @param maxHp 최대 체력 (1 이상)
 * @param attackPower 공격력 (근접·마법 구분 없는 단일 값)
 * @param defense 방어력
 * @param critical 크리티컬 확률 (0.1% 단위 정수, 10 = 1.0%)
 * @param experience 처치 시 획득 경험치
 * @param goldDrop 골드 드랍 범위 (필수)
 * @param itemDrops 아이템 드랍 테이블 (빈 목록 가능)
 * @param lines 조우 대사 목록 (정확히 3개: 소리 1 + 행동 묘사 2)
 * @param defenseBlockRate 방어 경감률 (%, 기본 100). 몬스터가 방어 시 공격자 피해를 이 비율만큼 경감 (100 = 0 피격)
 * @param defenseCounterRate 방어 반격율 (%, 기본 0). 몬스터가 방어 시 attackPower × 이 비율로 반격 (0 = 반격 없음)
 */
public record Monster(
        String id,
        String name,
        MonsterType type,
        int level,
        int maxHp,
        int attackPower,
        int defense,
        int critical,
        long experience,
        GoldDrop goldDrop,
        List<ItemDrop> itemDrops,
        List<String> lines,
        int defenseBlockRate,
        int defenseCounterRate) {

    private static final int DEFAULT_DEFENSE_BLOCK_RATE = 100;
    private static final int DEFAULT_DEFENSE_COUNTER_RATE = 0;

    /**
     * 방어 상수를 전역 기본값(100% 완전 방어 / 0 반격)으로 생성하는 보조 생성자.
     *
     * <p>기존 12필드 호출부와의 하위 호환을 유지하기 위해 제공된다. {@code monster.json}에서 두 필드가 미지정된 경우 이 생성자를 통해 인스턴스를
     * 생성한다.
     *
     * @param id 몬스터 고유 식별자
     * @param name 몬스터 표시 이름
     * @param type 몬스터 유형
     * @param level 레벨
     * @param maxHp 최대 체력
     * @param attackPower 공격력
     * @param defense 방어력
     * @param critical 크리티컬 확률
     * @param experience 처치 시 획득 경험치
     * @param goldDrop 골드 드랍 범위
     * @param itemDrops 아이템 드랍 테이블
     * @param lines 조우 대사 목록
     */
    public Monster(
            final String id,
            final String name,
            final MonsterType type,
            final int level,
            final int maxHp,
            final int attackPower,
            final int defense,
            final int critical,
            final long experience,
            final GoldDrop goldDrop,
            final List<ItemDrop> itemDrops,
            final List<String> lines) {
        this(
                id,
                name,
                type,
                level,
                maxHp,
                attackPower,
                defense,
                critical,
                experience,
                goldDrop,
                itemDrops,
                lines,
                DEFAULT_DEFENSE_BLOCK_RATE,
                DEFAULT_DEFENSE_COUNTER_RATE);
    }

    /**
     * 상호작용 버튼에 표시할 라벨을 반환한다.
     *
     * <p>일반 몬스터(배지가 빈 문자열)는 이름만 반환하고, 보스 몬스터(배지가 "👑")는 이름 뒤에 공백과 배지를 붙여 반환한다.
     *
     * @return 버튼 라벨 문자열 (예: "너구리", "너구리왕 👑")
     */
    public String buttonLabel() {
        if (type.badge().isBlank()) {
            return name;
        }
        return name + " " + type.badge();
    }
}
