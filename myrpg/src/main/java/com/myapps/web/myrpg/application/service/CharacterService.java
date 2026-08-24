package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.exception.CharacterCreationException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐릭터 진행상황 관리 서비스.
 *
 * <p>저장소에서 기존 진행상황을 로드하거나, 비어 있을 때 기본 캐릭터를 생성합니다. 신규 캐릭터 생성 시 기본 스킬(windmill F)과 기본 아이템(초보자 장비 6종
 * 장착 + 4종 미장착 + 포션)을 시드합니다. 턴 종료 시 변경된 진행상황을 저장하는 기능도 제공합니다.
 */
@Service
public class CharacterService {

    private static final Logger LOG = LoggerFactory.getLogger(CharacterService.class);

    private final CharacterProgressRepository characterProgressRepository;
    private final SkillService skillService;
    private final InventoryService inventoryService;

    /**
     * CharacterService를 생성합니다.
     *
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param skillService 스킬 시스템 서비스 (신규 캐릭터 시드용)
     * @param inventoryService 인벤토리 서비스 (신규 캐릭터 기본 아이템 시드용)
     */
    public CharacterService(
            final CharacterProgressRepository characterProgressRepository,
            final SkillService skillService,
            final InventoryService inventoryService) {
        this.characterProgressRepository = characterProgressRepository;
        this.skillService = skillService;
        this.inventoryService = inventoryService;
    }

    /**
     * 저장소에서 기존 캐릭터 진행상황을 로드하거나, 비어 있으면 기본 캐릭터를 생성·저장합니다.
     *
     * <p>저장소가 비어 있으면 닉네임 "고니", Base_Stats, Lv1/누적1/EXP0, 시작 노드 "tir-chonaill"의 기본 캐릭터를 정확히 1개 생성하여
     * 저장합니다. 저장소에 1개 이상의 레코드가 존재하면 기존 진행상황을 반환합니다.
     *
     * @return 로드되거나 새로 생성된 캐릭터 진행상황
     * @throws CharacterCreationException 기본 캐릭터 저장 실패 시 (트랜잭션 롤백 후)
     */
    @Transactional
    public CharacterProgress loadOrCreateDefault() {
        return characterProgressRepository
                .findFirstByOrderByIdAsc()
                .orElseGet(this::createAndSaveDefault);
    }

    /**
     * 지정된 식별자의 캐릭터 진행상황을 로드하거나, 없으면 기본 캐릭터를 로드/생성합니다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 기본 캐릭터 로드)
     * @return 로드되거나 새로 생성된 캐릭터 진행상황
     * @throws CharacterCreationException 캐릭터 로드 또는 생성 실패 시
     */
    @Transactional
    public CharacterProgress loadByCharacterId(final Long characterId) {
        if (characterId == null) {
            return loadOrCreateDefault();
        }
        return characterProgressRepository
                .findById(characterId)
                .orElseGet(this::createAndSaveDefault);
    }

    /**
     * 턴 종료 시 변경된 캐릭터 진행상황을 저장합니다.
     *
     * @param progress 저장할 캐릭터 진행상황
     * @return 저장된 캐릭터 진행상황
     * @throws CharacterCreationException 저장 실패 시 (트랜잭션 롤백 후)
     */
    @Transactional
    public CharacterProgress saveTurn(final CharacterProgress progress) {
        try {
            return characterProgressRepository.save(progress);
        } catch (final Exception exception) {
            LOG.error("턴 종료 저장 실패: {}", exception.getMessage(), exception);
            throw new CharacterCreationException("캐릭터 진행상황 저장에 실패했습니다.", exception);
        }
    }

    private CharacterProgress createAndSaveDefault() {
        final CharacterProgress defaultCharacter = CharacterProgress.createDefault();
        try {
            final CharacterProgress saved = characterProgressRepository.save(defaultCharacter);
            skillService.seedDefault(saved.getId());
            inventoryService.seedDefault();
            return saved;
        } catch (final Exception exception) {
            LOG.error("기본 캐릭터 저장 실패: {}", exception.getMessage(), exception);
            throw new CharacterCreationException("기본 캐릭터 생성에 실패했습니다.", exception);
        }
    }
}
