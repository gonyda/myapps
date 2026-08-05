package com.myapps.web.myrpg.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.CharacterProgress;

/**
 * 캐릭터 진행상황 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 기존 진행상황 로드 기능을 제공합니다.
 * 싱글 플레이어 구조로 id 오름차순 첫 번째 레코드가 유일한 진행상황입니다.
 */
public interface CharacterProgressRepository extends JpaRepository<CharacterProgress, Long> {

    /**
     * id 오름차순으로 첫 번째 캐릭터 진행상황을 조회합니다.
     *
     * <p>싱글 플레이어 게임 구조에서 기존에 저장된 진행상황을 로드할 때 사용합니다.
     * 저장된 진행상황이 없으면 빈 Optional을 반환합니다.
     *
     * @return 가장 먼저 생성된 캐릭터 진행상황, 없으면 빈 Optional
     */
    Optional<CharacterProgress> findFirstByOrderByIdAsc();
}
