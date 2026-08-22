package com.myapps.web.myrpg.domain.repository;

import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 던전 진행상황 엔티티({@link DungeonProgressEntity})에 대한 Spring Data JPA 영속성 인터페이스.
 *
 * <p>캐릭터별 진행 중인 인스턴스 던전 데이터의 조회 및 삭제 기능을 제공합니다.
 */
public interface DungeonProgressRepository extends JpaRepository<DungeonProgressEntity, Long> {

    /**
     * 캐릭터 ID로 진행 중인 던전 엔티티를 조회한다.
     *
     * @param characterId 조회할 캐릭터 ID
     * @return 던전 진행 엔티티, 없으면 빈 {@code Optional}
     */
    Optional<DungeonProgressEntity> findByCharacterId(final Long characterId);

    /**
     * 캐릭터 ID에 해당하는 던전 진행 엔티티를 삭제한다.
     *
     * @param characterId 삭제할 캐릭터 ID
     */
    void deleteByCharacterId(final Long characterId);
}
