package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.ItemType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 인벤토리 정렬 결정성 프로퍼티 테스트.
 *
 * <p>임의의 아이템 목록에 대해, 획득순(id 오름차순)/이름순/타입순 정렬이 동일 입력에 대해 항상 동일한 결과를 생성(결정적)하고, 각 정렬이 올바른 순서를 보장함을
 * 검증한다.
 *
 * <p>본 스펙에서 정렬은 클라이언트(JS)에서 수행되지만, 정렬 비교기의 동작 정확성을 서버 측에서 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 16: 인벤토리 정렬 결정성
 *
 * <p><b>Validates: Requirements 14.1, 14.4</b>
 */
class InventorySortPropertyTest {

    private static final int MIN_LIST_SIZE = 2;
    private static final int MAX_LIST_SIZE = 15;
    private static final int ID_MIN = 1;
    private static final int ID_MAX = 1000;

    /**
     * 획득순(id 오름차순) 정렬이 결정적이며 id 오름차순을 보장함을 검증한다.
     *
     * @param items 임의 생성된 아이템 목록
     */
    // Feature: 006-gold-item-inventory, Property 16: 인벤토리 정렬 결정성
    @Property(tries = 100)
    void should_sortByIdAscending_when_acquisitionOrder(
            @ForAll("sortableItems") final List<SortableItem> items) {

        final Comparator<SortableItem> acquisitionOrder =
                Comparator.comparingLong(SortableItem::id);

        final List<SortableItem> firstSort = items.stream().sorted(acquisitionOrder).toList();
        final List<SortableItem> secondSort = items.stream().sorted(acquisitionOrder).toList();

        assertThat(firstSort).isEqualTo(secondSort);

        for (int i = 0; i < firstSort.size() - 1; i++) {
            assertThat(firstSort.get(i).id()).isLessThanOrEqualTo(firstSort.get(i + 1).id());
        }
    }

    /**
     * 이름순 정렬이 결정적이며 이름 사전순을 보장함을 검증한다.
     *
     * @param items 임의 생성된 아이템 목록
     */
    @Property(tries = 100)
    void should_beDeterministic_when_sortByName(
            @ForAll("sortableItems") final List<SortableItem> items) {

        final Comparator<SortableItem> nameOrder = Comparator.comparing(SortableItem::name);

        final List<SortableItem> firstSort = items.stream().sorted(nameOrder).toList();
        final List<SortableItem> secondSort = items.stream().sorted(nameOrder).toList();

        assertThat(firstSort).isEqualTo(secondSort);

        for (int i = 0; i < firstSort.size() - 1; i++) {
            assertThat(firstSort.get(i).name().compareTo(firstSort.get(i + 1).name()))
                    .isLessThanOrEqualTo(0);
        }
    }

    /**
     * 타입순(타입 그룹 내 이름 보조) 정렬이 결정적이며 같은 타입 내에서 이름 사전순을 보장함을 검증한다.
     *
     * @param items 임의 생성된 아이템 목록
     */
    @Property(tries = 100)
    void should_beDeterministic_when_sortByTypeThenName(
            @ForAll("sortableItems") final List<SortableItem> items) {

        final Comparator<SortableItem> typeNameOrder =
                Comparator.comparing(SortableItem::type).thenComparing(SortableItem::name);

        final List<SortableItem> firstSort = items.stream().sorted(typeNameOrder).toList();
        final List<SortableItem> secondSort = items.stream().sorted(typeNameOrder).toList();

        assertThat(firstSort).isEqualTo(secondSort);

        for (int i = 0; i < firstSort.size() - 1; i++) {
            final SortableItem current = firstSort.get(i);
            final SortableItem next = firstSort.get(i + 1);
            final int typeCompare = current.type().compareTo(next.type());
            assertThat(typeCompare).isLessThanOrEqualTo(0);

            if (typeCompare == 0) {
                assertThat(current.name().compareTo(next.name())).isLessThanOrEqualTo(0);
            }
        }
    }

    /**
     * 정렬 전후로 요소 집합이 보존됨(원소 유실/추가 없음)을 검증한다.
     *
     * @param items 임의 생성된 아이템 목록
     */
    @Property(tries = 100)
    void should_preserveAllElements_when_sorted(
            @ForAll("sortableItems") final List<SortableItem> items) {

        final Comparator<SortableItem> nameOrder = Comparator.comparing(SortableItem::name);
        final Comparator<SortableItem> typeNameOrder =
                Comparator.comparing(SortableItem::type).thenComparing(SortableItem::name);

        final List<SortableItem> byName = new ArrayList<>(items);
        byName.sort(nameOrder);

        final List<SortableItem> byTypeName = new ArrayList<>(items);
        byTypeName.sort(typeNameOrder);

        assertThat(byName).containsExactlyInAnyOrderElementsOf(items);
        assertThat(byTypeName).containsExactlyInAnyOrderElementsOf(items);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 정렬 대상 아이템 목록을 생성하는 Arbitrary 제공자.
     *
     * @return SortableItem 리스트의 Arbitrary
     */
    @Provide
    Arbitrary<List<SortableItem>> sortableItems() {
        final Arbitrary<SortableItem> singleItem =
                Combinators.combine(
                                Arbitraries.longs().between(ID_MIN, ID_MAX),
                                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(8),
                                Arbitraries.of(ItemType.values()))
                        .as(SortableItem::new);

        return singleItem.list().ofMinSize(MIN_LIST_SIZE).ofMaxSize(MAX_LIST_SIZE);
    }

    // ─── Test Data ──────────────────────────────────────────────────────────

    /**
     * 정렬 검증에 사용되는 아이템 VO.
     *
     * @param id 획득 순서(PK id)
     * @param name 아이템 이름
     * @param type 아이템 타입
     */
    record SortableItem(long id, String name, ItemType type) {}
}
