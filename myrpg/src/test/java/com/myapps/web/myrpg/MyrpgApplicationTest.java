package com.myapps.web.myrpg;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.application.service.BattleSessionService;
import com.myapps.web.myrpg.application.service.GameSessionService;
import com.myapps.web.myrpg.application.service.MasterDataLoader;
import com.myapps.web.myrpg.domain.random.RandomSource;
import com.myapps.web.myrpg.domain.service.BattleService;
import com.myapps.web.myrpg.domain.service.CharacterService;
import com.myapps.web.myrpg.domain.service.DropService;
import com.myapps.web.myrpg.domain.service.DungeonService;
import com.myapps.web.myrpg.domain.service.EquipmentService;
import com.myapps.web.myrpg.domain.service.ShopService;
import com.myapps.web.myrpg.domain.service.StatCalculator;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.interfaces.api.GameController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * myrpg 모듈의 전체 애플리케이션 컨텍스트 기동 및 의존성 와이어링 통합 테스트.
 *
 * <p>GameController → GameSessionService/BattleSessionService → 규칙 서비스/리포지터리/MasterDataLoader
 * 전체 의존성 체인이 올바르게 연결되는지 검증한다.
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MyrpgApplicationTest {

    private final GameController gameController;
    private final GameSessionService gameSessionService;
    private final BattleSessionService battleSessionService;
    private final MasterDataLoader masterDataLoader;
    private final DungeonService dungeonService;
    private final ShopService shopService;
    private final EquipmentService equipmentService;
    private final RandomSource randomSource;
    private final BattleService battleService;
    private final CharacterService characterService;
    private final DropService dropService;
    private final StatCalculator statCalculator;

    MyrpgApplicationTest(final GameController gameController,
                         final GameSessionService gameSessionService,
                         final BattleSessionService battleSessionService,
                         final MasterDataLoader masterDataLoader,
                         final DungeonService dungeonService,
                         final ShopService shopService,
                         final EquipmentService equipmentService,
                         final RandomSource randomSource,
                         final BattleService battleService,
                         final CharacterService characterService,
                         final DropService dropService,
                         final StatCalculator statCalculator) {
        this.gameController = gameController;
        this.gameSessionService = gameSessionService;
        this.battleSessionService = battleSessionService;
        this.masterDataLoader = masterDataLoader;
        this.dungeonService = dungeonService;
        this.shopService = shopService;
        this.equipmentService = equipmentService;
        this.randomSource = randomSource;
        this.battleService = battleService;
        this.characterService = characterService;
        this.dropService = dropService;
        this.statCalculator = statCalculator;
    }

    /**
     * 애플리케이션 컨텍스트가 정상적으로 기동되는지 검증한다.
     */
    @Test
    void contextLoads() {
        // 컨텍스트 기동 자체가 성공하면 통과
    }

    /**
     * 컨트롤러 → 애플리케이션 서비스 → 도메인 서비스 → 리포지터리 전체 빈 와이어링을 검증한다.
     */
    @Test
    void should_wireAllBeans_when_contextStarts() {
        assertNotNull(gameController, "GameController 빈이 주입되어야 한다");
        assertNotNull(gameSessionService, "GameSessionService 빈이 주입되어야 한다");
        assertNotNull(battleSessionService, "BattleSessionService 빈이 주입되어야 한다");
        assertNotNull(masterDataLoader, "MasterDataLoader 빈이 주입되어야 한다");
        assertNotNull(dungeonService, "DungeonService 빈이 주입되어야 한다");
        assertNotNull(shopService, "ShopService 빈이 주입되어야 한다");
        assertNotNull(equipmentService, "EquipmentService 빈이 주입되어야 한다");
        assertNotNull(randomSource, "RandomSource 빈이 주입되어야 한다");
        assertNotNull(battleService, "BattleService 빈이 주입되어야 한다");
        assertNotNull(characterService, "CharacterService 빈이 주입되어야 한다");
        assertNotNull(dropService, "DropService 빈이 주입되어야 한다");
        assertNotNull(statCalculator, "StatCalculator 빈이 주입되어야 한다");
    }

    /**
     * MasterDataLoader가 기동 시 마스터 데이터를 정상 로딩했는지 검증한다.
     */
    @Test
    void should_loadMasterData_when_applicationStarts() {
        final List<DungeonTemplate> dungeons = masterDataLoader.allDungeons();
        assertNotNull(dungeons, "던전 목록이 null이 아니어야 한다");
        assertFalse(dungeons.isEmpty(), "던전 목록이 비어 있지 않아야 한다");
    }
}
