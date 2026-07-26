package com.myapps.web.myrpg.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.PlayerArmor;

/**
 * 플레이어 보유 방어구 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 방어구 인스턴스의 CRUD 및 플레이어별·부위별 조회 기능을 제공한다.
 */
public interface PlayerArmorRepository extends JpaRepository<PlayerArmor, Long> {

    /**
     * 특정 플레이어가 보유한 모든 방어구를 조회한다.
     *
     * @param playerId 플레이어 식별자
     * @return 해당 플레이어의 방어구 목록
     */
    List<PlayerArmor> findByPlayerId(final Long playerId);

    /**
     * 특정 플레이어의 특정 부위 방어구를 조회한다.
     *
     * @param playerId  플레이어 식별자
     * @param armorSlot 방어구 부위
     * @return 해당 플레이어의 해당 부위 방어구 목록
     */
    List<PlayerArmor> findByPlayerIdAndArmorSlot(final Long playerId, final ArmorSlot armorSlot);
}
