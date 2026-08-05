package com.myapps.web.myrpg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import com.myapps.web.myrpg.domain.model.AmbienceData;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시각/JS 보존 및 JSON 로딩 통합 테스트.
 *
 * <p>CSS 디자인 토큰(Req 1.2), JavaScript 줌/팬/팝업 함수(Req 1.3),
 * fragment 존재와 {@code play.html} {@code th:replace} 조합,
 * 그리고 Jackson 3을 통한 {@code map.json}/{@code ambience.json} 역직렬화 및
 * 양방향 링크 무결성(Req 4.5)을 통합 검증합니다.
 *
 * <p>Validates: Requirements 1.2, 1.3, 4.5
 */
@SpringBootTest
class VisualJsPreservationAndJsonLoadingIntegrationTest {

    private static final String CSS_PATH = "static/css/myrpg.css";
    private static final String JS_PATH = "static/js/myrpg.js";
    private static final String PLAY_HTML_PATH = "templates/play.html";
    private static final String MAP_JSON_PATH = "data/map.json";
    private static final String AMBIENCE_JSON_PATH = "data/ambience.json";

    private static final List<String> EXPECTED_FRAGMENTS = List.of(
            "templates/fragments/top-bar.html",
            "templates/fragments/left-sidebar.html",
            "templates/fragments/center.html",
            "templates/fragments/minimap.html",
            "templates/fragments/move-pad.html",
            "templates/fragments/action-log.html",
            "templates/fragments/panel-popup.html",
            "templates/fragments/full-map.html",
            "templates/fragments/move-response.html"
    );

    private static final List<String> CSS_DESIGN_TOKENS = List.of(
            "--gap", "--pad", "--radius",
            "--fs-xs", "--fs-sm", "--fs-md", "--fs-lg",
            "--touch", "--bar-h"
    );

    private static final List<String> JS_ZOOM_PAN_FUNCTIONS = List.of(
            "applyMapTransform", "zoomAt", "mapZoom"
    );

    private static final List<String> JS_POPUP_FUNCTIONS = List.of(
            "openPanel", "closePanel", "openMap", "closeMap"
    );

    // ========== Req 1.2: CSS 디자인 토큰 보존 ==========

    /**
     * CSS 파일이 :root 디자인 토큰을 포함하는지 검증한다.
     */
    @Test
    void should_containDesignTokens_when_cssMyrpgLoaded() throws IOException {
        final String css = loadClasspathResource(CSS_PATH);

        assertThat(css).contains(":root");
        for (final String token : CSS_DESIGN_TOKENS) {
            assertThat(css)
                    .as("CSS 디자인 토큰 '%s' 존재 확인", token)
                    .contains(token);
        }
    }

    // ========== Req 1.3: JavaScript 줌/팬/팝업 함수 보존 ==========

    /**
     * JS 파일이 줌/팬 함수를 보존하는지 검증한다.
     */
    @Test
    void should_containZoomPanFunctions_when_jsMyrpgLoaded() throws IOException {
        final String js = loadClasspathResource(JS_PATH);

        for (final String function : JS_ZOOM_PAN_FUNCTIONS) {
            assertThat(js)
                    .as("줌/팬 함수 '%s' 존재 확인", function)
                    .contains(function);
        }
    }

    /**
     * JS 파일이 팝업 함수를 보존하는지 검증한다.
     */
    @Test
    void should_containPopupFunctions_when_jsMyrpgLoaded() throws IOException {
        final String js = loadClasspathResource(JS_PATH);

        for (final String function : JS_POPUP_FUNCTIONS) {
            assertThat(js)
                    .as("팝업 함수 '%s' 존재 확인", function)
                    .contains(function);
        }
    }

    // ========== Fragment 존재 및 play.html th:replace 조합 ==========

    /**
     * 모든 fragment 파일이 클래스패스에 존재하는지 검증한다.
     */
    @Test
    void should_existAllFragments_when_templateDirectoryChecked() {
        for (final String fragmentPath : EXPECTED_FRAGMENTS) {
            final ClassPathResource resource = new ClassPathResource(fragmentPath);
            assertThat(resource.exists())
                    .as("Fragment '%s' 존재 확인", fragmentPath)
                    .isTrue();
        }
    }

    /**
     * play.html이 th:replace로 fragment를 조합하는지 검증한다.
     */
    @Test
    void should_composeFragmentsWithThReplace_when_playHtmlLoaded() throws IOException {
        final String playHtml = loadClasspathResource(PLAY_HTML_PATH);

        assertThat(playHtml).contains("th:replace");
        assertThat(playHtml).contains("fragments/top-bar");
        assertThat(playHtml).contains("fragments/left-sidebar");
        assertThat(playHtml).contains("fragments/center");
        assertThat(playHtml).contains("fragments/action-log");
        assertThat(playHtml).contains("fragments/panel-popup");
        assertThat(playHtml).contains("fragments/full-map");
    }

    // ========== Req 4.5: JSON 역직렬화 및 양방향 링크 검증 ==========

    /**
     * map.json을 Jackson 3로 역직렬화하여 노드/던전 구조를 확인한다.
     */
    @Test
    void should_deserializeMapJson_when_jacksonObjectMapperUsed() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ClassPathResource resource = new ClassPathResource(MAP_JSON_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            final JsonNode root = objectMapper.readTree(inputStream);

            assertThat(root.has("nodes")).isTrue();
            assertThat(root.get("nodes").isArray()).isTrue();
            assertThat(root.get("nodes").size()).isGreaterThan(0);

            assertThat(root.has("startNodeId")).isTrue();
            assertThat(root.get("startNodeId").asText()).isEqualTo("tir-chonaill");

            assertThat(root.has("dungeons")).isTrue();
            assertThat(root.get("dungeons").isArray()).isTrue();
        }
    }

    /**
     * map.json의 모든 링크가 양방향임을 검증한다 (Req 4.5).
     */
    @Test
    void should_haveBidirectionalLinks_when_mapJsonLoaded() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ClassPathResource resource = new ClassPathResource(MAP_JSON_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            final JsonNode root = objectMapper.readTree(inputStream);
            final JsonNode nodesArray = root.get("nodes");

            final Map<String, List<String>> linkMap = new java.util.HashMap<>();
            for (final JsonNode nodeJson : nodesArray) {
                final String nodeId = nodeJson.get("id").asText();
                final JsonNode linksArray = nodeJson.get("links");
                final List<String> links = new java.util.ArrayList<>();
                if (linksArray != null && linksArray.isArray()) {
                    for (final JsonNode linkNode : linksArray) {
                        links.add(linkNode.asText());
                    }
                }
                linkMap.put(nodeId, links);
            }

            for (final Map.Entry<String, List<String>> entry : linkMap.entrySet()) {
                final String nodeId = entry.getKey();
                for (final String linkedId : entry.getValue()) {
                    assertThat(linkMap)
                            .as("링크 대상 '%s'가 맵에 존재해야 함", linkedId)
                            .containsKey(linkedId);
                    assertThat(linkMap.get(linkedId))
                            .as("양방향 링크: '%s' → '%s' 존재 시 '%s' → '%s'도 존재해야 함",
                                    nodeId, linkedId, linkedId, nodeId)
                            .contains(nodeId);
                }
            }
        }
    }

    /**
     * ambience.json을 Jackson 3로 역직렬화하여 AmbienceData 구조를 확인한다.
     */
    @Test
    void should_deserializeAmbienceJson_when_jacksonObjectMapperUsed() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ClassPathResource resource = new ClassPathResource(AMBIENCE_JSON_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            final AmbienceData ambienceData = objectMapper.readValue(inputStream, AmbienceData.class);

            assertThat(ambienceData).isNotNull();
            assertThat(ambienceData.season()).isNotEmpty();
            assertThat(ambienceData.season()).containsKeys("spring", "summer", "autumn", "winter");
            assertThat(ambienceData.timeOfDay()).isNotEmpty();
            assertThat(ambienceData.timeOfDay()).containsKeys("dawn", "morning", "afternoon",
                    "late-afternoon", "night", "late-night");
            assertThat(ambienceData.themes()).isNotEmpty();
            assertThat(ambienceData.themes()).containsKeys("town", "field", "dungeon");
        }
    }

    private String loadClasspathResource(final String path) throws IOException {
        final ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
