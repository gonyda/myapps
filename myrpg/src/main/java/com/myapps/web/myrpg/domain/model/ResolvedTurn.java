package com.myapps.web.myrpg.domain.model;

/**
 * 9칸 매트릭스 기반 전투 턴 해결 결과를 담는 레코드.
 *
 * <p>{@link com.myapps.web.myrpg.domain.service.BattleResolver#resolve(TurnInput)}의 반환 값으로,
 * 양측의 최종 피해와 크리티컬·방어·반격 발생 여부를 포함한다.
 * 선후공 결정은 이 단계에서 처리하지 않으며, 상위 서비스(BattleService)가 담당한다.
 *
 * @param playerDamageToMonster 플레이어가 몬스터에게 가한 최종 피해
 * @param monsterDamageToPlayer 몬스터가 플레이어에게 가한 최종 피해
 * @param playerCritical        플레이어 크리티컬 발동 여부
 * @param monsterCritical       몬스터 크리티컬 발동 여부
 * @param blocked               몬스터가 방어에 성공했는지 (플레이어 일반 vs 몬스터 방어)
 * @param countered             몬스터가 반격했는지 (방어 성공 후 반격 발생)
 */
public record ResolvedTurn(
        int playerDamageToMonster,
        int monsterDamageToPlayer,
        boolean playerCritical,
        boolean monsterCritical,
        boolean blocked,
        boolean countered) {
}
