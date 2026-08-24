package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.exception.AuthenticationException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.model.UserAccount;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.UserAccountRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link AuthService} 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CharacterProgressRepository characterProgressRepository;
    @Mock private CharacterSkillRepository characterSkillRepository;
    @Mock private SkillCatalogService skillCatalogService;
    @Mock private InventoryService inventoryService;
    @Mock private CharacterService characterService;

    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("올바른 자격증명으로 로그인 시 UserSession이 정상 반환된다")
    void should_returnUserSession_when_validCredentials() {
        // given
        final UserAccount account = new UserAccount("bbsk", "1", "고니", 1L);
        given(userAccountRepository.findByUsername("bbsk")).willReturn(Optional.of(account));

        // when
        final UserSession session = authService.authenticate("bbsk", "1");

        // then
        assertThat(session).isNotNull();
        assertThat(session.username()).isEqualTo("bbsk");
        assertThat(session.nickname()).isEqualTo("고니");
        assertThat(session.characterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 아이디로 로그인 시 AuthenticationException이 발생한다")
    void should_throwAuthenticationException_when_usernameNotFound() {
        // given
        given(userAccountRepository.findByUsername("unknown")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.authenticate("unknown", "1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않는 경우 AuthenticationException이 발생한다")
    void should_throwAuthenticationException_when_passwordMismatch() {
        // given
        final UserAccount account = new UserAccount("admin", "1", "관리자", 2L);
        given(userAccountRepository.findByUsername("admin")).willReturn(Optional.of(account));

        // when & then
        assertThatThrownBy(() -> authService.authenticate("admin", "wrong_password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("username 또는 password가 null인 경우 AuthenticationException이 발생한다")
    void should_throwAuthenticationException_when_nullInput() {
        // given & when & then
        assertThatThrownBy(() -> authService.authenticate(null, "1"))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> authService.authenticate("bbsk", null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("기동 시 계정이 없으면 bbsk와 admin 계정을 자동 초기화한다")
    void should_initDefaultAccounts_when_accountsDoNotExist() {
        // given
        given(userAccountRepository.findByUsername("bbsk")).willReturn(Optional.empty());
        given(userAccountRepository.findByUsername("admin")).willReturn(Optional.empty());

        final CharacterProgress goni = CharacterProgress.createDefault();
        given(characterService.loadOrCreateDefault()).willReturn(goni);

        final CharacterProgress adminChar = CharacterProgress.createNamed("관리자");
        given(characterProgressRepository.save(any(CharacterProgress.class))).willReturn(adminChar);

        final Skill mockSkill =
                new UltimateSkill(
                        "meteor",
                        "메테오 스트라이크",
                        SkillType.ULTIMATE,
                        SkillTalent.MAGIC,
                        50,
                        Map.of(SkillRank.F, 300),
                        Map.of(SkillRank.F, 1),
                        100,
                        Map.of(SkillRank.F, 30),
                        "운석을 낙하시킨다.");
        given(skillCatalogService.all()).willReturn(List.of(mockSkill));
        given(characterSkillRepository.findByCharacterIdAndSkillId(any(), any()))
                .willReturn(Optional.empty());

        // when
        authService.initDefaultAccounts();

        // then
        verify(userAccountRepository, org.mockito.Mockito.times(2)).save(any(UserAccount.class));
        verify(inventoryService).seedDefault(adminChar.getId());
        verify(characterSkillRepository).save(any());
    }

    @Test
    @DisplayName("기동 시 계정이 이미 존재하면 중복 생성하지 않는다")
    void should_notCreateAccounts_when_alreadyExist() {
        // given
        final UserAccount existingBbsk = new UserAccount("bbsk", "1", "고니", 1L);
        final UserAccount existingAdmin = new UserAccount("admin", "1", "관리자", 2L);
        given(userAccountRepository.findByUsername("bbsk")).willReturn(Optional.of(existingBbsk));
        given(userAccountRepository.findByUsername("admin")).willReturn(Optional.of(existingAdmin));

        // when
        authService.initDefaultAccounts();

        // then
        verify(userAccountRepository, never()).save(any());
        verify(inventoryService, never()).seedDefault(any());
    }
}
