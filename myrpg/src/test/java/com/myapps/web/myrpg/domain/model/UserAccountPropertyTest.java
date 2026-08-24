package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** {@link UserAccount} 도메인 모델에 대한 Property-Based Test. */
class UserAccountPropertyTest {

    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> validNicknames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<Long> characterIds() {
        return Arbitraries.longs().greaterOrEqual(1L);
    }

    @Property(tries = 100)
    void should_preserveProperties_when_constructed(
            @ForAll("validUsernames") final String username,
            @ForAll("validPasswords") final String password,
            @ForAll("validNicknames") final String nickname,
            @ForAll("characterIds") final Long characterId) {
        // given & when
        final UserAccount account = new UserAccount(username, password, nickname, characterId);

        // then
        assertThat(account.getUsername()).isEqualTo(username);
        assertThat(account.getPassword()).isEqualTo(password);
        assertThat(account.getNickname()).isEqualTo(nickname);
        assertThat(account.getCharacterId()).isEqualTo(characterId);
    }

    @Property(tries = 100)
    void should_matchPassword_onlyWhen_identical(
            @ForAll("validUsernames") final String username,
            @ForAll("validPasswords") final String password,
            @ForAll("validPasswords") final String candidatePassword,
            @ForAll("validNicknames") final String nickname,
            @ForAll("characterIds") final Long characterId) {
        // given
        final UserAccount account = new UserAccount(username, password, nickname, characterId);

        // when
        final boolean matches = account.getPassword().equals(candidatePassword);

        // then
        if (password.equals(candidatePassword)) {
            assertThat(matches).isTrue();
        } else {
            assertThat(matches).isFalse();
        }
    }

    @Property(tries = 100)
    void should_isolateCredentials_when_differentUsernames(
            @ForAll("validUsernames") final String username1,
            @ForAll("validUsernames") final String username2,
            @ForAll("validPasswords") final String password,
            @ForAll("validNicknames") final String nickname,
            @ForAll("characterIds") final Long characterId) {
        // given
        Assume.that(!username1.equals(username2));
        final UserAccount account1 = new UserAccount(username1, password, nickname, characterId);
        final UserAccount account2 = new UserAccount(username2, password, nickname, characterId);

        // then
        assertThat(account1.getUsername()).isNotEqualTo(account2.getUsername());
    }
}
