package com.myapps.web.myrpg.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;

/**
 * 무기 스킬 장착 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 무기에 장착된 스킬의 CRUD 및 무기별 조회 기능을 제공한다.
 */
public interface PlayerWeaponSkillRepository extends JpaRepository<PlayerWeaponSkill, Long> {

    /**
     * 특정 무기에 장착된 모든 스킬을 조회한다.
     *
     * @param playerWeaponId 무기 인스턴스 식별자
     * @return 해당 무기의 스킬 목록
     */
    List<PlayerWeaponSkill> findByPlayerWeaponId(final Long playerWeaponId);

    /**
     * 특정 무기에 장착된 모든 스킬을 삭제한다.
     *
     * @param playerWeaponId 무기 인스턴스 식별자
     */
    void deleteByPlayerWeaponId(final Long playerWeaponId);
}
