package com.myapps.web.myrpg.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.PlayerInventory;

/**
 * 플레이어 인벤토리(소모품/스킬북) 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 인벤토리 항목의 CRUD 및 플레이어별 조회 기능을 제공한다.
 */
public interface PlayerInventoryRepository extends JpaRepository<PlayerInventory, Long> {

    /**
     * 특정 플레이어의 모든 인벤토리 항목을 조회한다.
     *
     * @param playerId 플레이어 식별자
     * @return 해당 플레이어의 인벤토리 목록
     */
    List<PlayerInventory> findByPlayerId(final Long playerId);

    /**
     * 특정 플레이어의 특정 아이템 종류와 참조 ID에 해당하는 인벤토리 항목을 조회한다.
     *
     * @param playerId  플레이어 식별자
     * @param itemType  아이템 종류
     * @param itemRefId 참조 ID
     * @return 해당 인벤토리 항목 (없으면 empty)
     */
    Optional<PlayerInventory> findByPlayerIdAndItemTypeAndItemRefId(final Long playerId,
                                                                    final ItemType itemType,
                                                                    final Long itemRefId);
}
