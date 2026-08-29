package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.MaterialItem;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 재료(MATERIAL) 아이템 스택 누적 불변식 프로퍼티 테스트.
 *
 * <p>재료 아이템은 포션과 동일하게 대상 저장소에 동일한 {@code itemId} 행이 존재하면 신규 행을 생성하지 않고 기존 행의 {@code quantity}가 증가해야
 * 한다.
 *
 * <p>Feature: 017-firewood-gathering, Property 2: 장작 스택 누적 불변식
 *
 * <p><b>Validates: Requirements 1.4, 1.5</b>
 */
class MaterialStackPropertyTest {

    private static final String FIREWOOD_ITEM_ID = "firewood";
    private static final MaterialItem FIREWOOD_ITEM = new MaterialItem(FIREWOOD_ITEM_ID, "장작", 20);

    @Property(tries = 100)
    void should_stackIntoExistingRow_when_materialAcquiredWithExistingStack(
            @ForAll("itemQuantity") final int initialQuantity,
            @ForAll("itemQuantity") final int addedQuantity) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository =
                mock(CharacterSkillRepository.class);

        final InventoryService service =
                new InventoryService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression,
                        actionLog,
                        skillCatalogService,
                        characterSkillRepository);

        when(itemCatalogService.byId(FIREWOOD_ITEM_ID)).thenReturn(Optional.of(FIREWOOD_ITEM));

        final OwnedItem existingStack =
                new OwnedItem(
                        1L, FIREWOOD_ITEM_ID, initialQuantity, StorageKind.INVENTORY, false, 0);
        when(ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                        1L, StorageKind.INVENTORY, FIREWOOD_ITEM_ID))
                .thenReturn(Optional.of(existingStack));

        service.acquireItem(1L, FIREWOOD_ITEM_ID, addedQuantity);

        assertThat(existingStack.getQuantity()).isEqualTo(initialQuantity + addedQuantity);
        verify(ownedItemRepository, never()).save(any(OwnedItem.class));
    }

    @Provide
    Arbitrary<Integer> itemQuantity() {
        return Arbitraries.integers().between(1, 50);
    }
}
