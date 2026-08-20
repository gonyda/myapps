package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 아이템 상세 자동 생성 프로퍼티 테스트.
 *
 * <p>임의의 포션/장비 아이템과 보유 인스턴스에 대해, {@link InventoryService#describe(Item, OwnedItem)}이 포션 회복 문구, 장비
 * 보너스 라인, 내구도, 양손검 배타 안내를 올바르게 포함하는지 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 15: 상세 자동 생성
 *
 * <p><b>Validates: Requirements 12.3, 12.4, 12.5</b>
 */
class ItemDescribePropertyTest {

    private static final int HEAL_HP_MIN = 1;
    private static final int HEAL_HP_MAX = 500;
    private static final int DURABILITY_MIN = 1;
    private static final int DURABILITY_MAX = 100;
    private static final int BONUS_AMOUNT_MIN = 1;
    private static final int BONUS_AMOUNT_MAX = 50;

    private final InventoryService inventoryService;

    ItemDescribePropertyTest() {
        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        this.inventoryService =
                new InventoryService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression,
                        mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                        mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                        mock(
                                com.myapps.web.myrpg.domain.repository.CharacterSkillRepository
                                        .class));
    }

    // Feature: 006-gold-item-inventory, Property 15: 상세 자동 생성

    /**
     * 포션 아이템에 대해 상세 설명에 "생명력을 {healHp} 회복한다." 문구가 포함됨을 검증한다.
     *
     * @param healHp 임의 생성된 HP 회복량
     */
    @Property(tries = 100)
    void should_containHealHpDescription_when_potionItem(@ForAll("healHpValues") final int healHp) {

        final PotionItem potion = new PotionItem("test_potion", "테스트 포션", healHp, null);
        final OwnedItem owned = new OwnedItem("test_potion", 3, StorageKind.INVENTORY, false, 0);

        final List<String> lines = inventoryService.describe(potion, owned);

        assertThat(lines).anyMatch(line -> line.contains("생명력을 " + healHp + " 회복한다."));
    }

    /**
     * 장비 아이템에 대해 상세 설명에 각 보너스 라인이 포함됨을 검증한다.
     *
     * @param equipData 임의 생성된 장비 데이터(kind, bonuses, maxDurability, currentDurability)
     */
    @Property(tries = 100)
    void should_containBonusLines_when_equipmentItem(
            @ForAll("equipmentWithBonuses") final EquipTestData equipData) {

        final EquipmentItem equipment =
                new EquipmentItem(
                        "test_equip",
                        "테스트 장비",
                        equipData.kind() == EquipmentKind.SHIELD
                                        || equipData.kind() == EquipmentKind.ARMOR_BODY
                                ? ItemType.ARMOR
                                : ItemType.WEAPON,
                        equipData.kind(),
                        equipData.bonuses(),
                        null,
                        equipData.maxDurability());

        final OwnedItem owned =
                new OwnedItem(
                        "test_equip",
                        1,
                        StorageKind.INVENTORY,
                        false,
                        equipData.currentDurability());

        final List<String> lines = inventoryService.describe(equipment, owned);

        for (final EquipBonus bonus : equipData.bonuses()) {
            final String targetLabel = bonusTargetLabel(bonus.target());
            if (bonus.target() == BonusTarget.CRITICAL) {
                final double percent = bonus.amount() * 0.1;
                assertThat(lines)
                        .anyMatch(
                                line ->
                                        line.contains(targetLabel)
                                                && line.contains("+" + percent + "%"));
            } else {
                assertThat(lines)
                        .anyMatch(
                                line ->
                                        line.contains(targetLabel)
                                                && line.contains("+" + bonus.amount()));
            }
        }
    }

    /**
     * 장비 아이템에 대해 상세 설명에 내구도 표시({현재}/{최대})가 포함됨을 검증한다.
     *
     * @param equipData 임의 생성된 장비 데이터
     */
    @Property(tries = 100)
    void should_containDurabilityLine_when_equipmentItem(
            @ForAll("equipmentWithBonuses") final EquipTestData equipData) {

        final EquipmentItem equipment =
                new EquipmentItem(
                        "test_equip",
                        "테스트 장비",
                        equipData.kind() == EquipmentKind.SHIELD
                                        || equipData.kind() == EquipmentKind.ARMOR_BODY
                                ? ItemType.ARMOR
                                : ItemType.WEAPON,
                        equipData.kind(),
                        equipData.bonuses(),
                        null,
                        equipData.maxDurability());

        final OwnedItem owned =
                new OwnedItem(
                        "test_equip",
                        1,
                        StorageKind.INVENTORY,
                        false,
                        equipData.currentDurability());

        final List<String> lines = inventoryService.describe(equipment, owned);

        assertThat(lines)
                .anyMatch(
                        line ->
                                line.contains("내구도:")
                                        && line.contains("/" + equipData.maxDurability()));
    }

    /**
     * 양손검(TWO_HANDED_SWORD) 아이템에 대해 방패 배타 안내가 포함됨을 검증한다.
     *
     * @param equipData 양손검 장비 데이터
     */
    @Property(tries = 100)
    void should_containExclusivityWarning_when_twoHandedSword(
            @ForAll("twoHandedSwordData") final EquipTestData equipData) {

        final EquipmentItem equipment =
                new EquipmentItem(
                        "test_two_hand",
                        "테스트 양손검",
                        ItemType.WEAPON,
                        EquipmentKind.TWO_HANDED_SWORD,
                        equipData.bonuses(),
                        null,
                        equipData.maxDurability());

        final OwnedItem owned =
                new OwnedItem(
                        "test_two_hand",
                        1,
                        StorageKind.INVENTORY,
                        false,
                        equipData.currentDurability());

        final List<String> lines = inventoryService.describe(equipment, owned);

        assertThat(lines).anyMatch(line -> line.contains("방패와 함께 착용할 수 없습니다."));
    }

    /**
     * 양손검이 아닌 장비에 대해 방패 배타 안내가 포함되지 않음을 검증한다.
     *
     * @param equipData 양손검이 아닌 장비 데이터
     */
    @Property(tries = 100)
    void should_notContainExclusivityWarning_when_notTwoHandedSword(
            @ForAll("nonTwoHandedSwordData") final EquipTestData equipData) {

        final ItemType itemType =
                equipData.kind() == EquipmentKind.SHIELD
                                || equipData.kind() == EquipmentKind.ARMOR_BODY
                        ? ItemType.ARMOR
                        : ItemType.WEAPON;

        final EquipmentItem equipment =
                new EquipmentItem(
                        "test_equip",
                        "테스트 장비",
                        itemType,
                        equipData.kind(),
                        equipData.bonuses(),
                        null,
                        equipData.maxDurability());

        final OwnedItem owned =
                new OwnedItem(
                        "test_equip",
                        1,
                        StorageKind.INVENTORY,
                        false,
                        equipData.currentDurability());

        final List<String> lines = inventoryService.describe(equipment, owned);

        assertThat(lines).noneMatch(line -> line.contains("방패와 함께 착용할 수 없습니다."));
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 포션 HP 회복량 값을 생성하는 Arbitrary 제공자.
     *
     * @return healHp ∈ [1, 500]의 Arbitrary
     */
    @Provide
    Arbitrary<Integer> healHpValues() {
        return Arbitraries.integers().between(HEAL_HP_MIN, HEAL_HP_MAX);
    }

    /**
     * 장비 데이터(종류, 보너스 목록, 최대 내구도, 현재 내구도)를 생성하는 Arbitrary 제공자.
     *
     * @return 임의 EquipTestData의 Arbitrary
     */
    @Provide
    Arbitrary<EquipTestData> equipmentWithBonuses() {
        final Arbitrary<EquipmentKind> kindArb = Arbitraries.of(EquipmentKind.values());
        final Arbitrary<List<EquipBonus>> bonusesArb = bonusList();
        final Arbitrary<Integer> maxDurArb =
                Arbitraries.integers().between(DURABILITY_MIN, DURABILITY_MAX);

        return Combinators.combine(kindArb, bonusesArb, maxDurArb)
                .flatAs(
                        (kind, bonuses, maxDur) ->
                                Arbitraries.doubles()
                                        .between(0.0, (double) maxDur)
                                        .map(
                                                curDur ->
                                                        new EquipTestData(
                                                                kind, bonuses, maxDur, curDur)));
    }

    /**
     * 양손검 장비 데이터를 생성하는 Arbitrary 제공자.
     *
     * @return TWO_HANDED_SWORD EquipTestData의 Arbitrary
     */
    @Provide
    Arbitrary<EquipTestData> twoHandedSwordData() {
        final Arbitrary<List<EquipBonus>> bonusesArb = bonusList();
        final Arbitrary<Integer> maxDurArb =
                Arbitraries.integers().between(DURABILITY_MIN, DURABILITY_MAX);

        return Combinators.combine(bonusesArb, maxDurArb)
                .flatAs(
                        (bonuses, maxDur) ->
                                Arbitraries.doubles()
                                        .between(0.0, (double) maxDur)
                                        .map(
                                                curDur ->
                                                        new EquipTestData(
                                                                EquipmentKind.TWO_HANDED_SWORD,
                                                                bonuses,
                                                                maxDur,
                                                                curDur)));
    }

    /**
     * 양손검이 아닌 장비 데이터를 생성하는 Arbitrary 제공자.
     *
     * @return ONE_HANDED_SWORD/SHIELD/ARMOR_BODY EquipTestData의 Arbitrary
     */
    @Provide
    Arbitrary<EquipTestData> nonTwoHandedSwordData() {
        final Arbitrary<EquipmentKind> kindArb =
                Arbitraries.of(
                        EquipmentKind.ONE_HANDED_SWORD,
                        EquipmentKind.SHIELD,
                        EquipmentKind.ARMOR_BODY);
        final Arbitrary<List<EquipBonus>> bonusesArb = bonusList();
        final Arbitrary<Integer> maxDurArb =
                Arbitraries.integers().between(DURABILITY_MIN, DURABILITY_MAX);

        return Combinators.combine(kindArb, bonusesArb, maxDurArb)
                .flatAs(
                        (kind, bonuses, maxDur) ->
                                Arbitraries.doubles()
                                        .between(0.0, (double) maxDur)
                                        .map(
                                                curDur ->
                                                        new EquipTestData(
                                                                kind, bonuses, maxDur, curDur)));
    }

    // ─── Helper Arbitraries ─────────────────────────────────────────────────

    /**
     * 보너스 목록(1~3개)을 생성하는 Arbitrary.
     *
     * @return EquipBonus 리스트의 Arbitrary
     */
    private Arbitrary<List<EquipBonus>> bonusList() {
        final Arbitrary<EquipBonus> singleBonus =
                Combinators.combine(
                                Arbitraries.of(BonusTarget.values()),
                                Arbitraries.integers().between(BONUS_AMOUNT_MIN, BONUS_AMOUNT_MAX))
                        .as(EquipBonus::new);
        return singleBonus.list().ofMinSize(1).ofMaxSize(3);
    }

    /**
     * BonusTarget을 한글 라벨로 변환한다.
     *
     * @param target 보너스 대상
     * @return 한글 라벨 문자열
     */
    private static String bonusTargetLabel(final BonusTarget target) {
        if (target == BonusTarget.STR) {
            return "STR";
        } else if (target == BonusTarget.DEX) {
            return "DEX";
        } else if (target == BonusTarget.INT) {
            return "INT";
        } else if (target == BonusTarget.CRITICAL) {
            return "CRIT";
        } else if (target == BonusTarget.DEF) {
            return "DEF";
        } else if (target == BonusTarget.HP) {
            return "HP";
        } else if (target == BonusTarget.MP) {
            return "MP";
        } else if (target == BonusTarget.STAMINA) {
            return "Stamina";
        }
        return target.name();
    }

    // ─── Test Data ──────────────────────────────────────────────────────────

    /**
     * 장비 테스트 데이터를 담는 레코드.
     *
     * @param kind 장비 종류
     * @param bonuses 보너스 목록
     * @param maxDurability 최대 내구도
     * @param currentDurability 현재 내구도
     */
    record EquipTestData(
            EquipmentKind kind,
            List<EquipBonus> bonuses,
            int maxDurability,
            double currentDurability) {}
}
