package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.WoodcutResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생활 채집(장작 패기 등) 및 필드/마을 나무 스폰 상태를 관리하는 애플리케이션 서비스.
 *
 * <p>노드 이동 시 50% 확률로 나무 오브젝트를 스폰하며, 1회 채집 시 5 SP를 소모하고 50% 확률로 장작 아이템을 획득합니다. 채집 완료 후에는 해당 노드의 나무가
 * 소멸됩니다.
 */
@Service
public class GatheringService {

    public static final int STAMINA_COST = 5;
    public static final double SPAWN_RATE = 0.50;
    public static final double SUCCESS_RATE = 0.50;
    public static final String FIREWOOD_ITEM_ID = "firewood";
    private static final String LOG_TYPE_ITEM = "item";
    private static final String LOG_TYPE_SYSTEM = "system";

    private final InventoryService inventoryService;
    private final CharacterProgressRepository characterProgressRepository;
    private final StatProgression statProgression;
    private final ActionLog actionLog;
    private final Random random;

    private final Map<Long, String> characterTreeNodes = new ConcurrentHashMap<>();

    /**
     * GatheringService를 생성합니다.
     *
     * @param inventoryService 인벤토리 서비스
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param statProgression 스탯 계산기
     * @param actionLog 행동 로그
     */
    @org.springframework.beans.factory.annotation.Autowired
    public GatheringService(
            final InventoryService inventoryService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog) {
        this(
                inventoryService,
                characterProgressRepository,
                statProgression,
                actionLog,
                new Random());
    }

    /**
     * 테스트용 Random 주입 생성자.
     *
     * @param inventoryService 인벤토리 서비스
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param statProgression 스탯 계산기
     * @param actionLog 행동 로그
     * @param random 난수 발생기
     */
    public GatheringService(
            final InventoryService inventoryService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final Random random) {
        this.inventoryService = inventoryService;
        this.characterProgressRepository = characterProgressRepository;
        this.statProgression = statProgression;
        this.actionLog = actionLog;
        this.random = random;
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
     * 노드 이동 시 나무 스폰 여부를 50% 확률로 롤하고 상태를 갱신합니다.
     *
     * <p>던전 노드는 스폰되지 않으며, 마을이나 자유필드 노드에서만 50% 확률로 스폰됩니다.
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

        final boolean spawned = random.nextDouble() < SPAWN_RATE;
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
     * <p>1. 스태미나 5 SP 차감<br>
     * 2. 노드 나무 즉시 소멸<br>
     * 3. 50% 확률 판정 및 인벤토리 지급 / 로그 출력
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

        if (progress.getStaminaCurrent() < STAMINA_COST) {
            return new WoodcutResult(
                    false,
                    "스태미나가 부족합니다 (필요: " + STAMINA_COST + " SP)",
                    null,
                    progress.getStaminaCurrent(),
                    vitalMax.stamina());
        }

        progress.spendStamina(STAMINA_COST);
        characterTreeNodes.remove(charId);

        final boolean success = random.nextDouble() < SUCCESS_RATE;
        final String message;
        final String itemId;

        if (success) {
            inventoryService.acquireItem(charId, FIREWOOD_ITEM_ID, 1);
            actionLog.add("[채집] 🪵 단단한 장작을 1개 얻었습니다!", LOG_TYPE_ITEM);
            message = "🪵 단단한 장작을 1개 획득했습니다!";
            itemId = FIREWOOD_ITEM_ID;
        } else {
            actionLog.add("[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다.", LOG_TYPE_SYSTEM);
            message = "💨 헛도끼질을 하여 장작을 얻지 못했습니다.";
            itemId = null;
        }

        characterProgressRepository.save(progress);

        return new WoodcutResult(
                success, message, itemId, progress.getStaminaCurrent(), vitalMax.stamina());
    }
}
