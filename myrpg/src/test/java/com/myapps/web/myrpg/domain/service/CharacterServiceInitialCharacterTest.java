package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.Player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CharacterService.createInitialCharacter 메서드의 단위 테스트.
 *
 * <p>초기 캐릭터 생성 시 Lv1/HP100/MP50/공10/방5/속5/치0/exp0/gold0
 * 초기값이 올바르게 설정되는지 검증한다.
 *
 * <p><b>Validates: Requirements 1.1</b>
 */
class CharacterServiceInitialCharacterTest {

    private final CharacterService characterService = new CharacterService();

    /**
     * 초기 캐릭터 생성 시 모든 스탯이 정의된 초기값과 일치하는지 검증한다.
     *
     * <p>기대 초기값: Lv1, HP 100(max 100), MP 50(max 50),
     * 공격력 10, 방어력 5, 속도 5, 치명타 0, 경험치 0, 골드 0.
     */
    @Test
    void should_have_correct_initial_stats_when_character_is_created() {
        final String name = "테스트용사";

        final Player player = characterService.createInitialCharacter(name);

        assertNotNull(player, "생성된 플레이어는 null이 아니어야 한다");
        assertEquals(name, player.getName(), "캐릭터명은 입력한 이름과 일치해야 한다");
        assertEquals(1, player.getLevel(), "초기 레벨은 1이어야 한다");
        assertEquals(100, player.getHp(), "초기 HP는 100이어야 한다");
        assertEquals(100, player.getMaxHp(), "초기 최대 HP는 100이어야 한다");
        assertEquals(50, player.getMp(), "초기 MP는 50이어야 한다");
        assertEquals(50, player.getMaxMp(), "초기 최대 MP는 50이어야 한다");
        assertEquals(10, player.getAttack(), "초기 공격력은 10이어야 한다");
        assertEquals(5, player.getDefense(), "초기 방어력은 5이어야 한다");
        assertEquals(5, player.getSpeed(), "초기 속도는 5이어야 한다");
        assertEquals(0, player.getCritical(), "초기 치명타는 0이어야 한다");
        assertEquals(0, player.getExp(), "초기 경험치는 0이어야 한다");
        assertEquals(0, player.getGold(), "초기 골드는 0이어야 한다");
    }
}
