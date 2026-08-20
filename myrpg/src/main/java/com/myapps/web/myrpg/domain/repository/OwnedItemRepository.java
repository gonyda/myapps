package com.myapps.web.myrpg.domain.repository;

import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 보유 아이템 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 저장 위치별 아이템 조회 기능을 제공한다. 인벤토리와 은행을 {@link StorageKind}로 구분하여
 * 조회한다.
 */
public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    /**
     * 지정된 저장 위치의 모든 보유 아이템을 ID 오름차순(획득순)으로 조회한다.
     *
     * @param storage 저장 위치 (INVENTORY 또는 BANK)
     * @return 해당 저장소의 아이템 목록 (없으면 빈 리스트)
     */
    List<OwnedItem> findByStorageOrderById(StorageKind storage);

    /**
     * 지정된 저장 위치에서 특정 아이템 ID를 가진 보유 아이템을 조회한다.
     *
     * <p>소비형(포션) 스택 누적 시 기존 행을 찾기 위해 사용된다.
     *
     * @param storage 저장 위치
     * @param itemId 아이템 카탈로그 ID
     * @return 해당 아이템, 없으면 빈 Optional
     */
    Optional<OwnedItem> findByStorageAndItemId(StorageKind storage, String itemId);

    /**
     * 지정된 저장 위치의 총 항목 수(스택 종류 수)를 반환한다.
     *
     * <p>저장소 용량(30) 검사에 사용된다.
     *
     * @param storage 저장 위치
     * @return 항목 수
     */
    long countByStorage(StorageKind storage);

    /**
     * 지정된 저장 위치에서 장착 중인 장비 목록을 조회한다.
     *
     * <p>착용 규칙 충돌 검사 및 장비 보너스 합산에 사용된다.
     *
     * @param storage 저장 위치
     * @return 장착 중인 아이템 목록 (없으면 빈 리스트)
     */
    List<OwnedItem> findByStorageAndEquippedTrue(StorageKind storage);
}
