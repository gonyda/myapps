package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.MonsterRewardService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** 몬스터 처치 보상, 아이템 드랍, 던전 진행(보스/일반/연쇄 조우) 및 플레이어 사망 결산을 담당하는 협력 객체. */
public final class BattleRewardService {

    private static final int PERCENT_DIVISOR = 100;
    private static final String LOG_TYPE_GROWTH = "growth";

    private final MonsterRewardService monsterRewardService;
    private final InventoryService inventoryService;
    private final ProgressionService progressionService;
    private final DungeonService dungeonService;
    private final ItemCatalogService itemCatalogService;
    private final GameMessageService gameMessageService;
    private final ActionLog actionLog;
    private final Random random;

    public BattleRewardService(
            final MonsterRewardService monsterRewardService,
            final InventoryService inventoryService,
            final ProgressionService progressionService,
            final DungeonService dungeonService,
            final ItemCatalogService itemCatalogService,
            final GameMessageService gameMessageService,
            final ActionLog actionLog,
            final Random random) {
        this.monsterRewardService = monsterRewardService;
        this.inventoryService = inventoryService;
        this.progressionService = progressionService;
        this.dungeonService = dungeonService;
        this.itemCatalogService = itemCatalogService;
        this.gameMessageService = gameMessageService;
        this.actionLog = actionLog;
        this.random = random;
    }

    /** 처치 보상(드랍/골드/경험치/레벨업)을 정산하고 메시지를 생성한다. */
    public DropResult processKillReward(
            final CharacterProgress progress, final Monster monster, final List<String> logLines) {
        final DropResult drop = monsterRewardService.rollDrop(monster);
        inventoryService.acquire(progress, drop);
        final LevelUpResult levelUpResult =
                progressionService.gainExperience(progress, monster.experience());

        final String victoryRewardMsg =
                gameMessageService.get("battle.victory_reward", monster.experience(), drop.gold());
        final StringBuilder sb = new StringBuilder(victoryRewardMsg);

        if (!drop.items().isEmpty()) {
            final List<String> itemSummaries = new ArrayList<>();
            for (final DroppedItem item : drop.items()) {
                final String itemName = resolveItemName(item.itemId());
                itemSummaries.add(itemName + " x" + item.quantity());
            }
            sb.append(" | ").append(String.join(", ", itemSummaries));
        }
        logLines.add(sb.toString());

        if (levelUpResult != null && levelUpResult.levelsGained() > 0) {
            final String levelUpMsg =
                    gameMessageService.get(
                            "log.growth.levelup",
                            levelUpResult.newLevel(),
                            levelUpResult.levelsGained());
            actionLog.add(levelUpMsg, LOG_TYPE_GROWTH);
        }

        return drop;
    }

    /** 아이템 ID에 대응하는 이름을 카탈로그에서 해석한다. */
    public String resolveItemName(final String itemId) {
        return itemCatalogService.byId(itemId).map(Item::name).orElse(itemId);
    }

    /** 몬스터 처치 시 던전 진행(보스 처치, 몬스터 차감, 연쇄 조우) 또는 일반 처치 보상을 처리한다. */
    public KillResolution handleMonsterKilled(
            final CharacterProgress progress,
            final BattleState state,
            final Monster monster,
            final List<String> combatLines,
            final List<String> settlementLines) {
        final Long characterId =
                progress.getId() != null ? progress.getId() : state.getCharacterId();

        final Optional<DungeonInstance> dungeonOpt =
                dungeonService != null
                        ? dungeonService.getActiveDungeon(characterId)
                        : Optional.empty();

        if (dungeonOpt.isPresent()) {
            final DungeonInstance dungeon = dungeonOpt.get();
            final boolean isBoss =
                    dungeon.bossRoomId().equals(dungeon.currentRoomId())
                            || monster.type() == MonsterType.BOSS
                            || "giant-spider".equals(monster.id());

            if (isBoss) {
                final DungeonClearResult clearResult = dungeonService.onBossDefeated(characterId);
                state.setActive(false);
                return new KillResolution(Outcome.WIN, null, clearResult);
            }

            if (!state.isDungeonMonsterDeducted()) {
                dungeonService.onMonsterDefeated(characterId, monster.id());
                state.setDungeonMonsterDeducted(true);
            }

            final boolean chainCombat = random.nextInt(PERCENT_DIVISOR) < 10;
            if (chainCombat) {
                state.setMonsterCurrentHp(monster.maxHp());
                state.setTurnCount(1);
                state.setActive(true);
                final String chainMsg = "🚨 [연쇄 조우] " + monster.name() + " 무리가 추가로 기습!";
                combatLines.add(chainMsg);
                settlementLines.add(chainMsg);
                return new KillResolution(Outcome.NONE, null, null);
            }
        }

        final DropResult drop = processKillReward(progress, monster, settlementLines);
        final String monsterSlainMsg =
                gameMessageService.get("battle.monster_slain", monster.name());
        combatLines.add(monsterSlainMsg);
        state.setActive(false);
        return new KillResolution(Outcome.WIN, drop, null);
    }

    /** 플레이어 사망 시 던전 사망 처리 및 사망 페널티(경험치 감소)를 적용한다. */
    public Outcome handleDeath(
            final CharacterProgress progress,
            final BattleState state,
            final List<String> logLines) {
        if (dungeonService != null) {
            final Long characterId =
                    progress.getId() != null ? progress.getId() : state.getCharacterId();
            dungeonService.handlePlayerDeath(characterId);
        }
        final DeathResult deathResult = progressionService.die(progress);
        state.setActive(false);
        final String deathMsg =
                gameMessageService.get("battle.death", deathResult.experienceLost());
        logLines.add(deathMsg);
        return Outcome.LOSE;
    }
}
