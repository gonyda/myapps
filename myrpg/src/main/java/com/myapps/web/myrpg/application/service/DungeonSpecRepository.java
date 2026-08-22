package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.application.exception.DungeonDataException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 던전 메타데이터 JSON 파일({@code data/dungeons.json})을 로드하고 조회하는 서비스.
 *
 * <p>애플리케이션 기동 시 던전 메타데이터를 파싱하고 무결성을 검증한 뒤 불변 맵으로 캐싱하여 제공합니다.
 */
@Service
public class DungeonSpecRepository {

    private static final String DUNGEONS_JSON_PATH = "data/dungeons.json";
    private static final double MIN_PROBABILITY = 0.0;
    private static final double MAX_PROBABILITY = 1.0;

    private final ObjectMapper objectMapper;
    private List<DungeonSpec> dungeons;
    private Map<String, DungeonSpec> byIdMap;

    /**
     * DungeonSpecRepository를 생성합니다.
     *
     * @param objectMapper Jackson 3 ObjectMapper
     */
    public DungeonSpecRepository(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 기동 시 던전 JSON 파일을 로드하고 검증합니다.
     *
     * @throws DungeonDataException JSON 파싱 실패 또는 데이터 무결성 위반 시
     */
    @PostConstruct
    void init() {
        try (InputStream inputStream = new ClassPathResource(DUNGEONS_JSON_PATH).getInputStream()) {
            this.dungeons = loadFromStream(inputStream);
        } catch (final IOException exception) {
            throw new DungeonDataException("던전 JSON 파일 로딩 실패: " + DUNGEONS_JSON_PATH, exception);
        }
        this.byIdMap =
                dungeons.stream()
                        .collect(Collectors.toUnmodifiableMap(DungeonSpec::id, spec -> spec));
    }

    /**
     * 입력 스트림에서 던전 메타데이터를 파싱하고 검증하여 불변 목록으로 반환합니다.
     *
     * @param inputStream 던전 JSON 데이터 입력 스트림
     * @return 검증 완료된 불변 던전 스펙 목록
     * @throws DungeonDataException JSON 파싱 실패 또는 데이터 무결성 위반 시
     */
    public List<DungeonSpec> loadFromStream(final InputStream inputStream) {
        final JsonNode root = parseJson(inputStream);
        final JsonNode dungeonsArray = root.get("dungeons");
        if (dungeonsArray == null || !dungeonsArray.isArray()) {
            throw new DungeonDataException("던전 JSON에 'dungeons' 배열이 없거나 올바르지 않습니다.");
        }

        final Set<String> ids = new HashSet<>();
        final List<DungeonSpec> list = new ArrayList<>();

        for (final JsonNode dungeonNode : dungeonsArray) {
            final DungeonSpec spec = parseDungeonNode(dungeonNode);
            if (!ids.add(spec.id())) {
                throw new DungeonDataException("던전 id '" + spec.id() + "'이(가) 중복됩니다.");
            }
            list.add(spec);
        }

        return List.copyOf(list);
    }

    /**
     * 던전 ID로 던전 스펙을 조회합니다.
     *
     * @param id 조회할 던전 ID
     * @return 대응하는 DungeonSpec을 감싼 Optional, 미존재 시 빈 Optional
     */
    public Optional<DungeonSpec> findById(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byIdMap.get(id));
    }

    /**
     * 전체 던전 스펙 목록을 반환합니다.
     *
     * @return 불변 던전 스펙 목록
     */
    public List<DungeonSpec> findAll() {
        return dungeons;
    }

    /**
     * 던전 ID로 던전 스펙을 조회하며, 미존재 시 예외를 발생시킵니다.
     *
     * @param id 조회할 던전 ID
     * @return 대응하는 DungeonSpec
     * @throws DungeonDataException 던전 스펙이 존재하지 않을 때
     */
    public DungeonSpec getById(final String id) {
        return findById(id).orElseThrow(() -> new DungeonDataException("던전 스펙을 찾을 수 없습니다: " + id));
    }

    private JsonNode parseJson(final InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (final RuntimeException exception) {
            throw new DungeonDataException("던전 JSON 파싱 실패", exception);
        }
    }

    private DungeonSpec parseDungeonNode(final JsonNode node) {
        final String id = extractRequiredField(node, "id", "(unknown)");
        final String name = extractRequiredField(node, "name", id);
        final String entranceNodeId = extractRequiredField(node, "entranceNodeId", id);
        final String theme = extractRequiredField(node, "theme", id);
        final boolean implemented = extractRequiredBoolean(node, "implemented", id);

        final DungeonGenerationSpec generation = parseGenerationSpec(node.get("generation"), id);
        final List<DungeonMonsterEntry> monsterPool =
                parseMonsterPool(node.get("monsterPool"), id, implemented);
        final double chainCombatProbability =
                extractRequiredProbability(node, "chainCombatProbability", id);
        final DungeonBossSpec boss = parseBossSpec(node.get("boss"), id);
        final DungeonRewardSpec rewards = parseRewardSpec(node.get("rewards"), id);

        return new DungeonSpec(
                id,
                name,
                entranceNodeId,
                theme,
                implemented,
                generation,
                monsterPool,
                chainCombatProbability,
                boss,
                rewards);
    }

    private DungeonGenerationSpec parseGenerationSpec(final JsonNode node, final String dungeonId) {
        if (node == null || node.isNull()) {
            throw new DungeonDataException("던전 '" + dungeonId + "'의 'generation' 설정이 누락되었습니다.");
        }
        final int minDistance = extractRequiredInt(node, "minDistanceToBoss", dungeonId);
        final int maxDistance = extractRequiredInt(node, "maxDistanceToBoss", dungeonId);
        final int minTotalRooms = extractRequiredInt(node, "minTotalRooms", dungeonId);
        final int maxTotalRooms = extractRequiredInt(node, "maxTotalRooms", dungeonId);
        final double branchProbability =
                extractRequiredProbability(node, "branchProbability", dungeonId);
        final int maxBranchDepth = extractRequiredInt(node, "maxBranchDepth", dungeonId);

        validateGenerationRanges(
                minDistance, maxDistance, minTotalRooms, maxTotalRooms, maxBranchDepth, dungeonId);

        return new DungeonGenerationSpec(
                minDistance,
                maxDistance,
                minTotalRooms,
                maxTotalRooms,
                branchProbability,
                maxBranchDepth);
    }

    private void validateGenerationRanges(
            final int minDistance,
            final int maxDistance,
            final int minTotalRooms,
            final int maxTotalRooms,
            final int maxBranchDepth,
            final String dungeonId) {
        if (minDistance < 1) {
            throw new DungeonDataException(
                    "던전 '" + dungeonId + "'의 minDistanceToBoss는 1 이상이어야 합니다.");
        }
        if (maxDistance < minDistance) {
            throw new DungeonDataException(
                    "던전 '" + dungeonId + "'의 maxDistanceToBoss가 minDistanceToBoss보다 작습니다.");
        }
        if (minTotalRooms <= maxDistance) {
            throw new DungeonDataException(
                    "던전 '" + dungeonId + "'의 minTotalRooms는 maxDistanceToBoss보다 커야 합니다.");
        }
        if (maxTotalRooms < minTotalRooms) {
            throw new DungeonDataException(
                    "던전 '" + dungeonId + "'의 maxTotalRooms가 minTotalRooms보다 작습니다.");
        }
        if (maxBranchDepth < 1) {
            throw new DungeonDataException("던전 '" + dungeonId + "'의 maxBranchDepth는 1 이상이어야 합니다.");
        }
    }

    private List<DungeonMonsterEntry> parseMonsterPool(
            final JsonNode poolNode, final String dungeonId, final boolean implemented) {
        if (poolNode == null || !poolNode.isArray()) {
            if (implemented) {
                throw new DungeonDataException(
                        "구현된 던전 '" + dungeonId + "'의 'monsterPool' 배열이 비어있습니다.");
            }
            return List.of();
        }

        if (implemented && poolNode.isEmpty()) {
            throw new DungeonDataException("구현된 던전 '" + dungeonId + "'의 'monsterPool' 배열이 비어있습니다.");
        }

        final List<DungeonMonsterEntry> entries = new ArrayList<>();
        for (final JsonNode entryNode : poolNode) {
            final String monsterId = extractRequiredField(entryNode, "monsterId", dungeonId);
            final int minCount = extractRequiredInt(entryNode, "minCount", dungeonId);
            final int maxCount = extractRequiredInt(entryNode, "maxCount", dungeonId);
            final int weight = extractRequiredInt(entryNode, "weight", dungeonId);

            if (minCount < 1 || maxCount < minCount || weight < 1) {
                throw new DungeonDataException(
                        "던전 '" + dungeonId + "'의 몬스터 '" + monsterId + "' 스폰 설정 수치가 유효하지 않습니다.");
            }
            entries.add(new DungeonMonsterEntry(monsterId, minCount, maxCount, weight));
        }

        return List.copyOf(entries);
    }

    private DungeonBossSpec parseBossSpec(final JsonNode node, final String dungeonId) {
        if (node == null || node.isNull()) {
            throw new DungeonDataException("던전 '" + dungeonId + "'의 'boss' 설정이 누락되었습니다.");
        }
        final String monsterId = extractRequiredField(node, "monsterId", dungeonId);
        final String name = extractRequiredField(node, "name", dungeonId);
        final String dialogue = node.has("dialogue") ? node.get("dialogue").asText("") : "";

        return new DungeonBossSpec(monsterId, name, dialogue);
    }

    private DungeonRewardSpec parseRewardSpec(final JsonNode node, final String dungeonId) {
        if (node == null || node.isNull()) {
            throw new DungeonDataException("던전 '" + dungeonId + "'의 'rewards' 설정이 누락되었습니다.");
        }
        final int exp = extractRequiredInt(node, "exp", dungeonId);
        final int gold = extractRequiredInt(node, "gold", dungeonId);
        if (exp < 0 || gold < 0) {
            throw new DungeonDataException("던전 '" + dungeonId + "'의 보상 exp/gold는 0 이상이어야 합니다.");
        }

        final List<DroppedItem> items = new ArrayList<>();
        final JsonNode itemsNode = node.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (final JsonNode itemNode : itemsNode) {
                final String itemId = extractRequiredField(itemNode, "itemId", dungeonId);
                final int quantity = extractRequiredInt(itemNode, "quantity", dungeonId);
                if (quantity < 1) {
                    throw new DungeonDataException(
                            "던전 '" + dungeonId + "'의 보상 아이템 '" + itemId + "' 수량은 1 이상이어야 합니다.");
                }
                items.add(new DroppedItem(itemId, quantity));
            }
        }

        return new DungeonRewardSpec(exp, gold, List.copyOf(items));
    }

    private String extractRequiredField(
            final JsonNode node, final String fieldName, final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
            throw new DungeonDataException(
                    "던전 '" + contextId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있습니다.");
        }
        return fieldNode.asText();
    }

    private int extractRequiredInt(
            final JsonNode node, final String fieldName, final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new DungeonDataException(
                    "던전 '" + contextId + "'의 필수 필드 '" + fieldName + "'이(가) 숫자가 아닙니다.");
        }
        return fieldNode.asInt();
    }

    private boolean extractRequiredBoolean(
            final JsonNode node, final String fieldName, final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isBoolean()) {
            throw new DungeonDataException(
                    "던전 '" + contextId + "'의 필수 필드 '" + fieldName + "'이(가) 불리언이 아닙니다.");
        }
        return fieldNode.asBoolean();
    }

    private double extractRequiredProbability(
            final JsonNode node, final String fieldName, final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new DungeonDataException(
                    "던전 '" + contextId + "'의 확률 필드 '" + fieldName + "'이(가) 숫자가 아닙니다.");
        }
        final double value = fieldNode.asDouble();
        if (value < MIN_PROBABILITY || value > MAX_PROBABILITY) {
            throw new DungeonDataException(
                    "던전 '"
                            + contextId
                            + "'의 확률 필드 '"
                            + fieldName
                            + "' 값("
                            + value
                            + ")은 0.0~1.0 사이여야 합니다.");
        }
        return value;
    }
}
