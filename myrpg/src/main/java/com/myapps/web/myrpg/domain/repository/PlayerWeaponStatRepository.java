package com.myapps.web.myrpg.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;

/**
 * 무기 랜덤 능력치 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 무기 능력치의 CRUD 및 무기별 조회 기능을 제공한다.
 */
public interface PlayerWeaponStatRepository extends JpaRepository<PlayerWeaponStat, Long> {

    /**
     * 특정 무기의 모든 랜덤 능력치를 조회한다.
     *
     * @param playerWeaponId 무기 인스턴스 식별자
     * @return 해당 무기의 능력치 목록
     */
    List<PlayerWeaponStat> findByPlayerWeaponId(final Long playerWeaponId);

    /**
     * 특정 무기의 모든 랜덤 능력치를 삭제한다.
     *
     * @param playerWeaponId 무기 인스턴스 식별자
     */
    void deleteByPlayerWeaponId(final Long playerWeaponId);
}
