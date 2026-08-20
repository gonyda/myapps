package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 포션 사용 회복·수량 프로퍼티 테스트.
 *
 * <p>임의의 HP 상태와 포션에 대해, {@code usePotion}은 {@code hpCurrent = min(hpCurrent + healHp, hpMax)}로 HP를
 * 회복하고 {@code quantity}를 1 감소시키며, 0이 되면 행을 제거한다. HP는 hpMax를 초과하지 않는다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 13: 포션 사용 회복·수량
 *
 * <p><b>Validates: Requirements 11.2, 11.3</b>
 */
class UsePotionPropertyTest {

    private static final long OWNED_ITEM_ID = 1L;
    private static final int MAX_HEAL_HP = 200;
    private static final int MAX_HP_CURRENT = 500;
    private static final int MIN_QUANTITY = 2;
    private static final int MAX_QUANTITY = 99;

    // Feature: 006-gold-item-inventory, Property 13: 포션 사용 회복·수량

    /**
     * 포션 사용 후 HP가 min(hpCurrent + healHp, hpMax)로 클램프됨을 검증한다.
     *
     * @param params 임의 생성된 포션 사용 파라미터
     */
    @Property(tries = 100)
    void should_clampHpToMax_when_potionUsed(
            @ForAll("potionWithQuantityAboveOne") final PotionUseParams params) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService =
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

        final OwnedItem potionOwned =
                new OwnedItem(
                        params.potionId(), params.quantity(), StorageKind.INVENTORY, false, 0);

        final PotionItem potionItem =
                new PotionItem(params.potionId(), "테스트 포션", params.healHp(), 30);

        final CharacterProgress character = createCharacterWithHp(params.hpCurrent());

        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(potionOwned));
        when(itemCatalogService.byId(params.potionId())).thenReturn(Optional.of(potionItem));
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(character));
        // equippedBonus 호출 시 장착 장비 없음
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.usePotion(OWNED_ITEM_ID);

        final int hpMax = computeHpMax(character);
        final int expectedHp = Math.min(params.hpCurrent() + params.healHp(), hpMax);
        assertThat(character.getHpCurrent()).isEqualTo(expectedHp);
    }

    /**
     * 포션 사용 후 수량이 1 감소함을 검증한다(수량 > 1인 경우).
     *
     * @param params 임의 생성된 포션 사용 파라미터
     */
    @Property(tries = 100)
    void should_decreaseQuantityByOne_when_potionUsed(
            @ForAll("potionWithQuantityAboveOne") final PotionUseParams params) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService =
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

        final OwnedItem potionOwned =
                new OwnedItem(
                        params.potionId(), params.quantity(), StorageKind.INVENTORY, false, 0);

        final PotionItem potionItem =
                new PotionItem(params.potionId(), "테스트 포션", params.healHp(), 30);

        final CharacterProgress character = createCharacterWithHp(params.hpCurrent());

        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(potionOwned));
        when(itemCatalogService.byId(params.potionId())).thenReturn(Optional.of(potionItem));
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(character));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final int originalQuantity = potionOwned.getQuantity();

        inventoryService.usePotion(OWNED_ITEM_ID);

        assertThat(potionOwned.getQuantity()).isEqualTo(originalQuantity - 1);
        verify(ownedItemRepository, never()).delete(potionOwned);
    }

    /**
     * 포션 수량이 1일 때 사용하면 행이 삭제됨을 검증한다.
     *
     * @param params 임의 생성된 포션 사용 파라미터 (수량 1)
     */
    @Property(tries = 100)
    void should_deleteItem_when_quantityReachesZero(
            @ForAll("potionWithQuantityOne") final PotionUseParams params) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService =
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

        final OwnedItem potionOwned =
                new OwnedItem(params.potionId(), 1, StorageKind.INVENTORY, false, 0);

        final PotionItem potionItem =
                new PotionItem(params.potionId(), "테스트 포션", params.healHp(), 30);

        final CharacterProgress character = createCharacterWithHp(params.hpCurrent());

        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(potionOwned));
        when(itemCatalogService.byId(params.potionId())).thenReturn(Optional.of(potionItem));
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(character));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.usePotion(OWNED_ITEM_ID);

        assertThat(potionOwned.getQuantity()).isZero();
        verify(ownedItemRepository).delete(potionOwned);
    }

    /**
     * HP가 이미 최대치 이상인 경우에도 hpMax를 초과하지 않음을 검증한다.
     *
     * @param healHp 임의 회복량
     */
    @Property(tries = 100)
    void should_neverExceedHpMax_when_alreadyAtMax(@ForAll("healHpAmount") final int healHp) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService =
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

        final String potionId = "full_hp_potion";
        // HP를 hpMax(레벨 1 기본 100)와 동일하게 설정
        final int hpMax = 100;
        final CharacterProgress character = createCharacterWithHp(hpMax);

        final OwnedItem potionOwned = new OwnedItem(potionId, 5, StorageKind.INVENTORY, false, 0);
        final PotionItem potionItem = new PotionItem(potionId, "큰 포션", healHp, 50);

        when(ownedItemRepository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(potionOwned));
        when(itemCatalogService.byId(potionId)).thenReturn(Optional.of(potionItem));
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(character));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.usePotion(OWNED_ITEM_ID);

        assertThat(character.getHpCurrent()).isLessThanOrEqualTo(hpMax);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 수량이 2 이상인 포션 사용 파라미터를 생성한다.
     *
     * @return PotionUseParams Arbitrary
     */
    @Provide
    Arbitrary<PotionUseParams> potionWithQuantityAboveOne() {
        return Combinators.combine(
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.integers().between(1, MAX_HEAL_HP),
                        Arbitraries.integers().between(1, MAX_HP_CURRENT),
                        Arbitraries.integers().between(MIN_QUANTITY, MAX_QUANTITY))
                .as(PotionUseParams::new);
    }

    /**
     * 수량이 1인 포션 사용 파라미터를 생성한다.
     *
     * @return PotionUseParams Arbitrary
     */
    @Provide
    Arbitrary<PotionUseParams> potionWithQuantityOne() {
        return Combinators.combine(
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.integers().between(1, MAX_HEAL_HP),
                        Arbitraries.integers().between(1, MAX_HP_CURRENT),
                        Arbitraries.just(1))
                .as(PotionUseParams::new);
    }

    /**
     * 회복량(healHp) Arbitrary를 생성한다.
     *
     * @return healHp 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> healHpAmount() {
        return Arbitraries.integers().between(1, MAX_HEAL_HP);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * 지정된 HP 현재값을 가진 캐릭터를 생성한다. 레벨 1, 재능 MELEE → vitalMaxFor(1, MELEE).hp() = 100 (장비 보너스 없음).
     *
     * @param hpCurrent 설정할 HP 현재값
     * @return CharacterProgress 인스턴스
     */
    private CharacterProgress createCharacterWithHp(final int hpCurrent) {
        return new CharacterProgress(
                "테스트",
                1,
                1,
                0L,
                TalentType.MELEE,
                null,
                hpCurrent,
                100,
                100,
                "tir-chonaill",
                0,
                0L);
    }

    /**
     * 레벨 1, MELEE 재능 기준 hpMax를 계산한다 (장비 보너스 0).
     *
     * @param character 대상 캐릭터
     * @return 계산된 HP 최대치
     */
    private int computeHpMax(final CharacterProgress character) {
        final StatProgression statProgression = new StatProgression();
        final VitalMax vitalMax =
                statProgression.vitalMaxFor(character.getCurrentLevel(), character.getTalent());
        // 장비 보너스 없음(테스트에서 장착 장비 없도록 모킹)
        return vitalMax.hp();
    }

    // ─── Inner types ────────────────────────────────────────────────────────

    /**
     * 포션 사용 테스트 파라미터.
     *
     * @param potionId 포션 아이템 ID
     * @param healHp 회복량
     * @param hpCurrent 현재 HP
     * @param quantity 보유 수량
     */
    private record PotionUseParams(String potionId, int healHp, int hpCurrent, int quantity) {}
}
