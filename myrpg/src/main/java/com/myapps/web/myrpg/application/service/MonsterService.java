package com.myapps.web.myrpg.application.service;

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

import com.myapps.web.myrpg.application.exception.MonsterDataException;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.ItemDrop;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 몬스터 카탈로그 로딩, 교차검증 및 조회 서비스.
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/monster.json}을 1회 파싱하여
 * 불변 {@link Monster} 목록을 구성하고, ID별·노드별 조회 기능을 제공합니다.
 * 카탈로그 파싱 후 맵 배치·아이템 존재 교차검증을 수행하며,
 * 무결성 위반 시 {@link MonsterDataException}을 발생시켜 기동을 실패시킵니다.
 */
@Service
public class MonsterService {

    private static final String MONSTER_JSON_PATH = "data/monster.json";
    private static final int REQUIRED_LINES_COUNT = 3;
    private static final int MIN_CHANCE_PERCENT = 1;
    private static final int MAX_CHANCE_PERCENT = 100;
    private static final int DEFAULT_DEFENSE_BLOCK_RATE = 40;
    private static final int DEFAULT_DEFENSE_COUNTER_RATE = 30;

    private final ObjectMapper objectMapper;
    private final MapService mapService;
    private final ItemCatalogService itemCatalogService;
    private List<Monster> monsters;
    private Map<String, Monster> byIdMap;

    /**
     * MonsterService를 생성합니다.
     *
     * @param objectMapper      Jackson 3 ObjectMapper
     * @param mapService        맵 데이터 서비스 (노드별 몬스터 배치 교차검증용)
     * @param itemCatalogService 아이템 카탈로그 서비스 (드랍 아이템 존재 교차검증용)
     */
    public MonsterService(final ObjectMapper objectMapper,
                          final MapService mapService,
                          final ItemCatalogService itemCatalogService) {
        this.objectMapper = objectMapper;
        this.mapService = mapService;
        this.itemCatalogService = itemCatalogService;
    }

    /**
     * 애플리케이션 기동 시 몬스터 JSON을 로드, 검증 및 교차검증합니다.
     *
     * @throws MonsterDataException JSON 파싱 실패, 데이터 무결성 위반 또는 교차검증 실패 시
     */
    @PostConstruct
    void init() {
        try (InputStream inputStream = new ClassPathResource(MONSTER_JSON_PATH).getInputStream()) {
            this.monsters = loadFromStream(inputStream);
        } catch (final IOException exception) {
            throw new MonsterDataException(
                    "몬스터 JSON 파일 로딩 실패: " + MONSTER_JSON_PATH, exception);
        }
        this.byIdMap = monsters.stream()
                .collect(Collectors.toUnmodifiableMap(Monster::id, monster -> monster));
        crossValidate();
    }

    /**
     * 입력 스트림에서 몬스터 데이터를 파싱하고 검증하여 불변 목록으로 반환합니다.
     *
     * <p>파싱 로직이 리소스 로딩과 분리되어 있으므로, 프로퍼티 테스트에서
     * 인메모리 데이터를 주입하여 검증할 수 있습니다.
     *
     * @param inputStream 몬스터 JSON 데이터 입력 스트림
     * @return 검증 완료된 불변 몬스터 목록 (정의 순서 보존)
     * @throws MonsterDataException 파싱 실패 또는 데이터 무결성 위반 시
     */
    public List<Monster> loadFromStream(final InputStream inputStream) {
        final JsonNode rootArray = parseJson(inputStream);
        validateRootArray(rootArray);

        final Set<String> ids = new HashSet<>();
        final List<Monster> parsed = new ArrayList<>();

        for (final JsonNode monsterNode : rootArray) {
            final Monster monster = parseMonsterNode(monsterNode);
            if (!ids.add(monster.id())) {
                throw new MonsterDataException(
                        "몬스터 id '" + monster.id() + "'이(가) 중복됩니다.");
            }
            parsed.add(monster);
        }

        return List.copyOf(parsed);
    }

    /**
     * 전체 몬스터 목록을 정의 순서대로 반환합니다.
     *
     * @return 불변 몬스터 목록
     */
    public List<Monster> all() {
        return monsters;
    }

    /**
     * 몬스터 ID로 몬스터를 조회합니다.
     *
     * @param monsterId 조회할 몬스터 ID
     * @return 대응하는 몬스터를 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<Monster> byId(final String monsterId) {
        if (monsterId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byIdMap.get(monsterId));
    }

    /**
     * 지정된 맵 노드에 배치된 몬스터 목록을 {@code map.json}의 순서대로 반환합니다.
     *
     * <p>미지 노드이거나 {@code null}이면 빈 목록을 반환합니다(예외를 던지지 않음).
     * 맵에 정의되었으나 카탈로그에 없는 몬스터는 교차검증에서 이미 걸러지므로
     * 런타임에는 항상 유효한 결과만 반환됩니다.
     *
     * @param nodeId 조회할 맵 노드 ID
     * @return 해당 노드의 몬스터 목록 (맵 정의 순서 보존), 미지/null 노드 시 빈 목록
     */
    public List<Monster> byNode(final String nodeId) {
        if (nodeId == null) {
            return List.of();
        }
        final Optional<MapNode> nodeOpt = mapService.graph().byId(nodeId);
        if (nodeOpt.isEmpty()) {
            return List.of();
        }
        final MapNode node = nodeOpt.get();
        final List<Monster> result = new ArrayList<>();
        for (final String monsterId : node.monsters()) {
            final Monster monster = byIdMap.get(monsterId);
            if (monster != null) {
                result.add(monster);
            }
        }
        return List.copyOf(result);
    }

    private JsonNode parseJson(final InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (final RuntimeException exception) {
            throw new MonsterDataException("몬스터 JSON 파싱 실패", exception);
        }
    }

    private void validateRootArray(final JsonNode rootArray) {
        if (rootArray == null || !rootArray.isArray()) {
            throw new MonsterDataException("몬스터 JSON 최상위 구조가 배열이 아닙니다.");
        }
    }

    private Monster parseMonsterNode(final JsonNode monsterNode) {
        final String id = extractRequiredString(monsterNode, "id");
        final String name = extractRequiredString(monsterNode, "name");
        final String typeString = extractRequiredString(monsterNode, "type");

        final MonsterType monsterType = MonsterType.fromType(typeString)
                .orElseThrow(() -> new MonsterDataException(
                        "몬스터 '" + id + "'의 type '" + typeString + "'을(를) 분류할 수 없습니다."));

        final int level = extractRequiredInt(monsterNode, "level", id);
        final int maxHp = extractRequiredInt(monsterNode, "maxHp", id);
        final int attackPower = extractRequiredInt(monsterNode, "attackPower", id);
        final int defense = extractRequiredInt(monsterNode, "defense", id);
        final int critical = extractRequiredInt(monsterNode, "critical", id);
        final long experience = extractRequiredLong(monsterNode, "experience", id);

        validateStatRanges(id, level, maxHp, attackPower, defense);

        final GoldDrop goldDrop = parseGoldDrop(monsterNode, id);
        final List<ItemDrop> itemDrops = parseItemDrops(monsterNode, id);
        final List<String> lines = parseLines(monsterNode, id);

        final int defenseBlockRate = extractOptionalInt(monsterNode, "defenseBlockRate",
                DEFAULT_DEFENSE_BLOCK_RATE);
        final int defenseCounterRate = extractOptionalInt(monsterNode, "defenseCounterRate",
                DEFAULT_DEFENSE_COUNTER_RATE);

        return new Monster(id, name, monsterType, level, maxHp, attackPower,
                defense, critical, experience, goldDrop, itemDrops, lines,
                defenseBlockRate, defenseCounterRate);
    }

    private void validateStatRanges(final String id, final int level, final int maxHp,
                                    final int attackPower, final int defense) {
        if (level < 1) {
            throw new MonsterDataException(
                    "몬스터 '" + id + "'의 level은 1 이상이어야 합니다: " + level);
        }
        if (maxHp < 1) {
            throw new MonsterDataException(
                    "몬스터 '" + id + "'의 maxHp는 1 이상이어야 합니다: " + maxHp);
        }
        if (attackPower < 0) {
            throw new MonsterDataException(
                    "몬스터 '" + id + "'의 attackPower는 0 이상이어야 합니다: " + attackPower);
        }
        if (defense < 0) {
            throw new MonsterDataException(
                    "몬스터 '" + id + "'의 defense는 0 이상이어야 합니다: " + defense);
        }
    }

    private GoldDrop parseGoldDrop(final JsonNode monsterNode, final String monsterId) {
        final JsonNode goldDropNode = monsterNode.get("goldDrop");
        if (goldDropNode == null || goldDropNode.isNull() || !goldDropNode.isObject()) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 필수 필드 'goldDrop'이(가) 비어있거나 형식이 올바르지 않습니다.");
        }

        final int min = extractRequiredInt(goldDropNode, "min", monsterId + ".goldDrop");
        final int max = extractRequiredInt(goldDropNode, "max", monsterId + ".goldDrop");

        try {
            return new GoldDrop(min, max);
        } catch (final IllegalArgumentException exception) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 goldDrop 범위가 올바르지 않습니다: " + exception.getMessage());
        }
    }

    private List<ItemDrop> parseItemDrops(final JsonNode monsterNode, final String monsterId) {
        final JsonNode itemDropsNode = monsterNode.get("itemDrops");
        if (itemDropsNode == null || itemDropsNode.isNull()) {
            return List.of();
        }
        if (!itemDropsNode.isArray()) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 'itemDrops' 필드가 배열이 아닙니다.");
        }

        final List<ItemDrop> result = new ArrayList<>();
        for (final JsonNode dropNode : itemDropsNode) {
            final ItemDrop itemDrop = parseItemDrop(dropNode, monsterId);
            result.add(itemDrop);
        }
        return List.copyOf(result);
    }

    private ItemDrop parseItemDrop(final JsonNode dropNode, final String monsterId) {
        final String itemId = extractRequiredString(dropNode, "itemId");
        final int chancePercent = extractRequiredInt(dropNode, "chancePercent", monsterId + ".itemDrops");
        final int minQuantity = extractRequiredInt(dropNode, "minQuantity", monsterId + ".itemDrops");
        final int maxQuantity = extractRequiredInt(dropNode, "maxQuantity", monsterId + ".itemDrops");

        if (chancePercent < MIN_CHANCE_PERCENT || chancePercent > MAX_CHANCE_PERCENT) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 itemDrops.chancePercent는 1~100이어야 합니다: "
                            + chancePercent);
        }
        if (minQuantity < 1) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 itemDrops.minQuantity는 1 이상이어야 합니다: "
                            + minQuantity);
        }
        if (maxQuantity < minQuantity) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 itemDrops.maxQuantity는 minQuantity 이상이어야 합니다: "
                            + "min=" + minQuantity + ", max=" + maxQuantity);
        }

        return new ItemDrop(itemId, chancePercent, minQuantity, maxQuantity);
    }

    private List<String> parseLines(final JsonNode monsterNode, final String monsterId) {
        final JsonNode linesNode = monsterNode.get("lines");
        if (linesNode == null || linesNode.isNull() || !linesNode.isArray()) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 필수 필드 'lines'가 배열이 아니거나 누락되었습니다.");
        }
        if (linesNode.size() != REQUIRED_LINES_COUNT) {
            throw new MonsterDataException(
                    "몬스터 '" + monsterId + "'의 lines는 정확히 " + REQUIRED_LINES_COUNT
                            + "개여야 합니다: " + linesNode.size() + "개");
        }

        final List<String> lines = new ArrayList<>();
        for (final JsonNode lineNode : linesNode) {
            lines.add(lineNode.asText());
        }
        return List.copyOf(lines);
    }

    private void crossValidate() {
        validateMapMonstersExist();
        validateNodeMonstersDuplicate();
        validateItemDropsExist();
    }

    private void validateMapMonstersExist() {
        for (final MapNode node : mapService.graph().nodes()) {
            for (final String monsterId : node.monsters()) {
                if (!byIdMap.containsKey(monsterId)) {
                    throw new MonsterDataException(
                            "맵 노드 '" + node.id() + "'의 몬스터 '" + monsterId
                                    + "'이(가) 몬스터 카탈로그에 존재하지 않습니다.");
                }
            }
        }
    }

    private void validateNodeMonstersDuplicate() {
        for (final MapNode node : mapService.graph().nodes()) {
            final Set<String> nodeMonsterIds = new HashSet<>();
            for (final String monsterId : node.monsters()) {
                if (!nodeMonsterIds.add(monsterId)) {
                    throw new MonsterDataException(
                            "맵 노드 '" + node.id() + "'의 monsters 배열에 '"
                                    + monsterId + "'이(가) 중복됩니다.");
                }
            }
        }
    }

    private void validateItemDropsExist() {
        for (final Monster monster : monsters) {
            for (final ItemDrop itemDrop : monster.itemDrops()) {
                if (itemCatalogService.byId(itemDrop.itemId()).isEmpty()) {
                    throw new MonsterDataException(
                            "몬스터 '" + monster.id() + "'의 itemDrops.itemId '"
                                    + itemDrop.itemId() + "'이(가) 아이템 카탈로그에 존재하지 않습니다.");
                }
            }
        }
    }

    private String extractRequiredString(final JsonNode node, final String fieldName) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
            final String contextId = node.has("id") ? node.get("id").asText() : "(unknown)";
            throw new MonsterDataException(
                    "몬스터 '" + contextId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있습니다.");
        }
        return fieldNode.asText();
    }

    private int extractRequiredInt(final JsonNode node, final String fieldName,
                                   final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new MonsterDataException(
                    "몬스터 '" + contextId + "'의 필수 필드 '" + fieldName
                            + "'이(가) 비어있거나 숫자가 아닙니다.");
        }
        return fieldNode.asInt();
    }

    private long extractRequiredLong(final JsonNode node, final String fieldName,
                                     final String contextId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new MonsterDataException(
                    "몬스터 '" + contextId + "'의 필수 필드 '" + fieldName
                            + "'이(가) 비어있거나 숫자가 아닙니다.");
        }
        return fieldNode.asLong();
    }

    private int extractOptionalInt(final JsonNode node, final String fieldName,
                                   final int defaultValue) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            return defaultValue;
        }
        return fieldNode.asInt();
    }
}
