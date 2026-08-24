package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.exception.SkillDataException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.DotSkill;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 스킬 카탈로그 로딩 및 조회 서비스.
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/skill.json}을 1회 파싱하여 불변 {@code List<Skill>}을 구성하고, ID별·전체 목록
 * 조회 기능을 제공합니다. 데이터 무결성 위반 시 {@link SkillDataException}을 발생시켜 기동을 실패시킵니다.
 */
@Service
public class SkillCatalogService {

    private static final String SKILL_JSON_PATH = "data/skill.json";
    private static final int EXPECTED_RANK_MAP_SIZE = 16;

    private static final int DEFAULT_HIT_COUNT = 1;
    private static final int MIN_HIT_COUNT = 1;
    private static final int MAX_HIT_COUNT = 8;

    private static final int DEFAULT_CRIT_BONUS = 0;
    private static final int MIN_CRIT_BONUS = 0;
    private static final int MAX_CRIT_BONUS = 100;

    private final ObjectMapper objectMapper;
    private List<Skill> skills;

    /**
     * SkillCatalogService를 생성합니다.
     *
     * @param objectMapper Jackson 3 ObjectMapper
     */
    public SkillCatalogService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 기동 시 스킬 JSON을 로드하고 검증합니다.
     *
     * @throws SkillDataException JSON 파싱 실패 또는 데이터 무결성 위반 시
     */
    @PostConstruct
    void init() {
        try (InputStream inputStream = new ClassPathResource(SKILL_JSON_PATH).getInputStream()) {
            this.skills = loadFromStream(inputStream);
        } catch (final IOException exception) {
            throw new SkillDataException("스킬 JSON 파일 로딩 실패: " + SKILL_JSON_PATH, exception);
        }
    }

    /**
     * 입력 스트림에서 스킬 데이터를 파싱하고 검증하여 불변 목록으로 반환합니다.
     *
     * <p>파싱 로직이 리소스 로딩과 분리되어 있으므로, 프로퍼티 테스트에서 인메모리 데이터를 주입하여 검증할 수 있습니다.
     *
     * @param inputStream 스킬 JSON 데이터 입력 스트림
     * @return 검증 완료된 불변 스킬 목록 (정의 순서 보존)
     * @throws SkillDataException 파싱 실패 또는 데이터 무결성 위반 시
     */
    public List<Skill> loadFromStream(final InputStream inputStream) {
        final JsonNode rootArray = parseJson(inputStream);
        final List<Skill> parsed = parseSkillArray(rootArray);
        validateNoDuplicateIds(parsed);
        return List.copyOf(parsed);
    }

    /**
     * 전체 스킬 목록을 정의 순서대로 반환합니다.
     *
     * @return 불변 스킬 목록
     */
    public List<Skill> all() {
        return skills;
    }

    /**
     * 스킬 ID로 스킬을 조회합니다.
     *
     * @param skillId 조회할 스킬 ID
     * @return 대응하는 스킬을 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<Skill> byId(final String skillId) {
        if (skillId == null) {
            return Optional.empty();
        }
        for (final Skill skill : skills) {
            if (skillId.equals(skill.id())) {
                return Optional.of(skill);
            }
        }
        return Optional.empty();
    }

    private JsonNode parseJson(final InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (final RuntimeException exception) {
            throw new SkillDataException("스킬 JSON 파싱 실패", exception);
        }
    }

    private List<Skill> parseSkillArray(final JsonNode rootArray) {
        if (rootArray == null || !rootArray.isArray()) {
            throw new SkillDataException("스킬 JSON 최상위 구조가 배열이 아닙니다.");
        }

        final List<Skill> result = new ArrayList<>();
        for (final JsonNode skillNode : rootArray) {
            final Skill skill = parseSkillNode(skillNode);
            result.add(skill);
        }
        return result;
    }

    private Skill parseSkillNode(final JsonNode skillNode) {
        final String id = extractRequiredField(skillNode, "id");
        final String label = extractRequiredField(skillNode, "label");
        final String typeString = extractRequiredField(skillNode, "type");
        final String talentString = extractRequiredField(skillNode, "talent");
        final String description = extractRequiredField(skillNode, "description");

        final SkillType skillType =
                SkillType.fromString(typeString)
                        .orElseThrow(
                                () ->
                                        new SkillDataException(
                                                "스킬 '"
                                                        + id
                                                        + "'의 type '"
                                                        + typeString
                                                        + "'을(를) 변환할 수 없습니다."));

        final SkillTalent skillTalent =
                SkillTalent.fromString(talentString)
                        .orElseThrow(
                                () ->
                                        new SkillDataException(
                                                "스킬 '"
                                                        + id
                                                        + "'의 talent '"
                                                        + talentString
                                                        + "'을(를) 변환할 수 없습니다."));

        if (skillType == SkillType.PASSIVE) {
            final int resourceCost = extractOptionalInt(skillNode, "resourceCost", id, 0, 0, 100);
            return parsePassiveSkill(
                    skillNode, id, label, skillType, skillTalent, resourceCost, description);
        }

        final int resourceCost = extractRequiredInt(skillNode, "resourceCost", id);

        return switch (skillType) {
            case DEFENSE ->
                    parseDefenseSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case RECOVERY ->
                    parseRecoverySkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case ULTIMATE ->
                    parseUltimateSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case BUFF ->
                    parseBuffSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case CC ->
                    parseCcSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case DOT ->
                    parseDotSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case NORMAL, HEAVY, DEBUFF ->
                    parseDamageSkill(
                            skillNode,
                            id,
                            label,
                            skillType,
                            skillTalent,
                            resourceCost,
                            description);
            case PASSIVE -> throw new IllegalStateException("Unreachable");
        };
    }

    private DamageSkill parseDamageSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> multiplierByRank =
                parseRankMap(skillNode, "multiplierByRank", id);
        final int hitCount =
                extractOptionalInt(
                        skillNode, "hitCount", id, DEFAULT_HIT_COUNT, MIN_HIT_COUNT, MAX_HIT_COUNT);
        final int critBonus =
                extractOptionalInt(
                        skillNode,
                        "critBonus",
                        id,
                        DEFAULT_CRIT_BONUS,
                        MIN_CRIT_BONUS,
                        MAX_CRIT_BONUS);
        final int minHits = extractOptionalInt(skillNode, "minHits", id, 0, 0, 16);
        final int maxHits = extractOptionalInt(skillNode, "maxHits", id, 0, 0, 16);
        final boolean defensePierce =
                skillNode.has("defensePierce")
                        && (skillNode.get("defensePierce").asBoolean()
                                || skillNode.get("defensePierce").asInt() > 0);
        final Map<SkillRank, Integer> freezeRateByRank =
                parseOptionalRankMap(skillNode, "freezeRateByRank", id);

        return new DamageSkill(
                id,
                label,
                type,
                talent,
                resourceCost,
                multiplierByRank,
                description,
                hitCount,
                minHits,
                maxHits,
                critBonus,
                defensePierce,
                freezeRateByRank);
    }

    private DefenseSkill parseDefenseSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> blockRateByRank =
                parseRankMap(skillNode, "blockRateByRank", id);
        final Map<SkillRank, Integer> counterMultiplierByRank =
                parseRankMap(skillNode, "counterMultiplierByRank", id);
        final Map<SkillRank, Integer> resourceCostByRank =
                parseOptionalRankMap(skillNode, "resourceCostByRank", id);
        final Map<SkillRank, Integer> critBonusByRank =
                parseOptionalRankMap(skillNode, "critBonusByRank", id);
        return new DefenseSkill(
                id,
                label,
                type,
                talent,
                resourceCost,
                blockRateByRank,
                counterMultiplierByRank,
                description,
                resourceCostByRank,
                critBonusByRank);
    }

    private RecoverySkill parseRecoverySkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> healAmountByRank =
                parseRankMap(skillNode, "healAmountByRank", id);
        final Map<SkillRank, Integer> resourceCostByRank =
                parseOptionalRankMap(skillNode, "resourceCostByRank", id);
        final Map<SkillRank, Integer> finalCostMap =
                resourceCostByRank.isEmpty()
                        ? fillUniformRankMap(resourceCost)
                        : resourceCostByRank;
        return new RecoverySkill(
                id, label, type, talent, resourceCost, healAmountByRank, finalCostMap, description);
    }

    private UltimateSkill parseUltimateSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> multiplierByRank =
                parseRankMap(skillNode, "multiplierByRank", id);
        final int hitCount = extractOptionalInt(skillNode, "hitCount", id, 1, 1, 16);
        final int critBonus =
                extractOptionalInt(skillNode, "critBonus", id, 100, MIN_CRIT_BONUS, MAX_CRIT_BONUS);
        final Map<SkillRank, Integer> hitCountByRank = fillUniformRankMap(hitCount);
        final Map<SkillRank, Integer> coolWinsByRank = parseCoolWinsMap(skillNode, id);
        return new UltimateSkill(
                id,
                label,
                type,
                talent,
                resourceCost,
                multiplierByRank,
                hitCountByRank,
                critBonus,
                coolWinsByRank,
                description);
    }

    private PassiveSkill parsePassiveSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final JsonNode bonusNode =
                skillNode.has("statBonusTotal")
                        ? skillNode.get("statBonusTotal")
                        : skillNode.get("totalStatBonus");
        final Map<BonusTarget, Integer> totalStatBonus = new EnumMap<>(BonusTarget.class);
        if (bonusNode != null && bonusNode.isObject()) {
            for (final BonusTarget target : BonusTarget.values()) {
                final JsonNode valNode = bonusNode.get(target.name());
                if (valNode != null && valNode.isNumber()) {
                    totalStatBonus.put(target, valNode.asInt());
                }
            }
        }
        return new PassiveSkill(
                id, label, type, talent, resourceCost, Map.copyOf(totalStatBonus), description);
    }

    private BuffSkill parseBuffSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final int durationTurns = extractOptionalInt(skillNode, "durationTurns", id, 5, 1, 50);
        final Map<SkillRank, Integer> absorbRateByRank =
                parseRankMap(skillNode, "absorbRateByRank", id);
        return new BuffSkill(
                id,
                label,
                type,
                talent,
                resourceCost,
                durationTurns,
                absorbRateByRank,
                description);
    }

    private CcSkill parseCcSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> successRateByRank =
                parseRankMap(skillNode, "successRateByRank", id);
        return new CcSkill(id, label, type, talent, resourceCost, successRateByRank, description);
    }

    private DotSkill parseDotSkill(
            final JsonNode skillNode,
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final String description) {
        final Map<SkillRank, Integer> initialMultiplierByRank =
                parseRankMap(skillNode, "initialMultiplierByRank", id);
        final Map<SkillRank, Integer> dotPerTurnByRank =
                parseRankMap(skillNode, "dotPerTurnByRank", id);
        final Map<SkillRank, Integer> dotTurnsByRank =
                parseRankMap(skillNode, "dotTurnsByRank", id);
        return new DotSkill(
                id,
                label,
                type,
                talent,
                resourceCost,
                initialMultiplierByRank,
                dotPerTurnByRank,
                dotTurnsByRank,
                description);
    }

    private Map<SkillRank, Integer> fillUniformRankMap(final int value) {
        final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            map.put(rank, value);
        }
        return Map.copyOf(map);
    }

    private Map<SkillRank, Integer> parseCoolWinsMap(
            final JsonNode skillNode, final String skillId) {
        final JsonNode coolWinsNode =
                skillNode.has("coolWinsByRank")
                        ? skillNode.get("coolWinsByRank")
                        : skillNode.get("coolWins");
        final Map<SkillRank, Integer> result = new EnumMap<>(SkillRank.class);
        if (coolWinsNode != null && coolWinsNode.isObject()) {
            if (coolWinsNode.size() == EXPECTED_RANK_MAP_SIZE) {
                return parseRankMap(skillNode, "coolWinsByRank", skillId);
            }
            final int fWins = coolWinsNode.has("F") ? coolWinsNode.get("F").asInt() : 30;
            final int masterWins =
                    coolWinsNode.has("MASTER") ? coolWinsNode.get("MASTER").asInt() : 10;
            for (final SkillRank rank : SkillRank.values()) {
                final int interpolated =
                        fWins - Math.round((float) (fWins - masterWins) * rank.order() / 15.0f);
                result.put(rank, interpolated);
            }
            return Map.copyOf(result);
        }
        for (final SkillRank rank : SkillRank.values()) {
            result.put(rank, 30 - Math.round(20.0f * rank.order() / 15.0f));
        }
        return Map.copyOf(result);
    }

    private Map<SkillRank, Integer> parseOptionalRankMap(
            final JsonNode skillNode, final String fieldName, final String skillId) {
        final JsonNode mapNode = skillNode.get(fieldName);
        if (mapNode == null || mapNode.isNull()) {
            return Map.of();
        }
        return parseRankMap(skillNode, fieldName, skillId);
    }

    private Map<SkillRank, Integer> parseRankMap(
            final JsonNode skillNode, final String fieldName, final String skillId) {
        final JsonNode mapNode = skillNode.get(fieldName);
        if (mapNode == null || !mapNode.isObject()) {
            throw new SkillDataException("스킬 '" + skillId + "'의 '" + fieldName + "' 맵이 누락되었습니다.");
        }

        final Map<SkillRank, Integer> rankMap = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            final JsonNode valueNode = mapNode.get(rank.name());
            if (valueNode == null || !valueNode.isNumber()) {
                throw new SkillDataException(
                        "스킬 '"
                                + skillId
                                + "'의 '"
                                + fieldName
                                + "' 맵에 랭크 '"
                                + rank.name()
                                + "' 키가 없거나 숫자가 아닙니다.");
            }
            rankMap.put(rank, valueNode.asInt());
        }

        if (rankMap.size() != EXPECTED_RANK_MAP_SIZE) {
            throw new SkillDataException(
                    "스킬 '"
                            + skillId
                            + "'의 '"
                            + fieldName
                            + "' 맵에 "
                            + EXPECTED_RANK_MAP_SIZE
                            + "개 랭크 키가 필요하지만 "
                            + rankMap.size()
                            + "개입니다.");
        }

        return Map.copyOf(rankMap);
    }

    private String extractRequiredField(final JsonNode skillNode, final String fieldName) {
        final JsonNode fieldNode = skillNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
            final String skillId = skillNode.has("id") ? skillNode.get("id").asText() : "(unknown)";
            throw new SkillDataException(
                    "스킬 '" + skillId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있습니다.");
        }
        return fieldNode.asText();
    }

    private int extractRequiredInt(
            final JsonNode skillNode, final String fieldName, final String skillId) {
        final JsonNode fieldNode = skillNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new SkillDataException(
                    "스킬 '" + skillId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있거나 숫자가 아닙니다.");
        }
        return fieldNode.asInt();
    }

    /**
     * 스킬 노드에서 선택적 정수 필드를 추출합니다.
     *
     * <p>필드가 부재하면 기본값을 반환하고, 존재하되 숫자가 아니거나 지정 범위 밖이면 {@link SkillDataException}을 발생시킵니다.
     *
     * @param skillNode 스킬 JSON 노드
     * @param fieldName 필드명
     * @param skillId 스킬 ID (오류 메시지용)
     * @param defaultValue 필드 부재 시 반환할 기본값
     * @param minValue 허용 최솟값 (포함)
     * @param maxValue 허용 최댓값 (포함)
     * @return 파싱된 정수 또는 기본값
     * @throws SkillDataException 숫자가 아니거나 범위 밖인 경우
     */
    private int extractOptionalInt(
            final JsonNode skillNode,
            final String fieldName,
            final String skillId,
            final int defaultValue,
            final int minValue,
            final int maxValue) {
        final JsonNode fieldNode = skillNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isNumber()) {
            throw new SkillDataException(
                    "스킬 '" + skillId + "'의 필드 '" + fieldName + "'이(가) 숫자가 아닙니다.");
        }
        final int value = fieldNode.asInt();
        if (value < minValue || value > maxValue) {
            throw new SkillDataException(
                    "스킬 '"
                            + skillId
                            + "'의 필드 '"
                            + fieldName
                            + "' 값 "
                            + value
                            + "이(가) 허용 범위 ["
                            + minValue
                            + ", "
                            + maxValue
                            + "]를 벗어납니다.");
        }
        return value;
    }

    private void validateNoDuplicateIds(final List<Skill> skillList) {
        final Set<String> ids = new HashSet<>();
        for (final Skill skill : skillList) {
            if (!ids.add(skill.id())) {
                throw new SkillDataException("스킬 id '" + skill.id() + "'이(가) 중복됩니다.");
            }
        }
    }
}
