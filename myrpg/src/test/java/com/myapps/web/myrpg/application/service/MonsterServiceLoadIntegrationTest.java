package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * 실제 {@code data/monster.json} 및 {@code data/map.json} 로딩 통합 테스트.
 *
 * <p>Spring Boot 컨텍스트 전체를 기동하여 {@link MonsterService}가 클래스패스 리소스를 정상 로드하고 교차검증을 통과하는지 검증합니다.
 *
 * <p><b>Validates: Requirements 1.1, 8.5, 10.1, 10.2</b>
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MonsterServiceLoadIntegrationTest {

    private static final int TOTAL_MONSTER_COUNT = 6;
    private static final String RACCOON_ID = "raccoon";
    private static final String RACCOON_NAME = "너구리";
    private static final int RACCOON_LEVEL = 1;
    private static final int RACCOON_MAX_HP = 45;
    private static final int RACCOON_ATTACK_POWER = 42;
    private static final int RACCOON_DEFENSE = 3;
    private static final int RACCOON_CRITICAL = 30;
    private static final long RACCOON_EXPERIENCE = 22L;
    private static final int RACCOON_GOLD_DROP_MIN = 6;
    private static final int RACCOON_GOLD_DROP_MAX = 16;
    private static final int RACCOON_ITEM_DROPS_COUNT = 1;
    private static final int RACCOON_LINES_COUNT = 3;
    private static final String DUGALD_NORTH_NODE_ID = "dugald-north";
    private static final String TIR_CHONAILL_NODE_ID = "tir-chonaill";

    private final MonsterService monsterService;

    MonsterServiceLoadIntegrationTest(final MonsterService monsterService) {
        this.monsterService = monsterService;
    }

    /** 전체 몬스터 수가 6마리(기존 너구리 + 알비 던전 5종)인지 검증한다. */
    @Test
    @DisplayName("애플리케이션 기동 시 카탈로그에 등록된 6마리의 몬스터가 모두 정상 로드된다")
    void should_loadAllMonsters_when_applicationStarts() {
        // given - 컨텍스트 기동 완료 상태

        // when
        final List<Monster> allMonsters = monsterService.all();

        // then
        assertThat(allMonsters).hasSize(TOTAL_MONSTER_COUNT);
    }

    /** 너구리(raccoon)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("너구리(raccoon)의 기본 스탯, 보상, 대사 필드가 올바르게 로드된다")
    void should_haveCorrectRaccoonFields_when_loaded() {
        // given
        final String monsterId = RACCOON_ID;

        // when
        final Monster raccoon = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(raccoon.id()).isEqualTo(RACCOON_ID);
        assertThat(raccoon.name()).isEqualTo(RACCOON_NAME);
        assertThat(raccoon.type()).isEqualTo(MonsterType.NORMAL);
        assertThat(raccoon.level()).isEqualTo(RACCOON_LEVEL);
        assertThat(raccoon.maxHp()).isEqualTo(RACCOON_MAX_HP);
        assertThat(raccoon.attackPower()).isEqualTo(RACCOON_ATTACK_POWER);
        assertThat(raccoon.defense()).isEqualTo(RACCOON_DEFENSE);
        assertThat(raccoon.critical()).isEqualTo(RACCOON_CRITICAL);
        assertThat(raccoon.experience()).isEqualTo(RACCOON_EXPERIENCE);
        assertThat(raccoon.goldDrop().min()).isEqualTo(RACCOON_GOLD_DROP_MIN);
        assertThat(raccoon.goldDrop().max()).isEqualTo(RACCOON_GOLD_DROP_MAX);
        assertThat(raccoon.itemDrops()).hasSize(RACCOON_ITEM_DROPS_COUNT);
        assertThat(raccoon.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(raccoon.lines()).hasSize(RACCOON_LINES_COUNT);
    }

    /** 거미(spider)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("거미(spider, Lv2)의 스탯, 드랍율, 대사가 정확하게 로드된다")
    void should_haveCorrectSpiderFields_when_loaded() {
        // given
        final String monsterId = "spider";

        // when
        final Monster spider = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(spider.id()).isEqualTo("spider");
        assertThat(spider.name()).isEqualTo("거미");
        assertThat(spider.type()).isEqualTo(MonsterType.NORMAL);
        assertThat(spider.level()).isEqualTo(2);
        assertThat(spider.maxHp()).isEqualTo(65);
        assertThat(spider.attackPower()).isEqualTo(48);
        assertThat(spider.defense()).isEqualTo(4);
        assertThat(spider.critical()).isEqualTo(30);
        assertThat(spider.experience()).isEqualTo(30L);
        assertThat(spider.goldDrop().min()).isEqualTo(8);
        assertThat(spider.goldDrop().max()).isEqualTo(20);
        assertThat(spider.itemDrops()).hasSize(1);
        assertThat(spider.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(spider.itemDrops().getFirst().chancePercent()).isEqualTo(10);
        assertThat(spider.lines()).hasSize(3);
        assertThat(spider.defenseBlockRate()).isEqualTo(40);
        assertThat(spider.defenseCounterRate()).isEqualTo(30);
    }

    /** 붉은거미(red-spider)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("붉은거미(red-spider, Lv3)의 스탯, 드랍율, 대사가 정확하게 로드된다")
    void should_haveCorrectRedSpiderFields_when_loaded() {
        // given
        final String monsterId = "red-spider";

        // when
        final Monster redSpider = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(redSpider.id()).isEqualTo("red-spider");
        assertThat(redSpider.name()).isEqualTo("붉은거미");
        assertThat(redSpider.type()).isEqualTo(MonsterType.NORMAL);
        assertThat(redSpider.level()).isEqualTo(3);
        assertThat(redSpider.maxHp()).isEqualTo(80);
        assertThat(redSpider.attackPower()).isEqualTo(50);
        assertThat(redSpider.defense()).isEqualTo(5);
        assertThat(redSpider.critical()).isEqualTo(30);
        assertThat(redSpider.experience()).isEqualTo(40L);
        assertThat(redSpider.goldDrop().min()).isEqualTo(12);
        assertThat(redSpider.goldDrop().max()).isEqualTo(25);
        assertThat(redSpider.itemDrops()).hasSize(1);
        assertThat(redSpider.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(redSpider.itemDrops().getFirst().chancePercent()).isEqualTo(15);
        assertThat(redSpider.lines()).hasSize(3);
    }

    /** 고블린(goblin)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("고블린(goblin, Lv3)의 스탯, 드랍율, 대사가 정확하게 로드된다")
    void should_haveCorrectGoblinFields_when_loaded() {
        // given
        final String monsterId = "goblin";

        // when
        final Monster goblin = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(goblin.id()).isEqualTo("goblin");
        assertThat(goblin.name()).isEqualTo("고블린");
        assertThat(goblin.type()).isEqualTo(MonsterType.NORMAL);
        assertThat(goblin.level()).isEqualTo(3);
        assertThat(goblin.maxHp()).isEqualTo(70);
        assertThat(goblin.attackPower()).isEqualTo(54);
        assertThat(goblin.defense()).isEqualTo(3);
        assertThat(goblin.critical()).isEqualTo(30);
        assertThat(goblin.experience()).isEqualTo(42L);
        assertThat(goblin.goldDrop().min()).isEqualTo(15);
        assertThat(goblin.goldDrop().max()).isEqualTo(30);
        assertThat(goblin.itemDrops()).hasSize(1);
        assertThat(goblin.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(goblin.itemDrops().getFirst().chancePercent()).isEqualTo(20);
        assertThat(goblin.lines()).hasSize(3);
    }

    /** 검은거미(black-spider)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("검은거미(black-spider, Lv4)의 스탯, 드랍율, 대사가 정확하게 로드된다")
    void should_haveCorrectBlackSpiderFields_when_loaded() {
        // given
        final String monsterId = "black-spider";

        // when
        final Monster blackSpider = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(blackSpider.id()).isEqualTo("black-spider");
        assertThat(blackSpider.name()).isEqualTo("검은거미");
        assertThat(blackSpider.type()).isEqualTo(MonsterType.NORMAL);
        assertThat(blackSpider.level()).isEqualTo(4);
        assertThat(blackSpider.maxHp()).isEqualTo(100);
        assertThat(blackSpider.attackPower()).isEqualTo(56);
        assertThat(blackSpider.defense()).isEqualTo(6);
        assertThat(blackSpider.critical()).isEqualTo(40);
        assertThat(blackSpider.experience()).isEqualTo(55L);
        assertThat(blackSpider.goldDrop().min()).isEqualTo(20);
        assertThat(blackSpider.goldDrop().max()).isEqualTo(45);
        assertThat(blackSpider.itemDrops()).hasSize(1);
        assertThat(blackSpider.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(blackSpider.itemDrops().getFirst().chancePercent()).isEqualTo(25);
        assertThat(blackSpider.lines()).hasSize(3);
    }

    /** 거대거미(giant-spider, BOSS)의 모든 필드가 기대값과 일치하는지 검증한다. */
    @Test
    @DisplayName("거대거미(giant-spider, Lv7 보스)의 스탯, 방어경감/반격율, 확정드랍, 대사가 정확하게 로드된다")
    void should_haveCorrectGiantSpiderFields_when_loaded() {
        // given
        final String monsterId = "giant-spider";

        // when
        final Monster giantSpider = monsterService.byId(monsterId).orElseThrow();

        // then
        assertThat(giantSpider.id()).isEqualTo("giant-spider");
        assertThat(giantSpider.name()).isEqualTo("거대거미");
        assertThat(giantSpider.type()).isEqualTo(MonsterType.BOSS);
        assertThat(giantSpider.level()).isEqualTo(7);
        assertThat(giantSpider.maxHp()).isEqualTo(380);
        assertThat(giantSpider.attackPower()).isEqualTo(72);
        assertThat(giantSpider.defense()).isEqualTo(12);
        assertThat(giantSpider.critical()).isEqualTo(70);
        assertThat(giantSpider.experience()).isEqualTo(350L);
        assertThat(giantSpider.goldDrop().min()).isEqualTo(150);
        assertThat(giantSpider.goldDrop().max()).isEqualTo(300);
        assertThat(giantSpider.itemDrops()).hasSize(1);
        assertThat(giantSpider.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_30");
        assertThat(giantSpider.itemDrops().getFirst().chancePercent()).isEqualTo(100);
        assertThat(giantSpider.itemDrops().getFirst().minQuantity()).isEqualTo(2);
        assertThat(giantSpider.itemDrops().getFirst().maxQuantity()).isEqualTo(3);
        assertThat(giantSpider.lines()).hasSize(3);
        assertThat(giantSpider.defenseBlockRate()).isEqualTo(60);
        assertThat(giantSpider.defenseCounterRate()).isEqualTo(50);
        assertThat(giantSpider.buttonLabel()).isEqualTo("거대거미 👑");
    }

    /** dugald-north 노드에 너구리가 배치되어 있는지 검증한다. */
    @Test
    @DisplayName("두갈드 아일 북부 노드에 너구리가 정상 배치되어 조회된다")
    void should_haveRaccoonInDugaldNorth_when_byNodeCalled() {
        // given
        final String nodeId = DUGALD_NORTH_NODE_ID;

        // when
        final List<Monster> monsters = monsterService.byNode(nodeId);

        // then
        assertThat(monsters).hasSize(1);
        assertThat(monsters.getFirst().id()).isEqualTo(RACCOON_ID);
    }

    /** tir-chonaill 노드에는 몬스터가 없음을 검증한다. */
    @Test
    @DisplayName("마을 노드(티르코네일)에는 몬스터가 없어 빈 목록이 반환된다")
    void should_returnEmptyList_when_byNodeCalledForTirChonaill() {
        // given
        final String nodeId = TIR_CHONAILL_NODE_ID;

        // when
        final List<Monster> monsters = monsterService.byNode(nodeId);

        // then
        assertThat(monsters).isEmpty();
    }

    /** 미존재 노드에 대해 빈 목록을 반환하는지 검증한다. */
    @Test
    @DisplayName("존재하지 않는 노드 ID 조회 시 빈 목록이 안전하게 반환된다")
    void should_returnEmptyList_when_byNodeCalledForUnknownNode() {
        // given
        final String unknownNodeId = "non-existent-node";

        // when
        final List<Monster> monsters = monsterService.byNode(unknownNodeId);

        // then
        assertThat(monsters).isEmpty();
    }
}
