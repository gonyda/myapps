package com.myapps.web.myrpg.domain.repository;

import com.myapps.web.myrpg.domain.model.BattleState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 전투 상태 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 전투 상태의 CRUD 및 활성 전투 조회 기능을 제공한다. 캐릭터당 활성 전투는 최대 1건이므로, {@link
 * #findByCharacterIdAndActiveTrue(long)}로 진행 중인 전투를 복원할 수 있다.
 */
public interface BattleStateRepository extends JpaRepository<BattleState, Long> {

    /**
     * 해당 캐릭터의 활성 전투 상태를 조회한다.
     *
     * <p>활성 전투가 없으면 빈 {@code Optional}을 반환한다. 브라우저 종료 후 재접속 시 전투 재개에 사용된다.
     *
     * @param characterId 조회할 캐릭터 ID
     * @return 활성 전투 상태, 없으면 빈 {@code Optional}
     */
    Optional<BattleState> findByCharacterIdAndActiveTrue(long characterId);
}
