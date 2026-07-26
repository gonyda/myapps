package com.myapps.web.myrpg.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.application.exception.MasterDataValidationException;
import com.myapps.web.myrpg.domain.exception.MasterDataNotFoundException;
import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.ItemTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

/**
 * 마스터 데이터 로더.
 *
 * <p>애플리케이션 기동 시 classpath의 JSON 데이터 파일을 로딩하여
 * id 기반 인덱싱 맵을 구성한다. 던전의 gradeChance 합 검증도 수행한다.
 */
@Service
public class MasterDataLoader {

    private static final double GRADE_CHANCE_TOLERANCE = 1e-6;
    private static final String DATA_PATH_PREFIX = "data/";

    private final ObjectMapper objectMapper;

    private Map<Long, MonsterTemplate> monsters = Map.of();
    private Map<Long, WeaponTemplate> weapons = Map.of();
    private Map<Long, ArmorTemplate> armors = Map.of();
    private Map<Long, SkillTemplate> skills = Map.of();
    private Map<Long, ItemTemplate> items = Map.of();
    private Map<Long, DungeonTemplate> dungeons = Map.of();

    /**
     * MasterDataLoader를 생성한다.
     *
     * @param objectMapper Jackson 3 ObjectMapper (Spring 자동 구성)
     */
    public MasterDataLoader(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 기동 시 JSON 마스터 데이터를 로딩하고 검증한다.
     *
     * <p>모든 JSON 파일을 파싱하여 id 기반 인덱스 맵을 구성한 뒤,
     * 던전별 gradeChance 합이 1.0(허용오차 1e-6)인지 검증한다.
     *
     * @throws MasterDataValidationException gradeChance 합 검증 실패 시
     */
    @PostConstruct
    public void load() {
        monsters = loadAndIndex("monsters.json", MonsterTemplate.class, MonsterTemplate::id);
        weapons = loadAndIndex("weapons.json", WeaponTemplate.class, WeaponTemplate::id);
        armors = loadAndIndex("armors.json", ArmorTemplate.class, ArmorTemplate::id);
        skills = loadAndIndex("skills.json", SkillTemplate.class, SkillTemplate::id);
        items = loadAndIndex("items.json", ItemTemplate.class, ItemTemplate::id);
        dungeons = loadAndIndex("dungeons.json", DungeonTemplate.class, DungeonTemplate::id);

        validateGradeChance();
    }

    /**
     * 몬스터 템플릿을 id로 조회한다.
     *
     * @param id 몬스터 id
     * @return 몬스터 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public MonsterTemplate findMonster(final long id) {
        final MonsterTemplate template = monsters.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Monster not found: id=" + id);
        }
        return template;
    }

    /**
     * 무기 템플릿을 id로 조회한다.
     *
     * @param id 무기 id
     * @return 무기 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public WeaponTemplate findWeaponTemplate(final long id) {
        final WeaponTemplate template = weapons.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Weapon not found: id=" + id);
        }
        return template;
    }

    /**
     * 방어구 템플릿을 id로 조회한다.
     *
     * @param id 방어구 id
     * @return 방어구 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public ArmorTemplate findArmorTemplate(final long id) {
        final ArmorTemplate template = armors.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Armor not found: id=" + id);
        }
        return template;
    }

    /**
     * 스킬 템플릿을 id로 조회한다.
     *
     * @param id 스킬 id
     * @return 스킬 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public SkillTemplate findSkill(final long id) {
        final SkillTemplate template = skills.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Skill not found: id=" + id);
        }
        return template;
    }

    /**
     * 아이템 템플릿을 id로 조회한다.
     *
     * @param id 아이템 id
     * @return 아이템 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public ItemTemplate findItem(final long id) {
        final ItemTemplate template = items.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Item not found: id=" + id);
        }
        return template;
    }

    /**
     * 던전 템플릿을 id로 조회한다.
     *
     * @param id 던전 id
     * @return 던전 템플릿
     * @throws MasterDataNotFoundException 해당 id가 존재하지 않을 때
     */
    public DungeonTemplate findDungeon(final long id) {
        final DungeonTemplate template = dungeons.get(id);
        if (template == null) {
            throw new MasterDataNotFoundException("Dungeon not found: id=" + id);
        }
        return template;
    }

    /**
     * 모든 던전 템플릿을 반환한다.
     *
     * @return 변경 불가능한 던전 템플릿 목록
     */
    public List<DungeonTemplate> allDungeons() {
        return Collections.unmodifiableList(List.copyOf(dungeons.values()));
    }

    /**
     * 모든 아이템(소모품) 템플릿을 반환한다.
     *
     * @return 변경 불가능한 아이템 템플릿 목록
     */
    public List<ItemTemplate> allItems() {
        return Collections.unmodifiableList(List.copyOf(items.values()));
    }

    /**
     * 지정한 무기 타입 목록에 해당하는 무기 템플릿들을 반환한다.
     *
     * @param weaponTypes 무기 타입 목록
     * @return 해당 무기 타입의 템플릿 목록
     */
    public List<WeaponTemplate> weaponsForTypes(final List<WeaponType> weaponTypes) {
        return weapons.values().stream()
                .filter(w -> weaponTypes.contains(w.weaponType()))
                .toList();
    }

    /**
     * 지정한 방어구 부위 목록에 해당하는 방어구 템플릿들을 반환한다.
     *
     * @param armorSlots 방어구 부위 목록
     * @return 해당 부위의 방어구 템플릿 목록
     */
    public List<ArmorTemplate> armorsForSlots(final List<ArmorSlot> armorSlots) {
        return armors.values().stream()
                .filter(a -> armorSlots.contains(a.slot()))
                .toList();
    }

    /**
     * 지정한 무기 타입에 해당하는 스킬 템플릿 목록을 반환한다.
     *
     * @param weaponType 무기 타입
     * @return 해당 무기 타입의 스킬 템플릿 목록
     */
    public List<SkillTemplate> skillsForWeaponType(final WeaponType weaponType) {
        return skills.values().stream()
                .filter(s -> s.weaponType() == weaponType)
                .toList();
    }

    private <T> Map<Long, T> loadAndIndex(final String fileName,
                                          final Class<T> elementType,
                                          final Function<T, Long> idExtractor) {
        final String resourcePath = DATA_PATH_PREFIX + fileName;
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            final List<T> list = objectMapper.readValue(
                    inputStream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
            return list.stream()
                    .collect(Collectors.toUnmodifiableMap(idExtractor, Function.identity()));
        } catch (final IOException e) {
            throw new MasterDataValidationException(
                    "Failed to load master data file: " + fileName + " - " + e.getMessage());
        }
    }

    private void validateGradeChance() {
        for (final DungeonTemplate dungeon : dungeons.values()) {
            final double sum = dungeon.gradeChance().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            if (Math.abs(sum - 1.0) > GRADE_CHANCE_TOLERANCE) {
                throw new MasterDataValidationException(
                        "Dungeon '" + dungeon.name() + "' (id=" + dungeon.id()
                                + ") gradeChance sum is " + sum + ", expected 1.0");
            }
        }
    }
}
