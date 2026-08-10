package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * 몬스터 카탈로그 항목을 나타내는 불변 레코드.
 *
 * <p>{@code classpath:data/monster.json}에서 로드되어 메모리에 보관되며,
 * 몬스터의 식별자, 이름, 유형, 스탯, 경험치, 드랍 정보, 조우 대사를 포함한다.
 * 영속 대상이 아니며 전투 중 현재 HP는 본 레코드에 포함하지 않는다.
 *
 * @param id          몬스터 고유 식별자 (예: "raccoon")
 * @param name        몬스터 표시 이름 (예: "너구리")
 * @param type        몬스터 유형 ({@link MonsterType} 열거 상수)
 * @param level       몬스터 레벨 (1 이상)
 * @param maxHp       최대 체력 (1 이상)
 * @param attackPower 공격력 (근접·마법 구분 없는 단일 값)
 * @param defense     방어력
 * @param critical    크리티컬 확률 (0.1% 단위 정수, 10 = 1.0%)
 * @param experience  처치 시 획득 경험치
 * @param goldDrop    골드 드랍 범위 (필수)
 * @param itemDrops   아이템 드랍 테이블 (빈 목록 가능)
 * @param lines       조우 대사 목록 (정확히 3개: 소리 1 + 행동 묘사 2)
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
        List<String> lines
) {

    /**
     * 상호작용 버튼에 표시할 라벨을 반환한다.
     *
     * <p>일반 몬스터(배지가 빈 문자열)는 이름만 반환하고,
     * 보스 몬스터(배지가 "👑")는 이름 뒤에 공백과 배지를 붙여 반환한다.
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
