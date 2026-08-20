package com.myapps.web.myrpg.domain.repository;

import com.myapps.web.myrpg.domain.model.Bank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 은행 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 은행 행 로드 기능을 제공한다. 싱글 플레이어 구조로 id 오름차순 첫 번째 레코드가 유일한 은행 금고이다.
 */
public interface BankRepository extends JpaRepository<Bank, Long> {

    /**
     * id 오름차순으로 첫 번째 은행 행을 조회한다.
     *
     * <p>싱글 플레이어 게임 구조에서 유일한 은행 금고를 로드할 때 사용한다. 저장된 행이 없으면 빈 Optional을 반환한다.
     *
     * @return 가장 먼저 생성된 은행 행, 없으면 빈 Optional
     */
    Optional<Bank> findFirstByOrderByIdAsc();
}
