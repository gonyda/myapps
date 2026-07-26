package com.myapps.web.myrpg.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.PlayerActiveRun;

/**
 * 진행 중 던전 체크포인트 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 활성 던전 런의 CRUD 기능을 제공한다.
 * 플레이어당 최대 1개의 활성 런만 존재할 수 있다 (UNIQUE 제약).
 */
public interface PlayerActiveRunRepository extends JpaRepository<PlayerActiveRun, Long> {

    /**
     * 특정 플레이어의 진행 중인 던전 런을 조회한다.
     *
     * <p>플레이어당 최대 1개이므로 Optional로 반환한다.
     *
     * @param playerId 플레이어 식별자
     * @return 진행 중인 던전 런 (없으면 empty)
     */
    Optional<PlayerActiveRun> findByPlayerId(final Long playerId);

    /**
     * 특정 플레이어의 진행 중인 던전 런을 삭제한다.
     *
     * @param playerId 플레이어 식별자
     */
    void deleteByPlayerId(final Long playerId);
}
