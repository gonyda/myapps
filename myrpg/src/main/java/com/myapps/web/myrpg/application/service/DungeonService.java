package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.application.exception.DungeonDataException;
import com.myapps.web.myrpg.application.exception.DungeonNotImplementedException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.DungeonProgressRepository;
import com.myapps.web.myrpg.domain.service.DungeonGenerator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 인스턴스 던전 생명주기 및 상태 관리를 총괄하는 애플리케이션 서비스.
 *
 * <p>던전 입장/퇴장, 백트래킹 이동 제어, 몬스터 처치 상태 동기화, 보스 처치 보상 지급 및 DB 영속 저장을 처리합니다.
 */
@Service
public class DungeonService {

    private static final String LOG_TYPE_DUNGEON = "dungeon";
    private static final String BLOCKED_FORWARD_MESSAGE = "앞으로 나아가려면 이 방의 적들을 모두 처치해야 합니다.";
    private static final String UNLINKED_ROOM_MESSAGE = "연결되지 않은 방입니다.";

    private final DungeonSpecRepository dungeonSpecRepository;
    private final DungeonGenerator dungeonGenerator;
    private final DungeonProgressRepository dungeonProgressRepository;
    private final CharacterProgressRepository characterProgressRepository;
    private final ProgressionService progressionService;
    private final InventoryService inventoryService;
    private final MonsterRewardService monsterRewardService;
    private final ItemCatalogService itemCatalogService;
    private final ActionLog actionLog;
    private final ObjectMapper objectMapper;

    /**
     * DungeonService를 생성합니다.
     *
     * @param dungeonSpecRepository 던전 스펙 저장소
     * @param dungeonGenerator 프로시저럴 던전 생성 엔진
     * @param dungeonProgressRepository 던전 진행상황 JPA 리포지토리
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param progressionService 경험치/사망 처리 서비스
     * @param inventoryService 인벤토리 서비스
     * @param monsterRewardService 몬스터/던전 보상 추첨 서비스
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param actionLog 활동 로그
     * @param objectMapper Jackson ObjectMapper
     */
    public DungeonService(
            final DungeonSpecRepository dungeonSpecRepository,
            final DungeonGenerator dungeonGenerator,
            final DungeonProgressRepository dungeonProgressRepository,
            final CharacterProgressRepository characterProgressRepository,
            final ProgressionService progressionService,
            final InventoryService inventoryService,
            final MonsterRewardService monsterRewardService,
            final ItemCatalogService itemCatalogService,
            final ActionLog actionLog,
            final ObjectMapper objectMapper) {
        this.dungeonSpecRepository = dungeonSpecRepository;
        this.dungeonGenerator = dungeonGenerator;
        this.dungeonProgressRepository = dungeonProgressRepository;
        this.characterProgressRepository = characterProgressRepository;
        this.progressionService = progressionService;
        this.inventoryService = inventoryService;
        this.monsterRewardService = monsterRewardService;
        this.itemCatalogService = itemCatalogService;
        this.actionLog = actionLog;
        this.objectMapper = objectMapper;
    }

    /**
     * 캐릭터를 지정된 던전에 입장시키고 시작방에 배치합니다.
     *
     * <p>던전 스펙이 구현되어 있는지 검증하고, 프로시저럴 맵을 생성하여 DB에 영속 저장한 뒤 캐릭터의 위치를 시작방으로 갱신합니다.
     *
     * @param characterId 입장할 캐릭터 ID
     * @param dungeonId 던전 메타데이터 ID
     * @return 생성된 던전 인스턴스
     * @throws DungeonDataException 던전 스펙을 찾을 수 없을 때
     * @throws DungeonNotImplementedException 아직 미구현된 던전일 때
     */
    @Transactional
    public DungeonInstance enterDungeon(final Long characterId, final String dungeonId) {
        final DungeonSpec spec =
                dungeonSpecRepository
                        .findById(dungeonId)
                        .orElseThrow(
                                () -> new DungeonDataException("던전 스펙을 찾을 수 없습니다: " + dungeonId));

        if (!spec.implemented()) {
            throw new DungeonNotImplementedException("해당 던전은 아직 준비 중입니다: " + dungeonId);
        }

        final CharacterProgress character = loadCharacterOrThrow(characterId);
        final DungeonInstance instance = dungeonGenerator.generate(spec, characterId);

        dungeonProgressRepository.deleteByCharacterId(characterId);

        final String graphJson = serializeDungeonGraph(instance.dungeonGraph());
        final String roomStatesJson = serializeRoomStates(instance.roomStates());
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        characterId,
                        spec.id(),
                        spec.entranceNodeId(),
                        instance.startRoomId(),
                        instance.bossRoomId(),
                        instance.startRoomId(),
                        graphJson,
                        roomStatesJson);
        dungeonProgressRepository.save(entity);

        character.updateCurrentNodeId(instance.startRoomId());
        characterProgressRepository.save(character);

        actionLog.add(spec.name() + "에 입장했습니다.", LOG_TYPE_DUNGEON);
        return instance;
    }

    /**
     * 시작방에서 던전을 자발적으로 퇴장하고 필드의 던전 입구 노드로 복귀합니다.
     *
     * @param characterId 퇴장할 캐릭터 ID
     */
    @Transactional
    public void leaveDungeon(final Long characterId) {
        final Optional<DungeonProgressEntity> entityOpt =
                dungeonProgressRepository.findByCharacterId(characterId);
        if (entityOpt.isEmpty()) {
            return;
        }

        final DungeonProgressEntity entity = entityOpt.get();
        final String entranceNodeId = entity.getEntranceNodeId();
        dungeonProgressRepository.delete(entity);

        final Optional<CharacterProgress> charOpt =
                characterProgressRepository.findById(characterId);
        if (charOpt.isPresent()) {
            final CharacterProgress character = charOpt.get();
            character.updateCurrentNodeId(entranceNodeId);
            characterProgressRepository.save(character);
        }

        actionLog.add("던전에서 나왔습니다.", LOG_TYPE_DUNGEON);
    }

    /**
     * 던전 내에서 대상 방으로의 이동을 검증하고 수행합니다.
     *
     * <p>백트래킹 규칙: 현재 방이 미클리어 상태일 때 이미 클리어된 방으로의 후퇴만 허용되며, 미클리어 방으로의 전진은 차단됩니다.
     *
     * @param characterId 이동할 캐릭터 ID
     * @param targetRoomId 이동할 대상 방 ID
     * @return 갱신된 던전 인스턴스
     * @throws BlockedMovementException 이동이 연결되어 있지 않거나 미클리어 방 전진 시
     */
    @Transactional
    public DungeonInstance moveToRoom(final Long characterId, final String targetRoomId) {
        final DungeonProgressEntity entity =
                dungeonProgressRepository
                        .findByCharacterId(characterId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "진행 중인 던전이 없습니다: characterId=" + characterId));

        final DungeonInstance instance = restoreInstance(entity);
        final String currentRoomId = instance.currentRoomId();
        final MapNode currentNode =
                instance.dungeonGraph()
                        .byId(currentRoomId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "현재 방을 그래프에서 찾을 수 없습니다: " + currentRoomId));

        if (!currentNode.links().contains(targetRoomId)) {
            throw new BlockedMovementException(UNLINKED_ROOM_MESSAGE);
        }

        final boolean currentCleared = instance.isRoomCleared(currentRoomId);
        if (!currentCleared) {
            final boolean targetCleared = instance.isRoomCleared(targetRoomId);
            if (!targetCleared) {
                throw new BlockedMovementException(BLOCKED_FORWARD_MESSAGE);
            }
        }

        instance.moveTo(targetRoomId);

        entity.setCurrentRoomId(targetRoomId);
        entity.setRoomStatesJson(serializeRoomStates(instance.roomStates()));
        dungeonProgressRepository.save(entity);

        final CharacterProgress character = loadCharacterOrThrow(characterId);
        character.updateCurrentNodeId(targetRoomId);
        characterProgressRepository.save(character);

        return instance;
    }

    /**
     * 몬스터 처치 시 현재 방의 잔여 몬스터 목록을 갱신하고 방 클리어 여부를 판정합니다.
     *
     * @param characterId 캐릭터 ID
     * @param monsterId 격파된 몬스터 ID
     */
    @Transactional
    public void onMonsterDefeated(final Long characterId, final String monsterId) {
        final Optional<DungeonProgressEntity> entityOpt =
                dungeonProgressRepository.findByCharacterId(characterId);
        if (entityOpt.isEmpty()) {
            return;
        }

        final DungeonProgressEntity entity = entityOpt.get();
        final DungeonInstance instance = restoreInstance(entity);
        final String currentRoomId = instance.currentRoomId();

        instance.removeMonster(currentRoomId, monsterId);

        entity.setRoomStatesJson(serializeRoomStates(instance.roomStates()));
        dungeonProgressRepository.save(entity);
    }

    /**
     * 보스 몬스터 처치 시 확정 클리어 보상을 지급하고 던전 진행 엔티티를 삭제한 뒤 던전 입구로 복귀합니다.
     *
     * @param characterId 캐릭터 ID
     * @return 던전 클리어 결과 DTO
     */
    @Transactional
    public DungeonClearResult onBossDefeated(final Long characterId) {
        final DungeonProgressEntity entity =
                dungeonProgressRepository
                        .findByCharacterId(characterId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "진행 중인 던전이 없습니다: characterId=" + characterId));

        final DungeonSpec spec = dungeonSpecRepository.getById(entity.getDungeonId());
        final DungeonRewardSpec reward = spec.rewards();
        final CharacterProgress character = loadCharacterOrThrow(characterId);

        if (reward.exp() > 0) {
            progressionService.gainExperience(character, reward.exp());
        }
        if (reward.gold() > 0) {
            character.gainGold(reward.gold());
        }
        final List<DroppedItem> droppedItems =
                monsterRewardService != null
                        ? monsterRewardService.rollItemDrops(reward.itemDrops())
                        : List.of();

        for (final DroppedItem item : droppedItems) {
            if (character == null || character.getId() == null || character.getId().equals(1L)) {
                inventoryService.acquireItem(item.itemId(), item.quantity());
            } else {
                inventoryService.acquireItem(character.getId(), item.itemId(), item.quantity());
            }
        }

        actionLog.add(spec.name() + "을(를) 완전히 정복했습니다!", LOG_TYPE_DUNGEON);
        actionLog.add(
                "던전 클리어 보상: EXP +" + reward.exp() + ", Gold +" + reward.gold() + "G",
                LOG_TYPE_DUNGEON);

        for (final DroppedItem item : droppedItems) {
            final String itemName =
                    itemCatalogService != null
                            ? itemCatalogService
                                    .byId(item.itemId())
                                    .map(com.myapps.web.myrpg.domain.model.Item::name)
                                    .orElse(item.itemId())
                            : item.itemId();
            actionLog.add("보상 획득: " + itemName + " x" + item.quantity(), LOG_TYPE_DUNGEON);
        }

        dungeonProgressRepository.delete(entity);

        character.updateCurrentNodeId(entity.getEntranceNodeId());
        characterProgressRepository.save(character);

        return new DungeonClearResult(
                spec.id(), spec.name(), reward.exp(), reward.gold(), droppedItems);
    }

    /**
     * 캐릭터의 활성 던전 인스턴스를 DB에서 복원하여 조회합니다.
     *
     * @param characterId 조회할 캐릭터 ID
     * @return 복원된 DungeonInstance, 없으면 빈 Optional
     */
    @Transactional(readOnly = true)
    public Optional<DungeonInstance> getActiveDungeon(final Long characterId) {
        final Optional<DungeonProgressEntity> entityOpt =
                dungeonProgressRepository.findByCharacterId(characterId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(restoreInstance(entityOpt.get()));
    }

    /**
     * 플레이어가 던전 내에서 사망했을 때 던전 인스턴스를 소멸시키고 마을 리스폰을 처리합니다.
     *
     * @param characterId 사망한 캐릭터 ID
     */
    @Transactional
    public void handlePlayerDeath(final Long characterId) {
        dungeonProgressRepository.deleteByCharacterId(characterId);
        final Optional<CharacterProgress> charOpt =
                characterProgressRepository.findById(characterId);
        if (charOpt.isPresent()) {
            final CharacterProgress character = charOpt.get();
            progressionService.die(character);
            characterProgressRepository.save(character);
        }
    }

    /**
     * 던전 엔티티로부터 도메인 던전 인스턴스를 복원합니다.
     *
     * @param entity 던전 진행 엔티티
     * @return 역직렬화된 DungeonInstance
     */
    public DungeonInstance restoreInstance(final DungeonProgressEntity entity) {
        final MapGraph graph =
                deserializeDungeonGraph(entity.getDungeonGraphJson(), entity.getStartRoomId());
        final Map<String, DungeonRoomState> roomStates =
                deserializeRoomStates(entity.getRoomStatesJson());
        return new DungeonInstance(
                entity.getCharacterId(),
                entity.getDungeonId(),
                entity.getEntranceNodeId(),
                entity.getStartRoomId(),
                entity.getBossRoomId(),
                entity.getCurrentRoomId(),
                graph,
                roomStates);
    }

    /**
     * 던전 맵 그래프를 JSON 문자열로 직렬화합니다.
     *
     * @param graph 직렬화할 맵 그래프
     * @return 직렬화된 JSON 문자열
     */
    public String serializeDungeonGraph(final MapGraph graph) {
        try {
            return objectMapper.writeValueAsString(graph.nodes());
        } catch (final RuntimeException exception) {
            throw new IllegalStateException("던전 그래프 직렬화 실패", exception);
        }
    }

    /**
     * JSON 문자열로부터 던전 맵 그래프를 역직렬화합니다.
     *
     * @param json 직렬화된 JSON 문자열
     * @param startRoomId 시작방 ID
     * @return 복원된 맵 그래프
     */
    public MapGraph deserializeDungeonGraph(final String json, final String startRoomId) {
        try {
            final JsonNode root = objectMapper.readTree(json);
            final List<MapNode> nodes = new ArrayList<>();
            for (final JsonNode nodeJson : root) {
                final String id = nodeJson.get("id").asText();
                final String name = nodeJson.get("name").asText();
                final String type = nodeJson.get("type").asText();
                final NodeType nodeType = NodeType.fromType(type).orElse(null);
                final int x = nodeJson.get("x").asInt();
                final int y = nodeJson.get("y").asInt();
                final String dungeonId =
                        nodeJson.has("dungeonId") && !nodeJson.get("dungeonId").isNull()
                                ? nodeJson.get("dungeonId").asText()
                                : null;
                final String theme =
                        nodeJson.has("theme") && !nodeJson.get("theme").isNull()
                                ? nodeJson.get("theme").asText()
                                : null;
                final List<String> links = parseStringArray(nodeJson.get("links"));
                final List<String> monsters = parseStringArray(nodeJson.get("monsters"));
                nodes.add(
                        new MapNode(
                                id, name, type, nodeType, x, y, dungeonId, theme, links, monsters));
            }
            return new MapGraph(nodes, List.of(), startRoomId);
        } catch (final RuntimeException exception) {
            throw new IllegalStateException("던전 그래프 역직렬화 실패", exception);
        }
    }

    /**
     * 각 방의 상태 맵을 JSON 문자열로 직렬화합니다.
     *
     * @param roomStates 직렬화할 방 상태 맵
     * @return 직렬화된 JSON 문자열
     */
    public String serializeRoomStates(final Map<String, DungeonRoomState> roomStates) {
        try {
            return objectMapper.writeValueAsString(roomStates.values());
        } catch (final RuntimeException exception) {
            throw new IllegalStateException("방 상태 직렬화 실패", exception);
        }
    }

    /**
     * JSON 문자열로부터 각 방의 상태 맵을 역직렬화합니다.
     *
     * @param json 직렬화된 JSON 문자열
     * @return 복원된 방 상태 맵
     */
    public Map<String, DungeonRoomState> deserializeRoomStates(final String json) {
        try {
            final JsonNode root = objectMapper.readTree(json);
            final Map<String, DungeonRoomState> map = new LinkedHashMap<>();
            for (final JsonNode stateJson : root) {
                final String roomId = stateJson.get("roomId").asText();
                final boolean cleared = stateJson.get("cleared").asBoolean();
                final boolean discovered = stateJson.get("discovered").asBoolean();
                final List<String> remainingMonsters =
                        parseStringArray(stateJson.get("remainingMonsters"));
                map.put(
                        roomId,
                        new DungeonRoomState(roomId, cleared, discovered, remainingMonsters));
            }
            return map;
        } catch (final RuntimeException exception) {
            throw new IllegalStateException("방 상태 역직렬화 실패", exception);
        }
    }

    private List<String> parseStringArray(final JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        final List<String> list = new ArrayList<>();
        for (final JsonNode elem : arrayNode) {
            list.add(elem.asText());
        }
        return List.copyOf(list);
    }

    private CharacterProgress loadCharacterOrThrow(final Long characterId) {
        return characterProgressRepository
                .findById(characterId)
                .orElseThrow(() -> new IllegalStateException("캐릭터를 찾을 수 없습니다: " + characterId));
    }
}
