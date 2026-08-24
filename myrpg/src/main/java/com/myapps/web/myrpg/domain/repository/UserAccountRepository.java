package com.myapps.web.myrpg.domain.repository;

import com.myapps.web.myrpg.domain.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 계정 엔티티에 대한 Spring Data JPA 영속성 인터페이스.
 *
 * <p>사용자 아이디({@code username}) 기준 계정 조회 및 존재 여부 검사 기능을 제공한다.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * 사용자 아이디로 계정을 조회한다.
     *
     * @param username 사용자 로그인 아이디
     * @return 해당 계정, 없으면 빈 Optional
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 사용자 아이디 존재 여부를 확인한다.
     *
     * @param username 사용자 로그인 아이디
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByUsername(String username);
}
