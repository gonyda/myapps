package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * 2차원 격자 기반 비선형 프로시저럴 던전 맵 생성 엔진.
 *
 * <p>(0, 0) 시작방에서부터 4방향 비순환 랜덤 워크(Self-avoiding Random Walk)로 보스방까지의 주 경로를 합성하고, 주 경로 노드들에서 서브
 * 브랜치(막다른 갈림길)를 확장하여 던전 스펙에 정의된 규모의 유효 격자 그래프({@link MapGraph}) 및 {@link DungeonInstance}를 생성합니다.
 */
public class DungeonGenerator {

    private static final int MAX_RETRIES = 200;
    private static final int MAX_BRANCH_ATTEMPTS = 500;
    private static final int START_ROOM_X = 0;
    private static final int START_ROOM_Y = 0;
    private static final String START_ROOM_NAME = "시작방";
    private static final String NORMAL_ROOM_NAME = "던전 방";
    private static final String BOSS_ROOM_SUFFIX = "의 방";
    private static final String DEFAULT_BOSS_ROOM_NAME = "보스방";
    private static final String ROOM_PREFIX = "room-";
    private static final String NODE_TYPE_DUNGEON = "dungeon";
    private static final int MAX_TYPES_PER_ROOM = 2;

    private static final int[][] CARDINAL_DIRECTIONS = {
        {1, 0}, // 동 (East)
        {-1, 0}, // 서 (West)
        {0, 1}, // 남 (South)
        {0, -1} // 북 (North)
    };

    private final Random random;

    /** 기본 난수 생성기를 사용하여 던전 생성기를 생성합니다. */
    public DungeonGenerator() {
        this(new Random());
    }

    /**
     * 주입된 난수 생성기를 사용하여 던전 생성기를 생성합니다.
     *
     * @param random 난수 생성기
     */
    public DungeonGenerator(final Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * 던전 스펙에 따라 캐릭터 전용 인스턴스 던전을 프로시저럴 생성합니다.
     *
     * @param spec 던전 메타데이터 및 생성 스펙
     * @param characterId 입장하는 캐릭터 ID
     * @return 생성된 던전 인스턴스
     * @throws IllegalStateException 지정된 재시도 횟수 내에 던전 생성을 완료하지 못했을 때
     */
    public DungeonInstance generate(final DungeonSpec spec, final Long characterId) {
        return generate(spec, characterId, this.random);
    }

    /**
     * 던전 스펙과 지정된 난수 생성기에 따라 캐릭터 전용 인스턴스 던전을 프로시저럴 생성합니다.
     *
     * @param spec 던전 메타데이터 및 생성 스펙
     * @param characterId 입장하는 캐릭터 ID
     * @param rng 난수 생성기
     * @return 생성된 던전 인스턴스
     * @throws IllegalStateException 지정된 재시도 횟수 내에 던전 생성을 완료하지 못했을 때
     */
    public DungeonInstance generate(
            final DungeonSpec spec, final Long characterId, final Random rng) {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(characterId, "characterId must not be null");
        Objects.requireNonNull(rng, "rng must not be null");

        final DungeonGenerationSpec genSpec =
                Objects.requireNonNull(spec.generation(), "generation spec must not be null");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            final Optional<DungeonInstance> maybeInstance =
                    tryGenerate(spec, genSpec, characterId, rng);
            if (maybeInstance.isPresent()) {
                return maybeInstance.get();
            }
        }

        throw new IllegalStateException(
                "Failed to generate dungeon map after "
                        + MAX_RETRIES
                        + " attempts for spec: "
                        + spec.id());
    }

    private Optional<DungeonInstance> tryGenerate(
            final DungeonSpec spec,
            final DungeonGenerationSpec genSpec,
            final Long characterId,
            final Random rng) {
        final int targetDistance =
                pickInRange(genSpec.minDistanceToBoss(), genSpec.maxDistanceToBoss(), rng);
        final int minRoomsRequired = targetDistance + 1;
        final int targetTotalRooms =
                Math.max(
                        minRoomsRequired,
                        pickInRange(genSpec.minTotalRooms(), genSpec.maxTotalRooms(), rng));

        final GenerationContext ctx = new GenerationContext();
        final GridPoint startPoint = new GridPoint(START_ROOM_X, START_ROOM_Y);
        final String startRoomId = formatRoomId(START_ROOM_X, START_ROOM_Y);

        final boolean pathSuccess =
                generateMainPath(ctx, startPoint, startRoomId, targetDistance, rng);
        if (!pathSuccess) {
            return Optional.empty();
        }

        final String bossRoomId = ctx.mainPath.get(ctx.mainPath.size() - 1);

        expandSubBranches(
                ctx, bossRoomId, startRoomId, targetTotalRooms, genSpec.maxBranchDepth(), rng);
        if (ctx.roomCount() < targetTotalRooms) {
            return Optional.empty();
        }

        final Map<String, DungeonRoomState> roomStates =
                buildRoomStates(ctx, spec, startRoomId, bossRoomId, rng);
        final List<MapNode> mapNodes =
                buildMapNodes(ctx, spec, startRoomId, bossRoomId, roomStates);
        final MapGraph dungeonGraph = new MapGraph(mapNodes, List.of(), startRoomId);

        return Optional.of(
                new DungeonInstance(
                        characterId,
                        spec.id(),
                        spec.entranceNodeId(),
                        startRoomId,
                        bossRoomId,
                        startRoomId,
                        dungeonGraph,
                        roomStates));
    }

    private boolean generateMainPath(
            final GenerationContext ctx,
            final GridPoint startPoint,
            final String startRoomId,
            final int targetDistance,
            final Random rng) {
        ctx.registerRoom(startRoomId, startPoint);
        ctx.mainPath.add(startRoomId);

        GridPoint currentPoint = startPoint;
        String currentRoomId = startRoomId;

        for (int step = 0; step < targetDistance; step++) {
            final List<GridPoint> freeNeighbors =
                    findFreeCardinalNeighbors(currentPoint, ctx.roomIdByCoord);
            if (freeNeighbors.isEmpty()) {
                return false;
            }
            final GridPoint nextPoint = freeNeighbors.get(rng.nextInt(freeNeighbors.size()));
            final String nextRoomId = formatRoomId(nextPoint.x(), nextPoint.y());

            ctx.registerRoom(nextRoomId, nextPoint);
            ctx.linkRooms(currentRoomId, nextRoomId);
            ctx.mainPath.add(nextRoomId);

            currentPoint = nextPoint;
            currentRoomId = nextRoomId;
        }
        return true;
    }

    private void expandSubBranches(
            final GenerationContext ctx,
            final String bossRoomId,
            final String startRoomId,
            final int targetTotalRooms,
            final int maxBranchDepth,
            final Random rng) {
        int branchAttempts = 0;
        while (ctx.roomCount() < targetTotalRooms && branchAttempts < MAX_BRANCH_ATTEMPTS) {
            branchAttempts++;
            final String branchRootId = selectBranchRoot(ctx, bossRoomId, startRoomId, rng);
            if (branchRootId == null) {
                break;
            }

            final int remainingNeeded = targetTotalRooms - ctx.roomCount();
            final int maxDepth = Math.min(maxBranchDepth, remainingNeeded);
            final int branchDepth = maxDepth <= 1 ? 1 : rng.nextInt(maxDepth) + 1;

            growSingleBranch(ctx, branchRootId, branchDepth, targetTotalRooms, rng);
        }
    }

    private String selectBranchRoot(
            final GenerationContext ctx,
            final String bossRoomId,
            final String startRoomId,
            final Random rng) {
        final List<String> candidates = new ArrayList<>();
        for (final Map.Entry<String, GridPoint> entry : ctx.coordByRoomId.entrySet()) {
            final String rid = entry.getKey();
            if (!rid.equals(bossRoomId)
                    && !findFreeCardinalNeighbors(entry.getValue(), ctx.roomIdByCoord).isEmpty()) {
                candidates.add(rid);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        final List<String> nonStart =
                candidates.stream().filter(id -> !id.equals(startRoomId)).toList();
        return !nonStart.isEmpty()
                ? nonStart.get(rng.nextInt(nonStart.size()))
                : candidates.get(rng.nextInt(candidates.size()));
    }

    private void growSingleBranch(
            final GenerationContext ctx,
            final String rootId,
            final int depth,
            final int targetTotalRooms,
            final Random rng) {
        GridPoint currentPoint = ctx.coordByRoomId.get(rootId);
        String currentRoomId = rootId;

        for (int d = 0; d < depth; d++) {
            final List<GridPoint> free = findFreeCardinalNeighbors(currentPoint, ctx.roomIdByCoord);
            if (free.isEmpty()) {
                break;
            }
            final GridPoint nextPoint = free.get(rng.nextInt(free.size()));
            final String nextRoomId = formatRoomId(nextPoint.x(), nextPoint.y());

            ctx.registerRoom(nextRoomId, nextPoint);
            ctx.linkRooms(currentRoomId, nextRoomId);

            currentPoint = nextPoint;
            currentRoomId = nextRoomId;

            if (ctx.roomCount() >= targetTotalRooms) {
                break;
            }
        }
    }

    private Map<String, DungeonRoomState> buildRoomStates(
            final GenerationContext ctx,
            final DungeonSpec spec,
            final String startRoomId,
            final String bossRoomId,
            final Random rng) {
        final Set<String> startAdjacent = ctx.linksByRoomId.get(startRoomId);
        final Map<String, DungeonRoomState> roomStates = new LinkedHashMap<>();

        for (final String roomId : ctx.coordByRoomId.keySet()) {
            final boolean isStart = roomId.equals(startRoomId);
            final boolean isBoss = roomId.equals(bossRoomId);

            final boolean cleared = isStart;
            final boolean discovered = isStart || startAdjacent.contains(roomId);
            final List<String> monsters = resolveMonsters(spec, isStart, isBoss, rng);

            roomStates.put(roomId, new DungeonRoomState(roomId, cleared, discovered, monsters));
        }
        return roomStates;
    }

    private List<String> resolveMonsters(
            final DungeonSpec spec, final boolean isStart, final boolean isBoss, final Random rng) {
        if (isStart) {
            return List.of();
        }
        if (isBoss) {
            return spec.boss() != null && spec.boss().monsterId() != null
                    ? List.of(spec.boss().monsterId())
                    : List.of();
        }
        return spawnMonstersForRoom(spec.monsterPool(), rng);
    }

    private List<MapNode> buildMapNodes(
            final GenerationContext ctx,
            final DungeonSpec spec,
            final String startRoomId,
            final String bossRoomId,
            final Map<String, DungeonRoomState> roomStates) {
        final List<MapNode> nodes = new ArrayList<>();
        for (final Map.Entry<String, GridPoint> entry : ctx.coordByRoomId.entrySet()) {
            final String roomId = entry.getKey();
            final GridPoint pt = entry.getValue();

            final String nodeName = resolveNodeName(spec, roomId, startRoomId, bossRoomId);
            final List<String> sortedLinks = new ArrayList<>(ctx.linksByRoomId.get(roomId));
            Collections.sort(sortedLinks);

            final DungeonRoomState state = roomStates.get(roomId);
            final List<String> monsters = state != null ? state.remainingMonsters() : List.of();

            nodes.add(
                    new MapNode(
                            roomId,
                            nodeName,
                            NODE_TYPE_DUNGEON,
                            NodeType.DUNGEON,
                            pt.x(),
                            pt.y(),
                            spec.id(),
                            spec.theme(),
                            sortedLinks,
                            monsters));
        }
        return nodes;
    }

    private String resolveNodeName(
            final DungeonSpec spec,
            final String roomId,
            final String startRoomId,
            final String bossRoomId) {
        if (roomId.equals(startRoomId)) {
            return START_ROOM_NAME;
        }
        if (roomId.equals(bossRoomId)) {
            return spec.boss() != null && spec.boss().name() != null
                    ? spec.boss().name() + BOSS_ROOM_SUFFIX
                    : DEFAULT_BOSS_ROOM_NAME;
        }
        return NORMAL_ROOM_NAME;
    }

    private List<GridPoint> findFreeCardinalNeighbors(
            final GridPoint current, final Map<GridPoint, String> occupied) {
        final List<GridPoint> free = new ArrayList<>(4);
        for (final int[] dir : CARDINAL_DIRECTIONS) {
            final GridPoint neighbor = new GridPoint(current.x() + dir[0], current.y() + dir[1]);
            if (!occupied.containsKey(neighbor)) {
                free.add(neighbor);
            }
        }
        return free;
    }

    private List<String> spawnMonstersForRoom(
            final List<DungeonMonsterEntry> monsterPool, final Random rng) {
        if (monsterPool == null || monsterPool.isEmpty()) {
            return List.of();
        }
        final int totalWeight = monsterPool.stream().mapToInt(DungeonMonsterEntry::weight).sum();
        if (totalWeight <= 0) {
            return List.of();
        }

        final int typesCount = monsterPool.size() > 1 && rng.nextBoolean() ? MAX_TYPES_PER_ROOM : 1;
        final List<String> monsters = new ArrayList<>();
        for (int t = 0; t < typesCount; t++) {
            final DungeonMonsterEntry entry = selectMonsterByWeight(monsterPool, totalWeight, rng);
            if (entry != null) {
                final int min = Math.max(1, entry.minCount());
                final int max = Math.max(min, entry.maxCount());
                final int count = min == max ? min : min + rng.nextInt(max - min + 1);
                for (int i = 0; i < count; i++) {
                    monsters.add(entry.monsterId());
                }
            }
        }
        return List.copyOf(monsters);
    }

    private DungeonMonsterEntry selectMonsterByWeight(
            final List<DungeonMonsterEntry> monsterPool, final int totalWeight, final Random rng) {
        final int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (final DungeonMonsterEntry entry : monsterPool) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return monsterPool.get(monsterPool.size() - 1);
    }

    private static String formatRoomId(final int x, final int y) {
        return ROOM_PREFIX + x + "-" + y;
    }

    private static int pickInRange(final int min, final int max, final Random rng) {
        if (min >= max) {
            return min;
        }
        return min + rng.nextInt(max - min + 1);
    }

    private static final class GenerationContext {
        private final Map<String, GridPoint> coordByRoomId = new LinkedHashMap<>();
        private final Map<GridPoint, String> roomIdByCoord = new HashMap<>();
        private final Map<String, Set<String>> linksByRoomId = new HashMap<>();
        private final List<String> mainPath = new ArrayList<>();

        void registerRoom(final String roomId, final GridPoint point) {
            coordByRoomId.put(roomId, point);
            roomIdByCoord.put(point, roomId);
            linksByRoomId.put(roomId, new LinkedHashSet<>());
        }

        void linkRooms(final String room1, final String room2) {
            linksByRoomId.get(room1).add(room2);
            linksByRoomId.get(room2).add(room1);
        }

        int roomCount() {
            return coordByRoomId.size();
        }
    }

    private record GridPoint(int x, int y) {}
}
