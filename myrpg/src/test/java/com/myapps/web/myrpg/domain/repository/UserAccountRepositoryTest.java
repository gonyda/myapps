package com.myapps.web.myrpg.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.UserAccount;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/** {@link UserAccountRepository} JPA 레포지토리 슬라이스 테스트. */
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserAccountRepositoryTest {

    private final UserAccountRepository userAccountRepository;
    private final TestEntityManager entityManager;

    UserAccountRepositoryTest(
            final UserAccountRepository userAccountRepository,
            final TestEntityManager entityManager) {
        this.userAccountRepository = userAccountRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("UserAccount를 저장하고 findByUsername으로 정상 조회할 수 있다")
    void should_saveAndFindByUsername_when_accountPersisted() {
        // given
        final UserAccount account = new UserAccount("bbsk", "1", "고니", 1L);
        entityManager.persistAndFlush(account);
        entityManager.clear();

        // when
        final Optional<UserAccount> found = userAccountRepository.findByUsername("bbsk");

        // then
        assertThat(found).isPresent();
        final UserAccount loaded = found.get();
        assertThat(loaded.getUsername()).isEqualTo("bbsk");
        assertThat(loaded.getPassword()).isEqualTo("1");
        assertThat(loaded.getNickname()).isEqualTo("고니");
        assertThat(loaded.getCharacterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 username으로 조회 시 빈 Optional이 반환된다")
    void should_returnEmpty_when_usernameNotFound() {
        // given
        final String nonExistentUsername = "unknown_user";

        // when
        final Optional<UserAccount> found =
                userAccountRepository.findByUsername(nonExistentUsername);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByUsername은 계정이 존재하면 true, 없으면 false를 반환한다")
    void should_returnCorrectBoolean_when_existsByUsernameCalled() {
        // given
        final UserAccount account = new UserAccount("admin", "1", "관리자", 2L);
        entityManager.persistAndFlush(account);
        entityManager.clear();

        // when
        final boolean existsAdmin = userAccountRepository.existsByUsername("admin");
        final boolean existsGhost = userAccountRepository.existsByUsername("ghost");

        // then
        assertThat(existsAdmin).isTrue();
        assertThat(existsGhost).isFalse();
    }
}
