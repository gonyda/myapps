package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.WoodcutResult;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생활 채집(장작 패기 등) 및 필드/마을 나무 스폰 상태를 관리하는 애플리케이션 서비스.
 *
 * <p>노드 이동 시 설정된 확률로 나무 오브젝트를 스폰하며, 1회 채집 시 SP를 소모하고 확률 판정에 따라 장작 아이템을 획득합니다. 채집 완료 후에는 해당 노드의 나무가
 * 소멸됩니다.
 */
@Service
public class GatheringService {

    public static final int DEFAULT_STAMINA_COST = 5;
    public static final double DEFAULT_SPAWN_RATE = 0.50;
    public static final double DEFAULT_SUCCESS_RATE = 0.50;
    public static final String FIREWOOD_ITEM_ID = "firewood";
    private static final String LOG_TYPE_ITEM = "item";
    private static final String LOG_TYPE_SYSTEM = "system";

    private final InventoryService inventoryService;
    private final CharacterProgressRepository characterProgressRepository;
    private final StatProgression statProgression;
    private final ActionLog actionLog;
    private final GameMessageService gameMessageService;
    private final GameProperties gameProperties;
    private final Random random;

    private final Map<Long, String> characterTreeNodes = new ConcurrentHashMap<>();

    /**
     * GatheringService를 생성합니다 (Spring 빈 주입).
     *
     * @param inventoryService 인벤토리 서비스
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param statProgression 스탯 계산기
     * @param actionLog 행동 로그
     * @param gameMessageService 메시지 서비스
     * @param gameProperties 게임 설정 프로퍼티
     */
    @org.springframework.beans.factory.annotation.Autowired
    public GatheringService(
            final InventoryService inventoryService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties) {
        this(
                inventoryService,
                characterProgressRepository,
                statProgression,
                actionLog,
                gameMessageService,
                gameProperties,
                new Random());
    }

    /**
     * 테스트용 전체 인자 주입 생성자.
     *
     * @param inventoryService 인벤토리 서비스
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param statProgression 스탯 계산기
     * @param actionLog 행동 로그
     * @param gameMessageService 메시지 서비스
     * @param gameProperties 게임 설정 프로퍼티
     * @param random 난수 발생기
     */
    public GatheringService(
            final InventoryService inventoryService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties,
            final Random random) {
        this.inventoryService = inventoryService;
        this.characterProgressRepository = characterProgressRepository;
        this.statProgression = statProgression;
        this.actionLog = actionLog;
        this.gameMessageService = gameMessageService;
        this.gameProperties = gameProperties;
        this.random = random;
    }

    /** 이전 호환용 생성자. */
    public GatheringService(
            final InventoryService inventoryService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final Random random) {
        this(
                inventoryService,
                characterProgressRepository,
                statProgression,
                actionLog,
                null,
                null,
                random);
    }

    /**
     * 특정 캐릭터의 현재 노드에 나무가 스폰되어 있는지 여부를 확인합니다.
     *
     * @param characterId 캐릭터 식별자
     * @param nodeId 노드 ID
     * @return 나무 존재 여부
     */
    public boolean isTreeAvailable(final Long characterId, final String nodeId) {
        if (nodeId == null) {
            return false;
        }
        final Long charId = characterId != null ? characterId : 1L;
        final String treeNode = characterTreeNodes.get(charId);
        return nodeId.equals(treeNode);
    }

    /**
     * 노드 이동 시 나무 스폰 여부를 확률로 롤하고 상태를 갱신합니다.
     *
     * <p>던전 노드는 스폰되지 않으며, 마을이나 자유필드 노드에서만 스폰됩니다.
     *
     * @param characterId 캐릭터 식별자
     * @param nodeId 대상 노드 ID
     * @param nodeType 노드 유형 (town, field, dungeon 등)
     * @return 스폰 성공 여부
     */
    public boolean rollTreeSpawn(
            final Long characterId, final String nodeId, final String nodeType) {
        final Long charId = characterId != null ? characterId : 1L;
        if (nodeId == null || nodeType == null || "dungeon".equalsIgnoreCase(nodeType)) {
            characterTreeNodes.remove(charId);
            return false;
        }

        final double spawnRate =
                gameProperties != null && gameProperties.gathering() != null
                        ? gameProperties.gathering().woodcutSpawnRate() / 100.0
                        : DEFAULT_SPAWN_RATE;

        final boolean spawned = random.nextDouble() < spawnRate;
        if (spawned) {
            characterTreeNodes.put(charId, nodeId);
        } else {
            characterTreeNodes.remove(charId);
        }
        return spawned;
    }

    /**
     * 캐릭터의 나무 스폰 상태를 강제로 설정합니다 (테스트 및 특정 시나리오용).
     *
     * @param characterId 캐릭터 식별자
     * @param nodeId 노드 ID (null일 경우 제거)
     */
    public void setTreeAvailable(final Long characterId, final String nodeId) {
        final Long charId = characterId != null ? characterId : 1L;
        if (nodeId != null) {
            characterTreeNodes.put(charId, nodeId);
        } else {
            characterTreeNodes.remove(charId);
        }
    }

    /**
     * 기본 캐릭터의 장작 패기 채집을 실행합니다.
     *
     * @param progress 캐릭터 진행상황
     * @return 채집 결과 DTO
     */
    @Transactional
    public WoodcutResult gatherWood(final CharacterProgress progress) {
        final Long charId = progress.getId() != null ? progress.getId() : 1L;
        return gatherWood(charId, progress);
    }

    /**
     * 특정 캐릭터의 장작 패기 채집을 실행합니다.
     *
     * <p>1. 스태미나 SP 차감<br>
     * 2. 노드 나무 즉시 소멸<br>
     * 3. 확률 판정 및 인벤토리 지급 / 로그 출력
     *
     * @param characterId 대상 캐릭터 식별자
     * @param progress 캐릭터 진행상황
     * @return 채집 결과 DTO
     */
    @Transactional
    public WoodcutResult gatherWood(final Long characterId, final CharacterProgress progress) {
        final Long charId =
                characterId != null
                        ? characterId
                        : (progress.getId() != null ? progress.getId() : 1L);
        final VitalMax vitalMax =
                statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent());

        final int staminaCost =
                gameProperties != null && gameProperties.gathering() != null
                        ? gameProperties.gathering().woodcutStaminaCost()
                        : DEFAULT_STAMINA_COST;

        if (progress.getStaminaCurrent() < staminaCost) {
            final String lackMsg =
                    gameMessageService != null
                            ? gameMessageService.get(
                                    "exception.vital.insufficient_stamina", staminaCost)
                            : "스태미나가 부족합니다 (필요: " + staminaCost + " SP)";
            return new WoodcutResult(
                    false, lackMsg, null, progress.getStaminaCurrent(), vitalMax.stamina());
        }

        progress.spendStamina(staminaCost);
        characterTreeNodes.remove(charId);

        final double successRate =
                gameProperties != null && gameProperties.gathering() != null
                        ? gameProperties.gathering().woodcutSuccessRate() / 100.0
                        : DEFAULT_SUCCESS_RATE;

        final boolean success = random.nextDouble() < successRate;
        final String message;
        final String itemId;

        if (success) {
            inventoryService.acquireItem(charId, FIREWOOD_ITEM_ID, 1);
            final String logMsg =
                    gameMessageService != null
                            ? gameMessageService.get("log.gathering.success", "단단한 장작")
                            : "[채집] 🪵 단단한 장작을 1개 얻었습니다!";
            actionLog.add(logMsg, LOG_TYPE_ITEM);
            message = logMsg;
            itemId = FIREWOOD_ITEM_ID;
        } else {
            final String logMsg =
                    gameMessageService != null
                            ? gameMessageService.get("log.gathering.failure")
                            : "[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다.";
            actionLog.add(logMsg, LOG_TYPE_SYSTEM);
            message = logMsg;
            itemId = null;
        }

        characterProgressRepository.save(progress);

        return new WoodcutResult(
                success, message, itemId, progress.getStaminaCurrent(), vitalMax.stamina());
    }
}
