package com.myapps.web.myrpg.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class GameMessageServiceTest {

    private GameMessageService gameMessageService;

    @BeforeEach
    void setUp() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        gameMessageService = new GameMessageService(messageSource);
    }

    @Test
    @DisplayName("정의된 키와 인자가 전달되면 정상적으로 치환된 메시지를 반환한다")
    void should_returnFormattedMessage_when_validKeyAndArgsProvided() {
        final String gatheringMsg = gameMessageService.get("log.gathering.success", "단단한 장작");
        assertThat(gatheringMsg).isEqualTo("[채집] 단단한 장작 획득!");

        final String levelupMsg = gameMessageService.get("log.growth.levelup", 5, 10);
        assertThat(levelupMsg).isEqualTo("🎉 레벨업! Lv.5 달성! (AP +10)");
    }

    @Test
    @DisplayName("인자가 없는 키의 경우 원본 문구를 그대로 반환한다")
    void should_returnExactMessage_when_noArgsRequired() {
        final String fleeMsg = gameMessageService.get("battle.flee.success");
        assertThat(fleeMsg).isEqualTo("도망쳤다!");
    }

    @Test
    @DisplayName("존재하지 않는 키가 전달되면 예외를 던지지 않고 키 문자열 자체를 반환한다")
    void should_returnKeyName_when_keyNotFound() {
        final String unknownKey = "non.existent.message.key";
        final String result = gameMessageService.get(unknownKey, "arg1");
        assertThat(result).isEqualTo(unknownKey);
    }

    @Test
    @DisplayName("null 키가 전달되면 빈 문자열을 안전하게 반환한다")
    void should_returnEmptyString_when_nullKeyProvided() {
        final String result = gameMessageService.get(null);
        assertThat(result).isEmpty();
    }
}
