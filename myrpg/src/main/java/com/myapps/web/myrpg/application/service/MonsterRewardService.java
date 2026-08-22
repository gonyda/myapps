package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.ItemDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * 몬스터 처치 보상(골드·아이템) 드랍 계산 서비스.
 *
 * <p>골드는 {@link GoldDrop}의 {@code [min, max]} 범위에서 결정적 공식으로 산출하고, 아이템은 각 {@link ItemDrop}의 확률 판정을
 * 거쳐 수량 범위 내에서 추첨한다. 순수 계산만 수행하며, 실제 지급(골드 가산·인벤토리 적재)은 이 서비스의 책임 밖이다.
 */
@Service
public class MonsterRewardService {

    private static final int PERCENT_BOUND = 100;

    private final Random random;

    /**
     * MonsterRewardService를 생성합니다.
     *
     * @param random 드랍 판정에 사용할 Random (테스트 시 시드 고정 가능)
     */
    public MonsterRewardService(final Random random) {
        this.random = random;
    }

    /**
     * 골드 드랍 범위와 주어진 roll 값으로 확정 골드 금액을 산출한다.
     *
     * <p>순수 함수로서, 동일 입력에 대해 항상 동일 결과를 반환한다. 결과는 항상 {@code [goldDrop.min(), goldDrop.max()]} 범위에
     * 속한다.
     *
     * @param goldDrop 골드 드랍 범위 (min ≤ max 보장)
     * @param roll 무작위 값 (0 이상)
     * @return 확정 골드 금액
     */
    public long goldFor(final GoldDrop goldDrop, final int roll) {
        final int range = goldDrop.max() - goldDrop.min() + 1;
        return goldDrop.min() + (Math.abs(roll) % range);
    }

    /**
     * 몬스터의 드랍 테이블을 추첨하여 결과만 반환한다.
     *
     * <p>골드는 필수로 산출되며, 각 아이템 드랍은 {@code chancePercent} 확률 판정을 통과한 경우에만 {@code [minQuantity,
     * maxQuantity]} 범위에서 수량이 결정된다.
     *
     * <p>이 메서드는 드랍 테이블을 추첨하여 결과만 반환한다. 실제 골드 가산({@code CharacterProgress.gainGold})과 인벤토리
     * 적재(OwnedItem 생성/수량 증가)는 6순위(전투 완료 후) 구현에서 이 결과를 소비하여 처리한다. 6순위에서는 인벤토리 획득 API를 신설하고 이 메서드의
     * 반환값을 입력으로 전달한다.
     *
     * @param monster 드랍을 계산할 몬스터
     * @return 골드(필수) + 아이템(0개 이상)을 담은 드랍 결과
     */
    public DropResult rollDrop(final Monster monster) {
        final long gold = goldFor(monster.goldDrop(), random.nextInt(Integer.MAX_VALUE));
        final List<DroppedItem> items = rollItemDrops(monster.itemDrops());
        return new DropResult(gold, items);
    }

    /**
     * 아이템 드랍 목록을 확률 추첨하여 획득된 아이템 목록을 반환한다.
     *
     * @param itemDrops 드랍 대상 아이템 확률/수량 목록 (null 또는 빈 목록 가능)
     * @return 추첨 결과 획득된 아이템 목록
     */
    public List<DroppedItem> rollItemDrops(final List<ItemDrop> itemDrops) {
        if (itemDrops == null || itemDrops.isEmpty()) {
            return List.of();
        }

        final List<DroppedItem> items = new ArrayList<>();
        for (final ItemDrop itemDrop : itemDrops) {
            final int chanceRoll = random.nextInt(PERCENT_BOUND);
            if (chanceRoll < itemDrop.chancePercent()) {
                final int quantity = calculateQuantity(itemDrop);
                items.add(new DroppedItem(itemDrop.itemId(), quantity));
            }
        }

        return List.copyOf(items);
    }

    /**
     * 아이템 드랍의 수량을 범위 내에서 결정한다.
     *
     * @param itemDrop 수량 범위를 포함하는 아이템 드랍 정보
     * @return 확정된 드랍 수량
     */
    private int calculateQuantity(final ItemDrop itemDrop) {
        if (itemDrop.minQuantity() == itemDrop.maxQuantity()) {
            return itemDrop.minQuantity();
        }
        final int quantityRange = itemDrop.maxQuantity() - itemDrop.minQuantity() + 1;
        return itemDrop.minQuantity() + random.nextInt(quantityRange);
    }
}
