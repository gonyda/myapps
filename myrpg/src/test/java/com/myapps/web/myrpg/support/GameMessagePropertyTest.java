package com.myapps.web.myrpg.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.context.support.ResourceBundleMessageSource;

class GameMessagePropertyTest {

    private final GameMessageService gameMessageService;
    private final List<String> messageKeys;

    GameMessagePropertyTest() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        this.gameMessageService = new GameMessageService(messageSource);

        this.messageKeys = loadAllKeys();
    }

    private static List<String> loadAllKeys() {
        final Properties properties = new Properties();
        try (InputStream in =
                GameMessagePropertyTest.class.getResourceAsStream("/messages.properties")) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load messages.properties", ex);
        }
        return new ArrayList<>(properties.stringPropertyNames());
    }

    @Provide
    Arbitrary<String> validMessageKeys() {
        return Arbitraries.of(messageKeys);
    }

    @Property(tries = 100)
    void should_alwaysReturnNonEmptyFormattedString_forAnyDefinedKey(
            @ForAll("validMessageKeys") final String key,
            @ForAll final String arg1,
            @ForAll final int arg2) {
        final String result = gameMessageService.get(key, arg1, arg2);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).isNotEqualTo(key);
    }
}
