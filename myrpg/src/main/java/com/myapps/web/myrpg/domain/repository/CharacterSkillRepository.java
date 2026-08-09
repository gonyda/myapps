package com.myapps.web.myrpg.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.myrpg.domain.model.CharacterSkill;

/**
 * 캐릭터 보유 스킬 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 캐릭터별 스킬 조회 기능을 제공한다.
 * {@code character_id}와 {@code skill_id}의 조합은 논리적으로 유일하다.
 */
public interface CharacterSkillRepository extends JpaRepository<CharacterSkill, Long> {

    /**
     * 특정 캐릭터가 보유한 모든 스킬을 조회한다.
     *
     * @param characterId 캐릭터 ID
     * @return 보유 스킬 목록 (없으면 빈 리스트)
     */
    List<CharacterSkill> findByCharacterId(Long characterId);

    /**
     * 특정 캐릭터의 특정 스킬을 조회한다.
     *
     * @param characterId 캐릭터 ID
     * @param skillId     스킬 카탈로그 ID
     * @return 해당 스킬, 없으면 빈 Optional
     */
    Optional<CharacterSkill> findByCharacterIdAndSkillId(Long characterId, String skillId);
}
