package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.exception.AuthenticationException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.UserAccount;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 및 기본 계정 자동 초기화 서비스.
 *
 * <p>사용자 자격증명 검증({@code authenticate}) 및 애플리케이션 기동 시 기본 프리셋 계정({@code bbsk}, {@code admin})을 자동
 * 시드합니다.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_PASSWORD = "1";
    private static final String USERNAME_BBSK = "bbsk";
    private static final String NICKNAME_BBSK = "고니";
    private static final String USERNAME_ADMIN = "admin";
    private static final String NICKNAME_ADMIN = "관리자";
    private static final String AUTH_ERROR_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";

    private final UserAccountRepository userAccountRepository;
    private final CharacterProgressRepository characterProgressRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillCatalogService skillCatalogService;
    private final InventoryService inventoryService;
    private final CharacterService characterService;

    /**
     * AuthService를 생성한다.
     *
     * @param userAccountRepository 사용자 계정 리포지토리
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param characterSkillRepository 캐릭터 스킬 리포지토리
     * @param skillCatalogService 스킬 카탈로그 서비스
     * @param inventoryService 인벤토리 서비스
     * @param characterService 캐릭터 서비스
     */
    public AuthService(
            final UserAccountRepository userAccountRepository,
            final CharacterProgressRepository characterProgressRepository,
            final CharacterSkillRepository characterSkillRepository,
            final SkillCatalogService skillCatalogService,
            final InventoryService inventoryService,
            final CharacterService characterService) {
        this.userAccountRepository = userAccountRepository;
        this.characterProgressRepository = characterProgressRepository;
        this.characterSkillRepository = characterSkillRepository;
        this.skillCatalogService = skillCatalogService;
        this.inventoryService = inventoryService;
        this.characterService = characterService;
    }

    /** 애플리케이션 기동 완료 시 기본 프리셋 계정({@code bbsk}, {@code admin})을 자동 초기화한다. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultAccounts() {
        initBbskAccount();
        initAdminAccount();
    }

    /**
     * 사용자 아이디와 비밀번호로 인증을 수행하고 불변 세션 객체를 반환한다.
     *
     * @param username 사용자 아이디
     * @param password 사용자 비밀번호
     * @return 인증된 사용자 세션 객체
     * @throws AuthenticationException 아이디가 없거나 비밀번호가 일치하지 않을 때
     */
    @Transactional(readOnly = true)
    public UserSession authenticate(final String username, final String password) {
        if (username == null || password == null) {
            throw new AuthenticationException(AUTH_ERROR_MESSAGE);
        }

        final UserAccount user =
                userAccountRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AuthenticationException(AUTH_ERROR_MESSAGE));

        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException(AUTH_ERROR_MESSAGE);
        }

        return new UserSession(
                user.getId(), user.getUsername(), user.getNickname(), user.getCharacterId());
    }

    private void initBbskAccount() {
        if (userAccountRepository.findByUsername(USERNAME_BBSK).isPresent()) {
            return;
        }

        final CharacterProgress goniChar = characterService.loadOrCreateDefault();
        final UserAccount bbskAccount =
                new UserAccount(USERNAME_BBSK, DEFAULT_PASSWORD, NICKNAME_BBSK, goniChar.getId());
        userAccountRepository.save(bbskAccount);
        LOG.info("기본 프리셋 계정 '{}' 생성 완료 (characterId={})", USERNAME_BBSK, goniChar.getId());
    }

    private void initAdminAccount() {
        if (userAccountRepository.findByUsername(USERNAME_ADMIN).isPresent()) {
            return;
        }

        final CharacterProgress adminChar = CharacterProgress.createNamed(NICKNAME_ADMIN);
        final CharacterProgress savedAdmin = characterProgressRepository.save(adminChar);

        // 1. 초보자 장비 풀세트(6종 착용 + 4종 보유 + 포션 15개) 지급
        inventoryService.seedDefault(savedAdmin.getId());

        // 2. 35종 전체 스킬 F랭크 일괄 지급
        for (final Skill skill : skillCatalogService.all()) {
            if (characterSkillRepository
                    .findByCharacterIdAndSkillId(savedAdmin.getId(), skill.id())
                    .isEmpty()) {
                characterSkillRepository.save(
                        CharacterSkill.newSkill(savedAdmin.getId(), skill.id()));
            }
        }

        // 3. 어드민 계정 저장
        final UserAccount adminAccount =
                new UserAccount(
                        USERNAME_ADMIN, DEFAULT_PASSWORD, NICKNAME_ADMIN, savedAdmin.getId());
        userAccountRepository.save(adminAccount);
        LOG.info(
                "관리자 프리셋 계정 '{}' 생성 완료 (characterId={}, 스킬 35종 F랭크 일괄 습득)",
                USERNAME_ADMIN,
                savedAdmin.getId());
    }
}
