package com.myapps.web.myrpg.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.Player;

/**
 * 플레이어 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 플레이어 기본 CRUD 기능을 제공한다.
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {
}
