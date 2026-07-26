package com.myapps.web.myrpg.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerWeapon;

/**
 * 플레이어 보유 무기 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 무기 인스턴스의 CRUD 및 플레이어별 조회 기능을 제공한다.
 */
public interface PlayerWeaponRepository extends JpaRepository<PlayerWeapon, Long> {

    /**
     * 특정 플레이어가 보유한 모든 무기를 조회한다.
     *
     * @param playerId 플레이어 식별자
     * @return 해당 플레이어의 무기 목록
     */
    List<PlayerWeapon> findByPlayerId(final Long playerId);
}
