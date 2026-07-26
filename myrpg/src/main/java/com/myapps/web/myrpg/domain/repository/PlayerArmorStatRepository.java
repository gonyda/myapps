package com.myapps.web.myrpg.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerArmorStat;

/**
 * 방어구 랜덤 능력치 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 방어구 능력치의 CRUD 및 방어구별 조회 기능을 제공한다.
 */
public interface PlayerArmorStatRepository extends JpaRepository<PlayerArmorStat, Long> {

    /**
     * 특정 방어구의 모든 랜덤 능력치를 조회한다.
     *
     * @param playerArmorId 방어구 인스턴스 식별자
     * @return 해당 방어구의 능력치 목록
     */
    List<PlayerArmorStat> findByPlayerArmorId(final Long playerArmorId);

    /**
     * 특정 방어구의 모든 랜덤 능력치를 삭제한다.
     *
     * @param playerArmorId 방어구 인스턴스 식별자
     */
    void deleteByPlayerArmorId(final Long playerArmorId);
}
