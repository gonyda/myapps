package com.myapps.web.myrpg.application.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 {@code data/monster.json} 및 {@code data/map.json} 로딩 통합 테스트.
 *
 * <p>Spring Boot 컨텍스트 전체를 기동하여 {@link MonsterService}가
 * 클래스패스 리소스를 정상 로드하고 교차검증을 통과하는지 검증합니다.
 *
 * <p><b>Validates: Requirements 1.1, 8.5</b>
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MonsterServiceLoadIntegrationTest {

    private static final int TOTAL_MONSTER_COUNT = 1;
    private static final String RACCOON_ID = "raccoon";
    private static final String RACCOON_NAME = "너구리";
    private static final int RACCOON_LEVEL = 1;
    private static final int RACCOON_MAX_HP = 25;
    private static final int RACCOON_ATTACK_POWER = 4;
    private static final int RACCOON_DEFENSE = 1;
    private static final int RACCOON_CRITICAL = 10;
    private static final long RACCOON_EXPERIENCE = 15L;
    private static final int RACCOON_GOLD_DROP_MIN = 3;
    private static final int RACCOON_GOLD_DROP_MAX = 10;
    private static final int RACCOON_ITEM_DROPS_COUNT = 1;
    private static final int RACCOON_LINES_COUNT = 3;
    private static final String DUGALD_NORTH_NODE_ID = "dugald-north";
    private static final String TIR_CHONAILL_NODE_ID = "tir-chonaill";

    private final MonsterService monsterService;

    MonsterServiceLoadIntegrationTest(final MonsterService monsterService) {
        this.monsterService = monsterService;
    }

    /**
     * 전체 몬스터 수가 1마리(너구리)인지 검증한다.
     */
    @Test
    void should_loadOneMonster_when_applicationStarts() {
        final List<Monster> allMonsters = monsterService.all();

        assertThat(allMonsters).hasSize(TOTAL_MONSTER_COUNT);
    }

    /**
     * 너구리(raccoon)의 모든 필드가 기대값과 일치하는지 검증한다.
     */
    @Test
    void should_haveCorrectRaccoonFields_when_loaded() {
        final Monster raccoon = monsterService.byId(RACCOON_ID).orElseThrow();

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
        assertThat(raccoon.itemDrops().getFirst().itemId()).isEqualTo("hp_potion_50");
        assertThat(raccoon.lines()).hasSize(RACCOON_LINES_COUNT);
    }

    /**
     * dugald-north 노드에 너구리가 배치되어 있는지 검증한다.
     */
    @Test
    void should_haveRaccoonInDugaldNorth_when_byNodeCalled() {
        final List<Monster> monsters = monsterService.byNode(DUGALD_NORTH_NODE_ID);

        assertThat(monsters).hasSize(1);
        assertThat(monsters.getFirst().id()).isEqualTo(RACCOON_ID);
    }

    /**
     * tir-chonaill 노드에는 몬스터가 없음을 검증한다.
     */
    @Test
    void should_returnEmptyList_when_byNodeCalledForTirChonaill() {
        final List<Monster> monsters = monsterService.byNode(TIR_CHONAILL_NODE_ID);

        assertThat(monsters).isEmpty();
    }

    /**
     * 미존재 노드에 대해 빈 목록을 반환하는지 검증한다.
     */
    @Test
    void should_returnEmptyList_when_byNodeCalledForUnknownNode() {
        final List<Monster> monsters = monsterService.byNode("non-existent-node");

        assertThat(monsters).isEmpty();
    }
}
