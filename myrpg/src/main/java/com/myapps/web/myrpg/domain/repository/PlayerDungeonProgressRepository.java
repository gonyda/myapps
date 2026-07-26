package com.myapps.web.myrpg.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerDungeonProgress;

/**
 * 던전 클리어 이력 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 던전 진행 이력의 CRUD 및 플레이어별·던전별 조회 기능을 제공한다.
 */
public interface PlayerDungeonProgressRepository extends JpaRepository<PlayerDungeonProgress, Long> {

    /**
     * 특정 플레이어의 모든 던전 진행 이력을 조회한다.
     *
     * @param playerId 플레이어 식별자
     * @return 해당 플레이어의 던전 진행 이력 목록
     */
    List<PlayerDungeonProgress> findByPlayerId(final Long playerId);

    /**
     * 특정 플레이어의 특정 던전에 대한 진행 이력을 조회한다.
     *
     * @param playerId  플레이어 식별자
     * @param dungeonId 던전 식별자
     * @return 해당 던전 진행 이력 (없으면 empty)
     */
    Optional<PlayerDungeonProgress> findByPlayerIdAndDungeonId(final Long playerId, final Long dungeonId);
}
