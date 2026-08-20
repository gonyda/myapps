package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.exception.NpcDataException;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * NPC 고정 데이터 로딩 및 조회 서비스.
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/npc.json}을 1회 파싱하여 불변 {@code List<Npc>}를 구성하고, 노드별·ID별 조회
 * 기능을 제공합니다. 데이터 무결성 위반 시 {@link NpcDataException}을 발생시켜 기동을 실패시킵니다.
 */
@Service
public class NpcService {

    private static final String NPC_JSON_PATH = "data/npc.json";

    private final ObjectMapper objectMapper;
    private List<Npc> npcs;

    /**
     * NpcService를 생성합니다.
     *
     * @param objectMapper Jackson 3 ObjectMapper
     */
    public NpcService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 기동 시 NPC JSON을 로드하고 검증합니다.
     *
     * @throws NpcDataException JSON 파싱 실패 또는 데이터 무결성 위반 시
     */
    @PostConstruct
    void init() {
        try (InputStream inputStream = new ClassPathResource(NPC_JSON_PATH).getInputStream()) {
            this.npcs = loadFromStream(inputStream);
        } catch (final IOException exception) {
            throw new NpcDataException("NPC JSON 파일 로딩 실패: " + NPC_JSON_PATH, exception);
        }
    }

    /**
     * 입력 스트림에서 NPC 데이터를 파싱하고 검증하여 불변 목록으로 반환합니다.
     *
     * <p>파싱 로직이 리소스 로딩과 분리되어 있으므로, 프로퍼티 테스트에서 인메모리 데이터를 주입하여 검증할 수 있습니다.
     *
     * @param inputStream NPC JSON 데이터 입력 스트림
     * @return 검증 완료된 불변 NPC 목록 (정의 순서 보존)
     * @throws NpcDataException 파싱 실패 또는 데이터 무결성 위반 시
     */
    public List<Npc> loadFromStream(final InputStream inputStream) {
        final JsonNode rootArray = parseJson(inputStream);
        final List<Npc> parsed = parseNpcArray(rootArray);
        validate(parsed);
        return List.copyOf(parsed);
    }

    /**
     * 전체 NPC 목록을 정의 순서대로 반환합니다.
     *
     * @return 불변 NPC 목록
     */
    public List<Npc> all() {
        return npcs;
    }

    /**
     * 지정된 노드에 배치된 NPC 목록을 정의 순서대로 반환합니다.
     *
     * <p>일치하는 NPC가 없거나 미지 노드 ID인 경우 빈 목록을 반환합니다.
     *
     * @param nodeId 조회할 맵 노드 ID
     * @return 해당 노드의 NPC 목록 (정의 순서), 미일치 시 빈 목록
     */
    public List<Npc> byNode(final String nodeId) {
        if (nodeId == null) {
            return List.of();
        }
        final List<Npc> result = new ArrayList<>();
        for (final Npc npc : npcs) {
            if (nodeId.equals(npc.nodeId())) {
                result.add(npc);
            }
        }
        return List.copyOf(result);
    }

    /**
     * NPC ID로 NPC를 조회합니다.
     *
     * @param npcId 조회할 NPC ID
     * @return 대응하는 NPC를 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<Npc> byId(final String npcId) {
        if (npcId == null) {
            return Optional.empty();
        }
        for (final Npc npc : npcs) {
            if (npcId.equals(npc.id())) {
                return Optional.of(npc);
            }
        }
        return Optional.empty();
    }

    private JsonNode parseJson(final InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (final RuntimeException exception) {
            throw new NpcDataException("NPC JSON 파싱 실패", exception);
        }
    }

    private List<Npc> parseNpcArray(final JsonNode rootArray) {
        if (rootArray == null || !rootArray.isArray()) {
            throw new NpcDataException("NPC JSON 최상위 구조가 배열이 아닙니다.");
        }

        final List<Npc> result = new ArrayList<>();
        for (final JsonNode npcNode : rootArray) {
            final Npc npc = parseNpcNode(npcNode);
            result.add(npc);
        }
        return result;
    }

    private Npc parseNpcNode(final JsonNode npcNode) {
        final String id = extractRequiredField(npcNode, "id");
        final String name = extractRequiredField(npcNode, "name");
        final String typeString = extractRequiredField(npcNode, "type");
        final String nodeId = extractRequiredField(npcNode, "nodeId");
        final String personality =
                npcNode.has("personality") ? npcNode.get("personality").asText() : "";

        final NpcType npcType =
                NpcType.fromType(typeString)
                        .orElseThrow(
                                () ->
                                        new NpcDataException(
                                                "NPC '"
                                                        + id
                                                        + "'의 type '"
                                                        + typeString
                                                        + "'을(를) 분류할 수 없습니다."));

        final NpcLines lines = parseLines(npcNode, id);
        final List<String> shopItems = parseStringList(npcNode.get("shopItems"));

        return new Npc(id, name, npcType, nodeId, personality, lines, shopItems);
    }

    private String extractRequiredField(final JsonNode npcNode, final String fieldName) {
        final JsonNode fieldNode = npcNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
            final String npcId = npcNode.has("id") ? npcNode.get("id").asText() : "(unknown)";
            throw new NpcDataException(
                    "NPC '" + npcId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있습니다.");
        }
        return fieldNode.asText();
    }

    private NpcLines parseLines(final JsonNode npcNode, final String npcId) {
        final JsonNode linesNode = npcNode.get("lines");
        if (linesNode == null || linesNode.isNull()) {
            return new NpcLines(List.of(), java.util.Map.of());
        }

        final List<String> defaultLines = parseStringList(linesNode.get("default"));
        final java.util.Map<String, List<String>> byTime = parseByTimeMap(linesNode.get("byTime"));

        return new NpcLines(defaultLines, byTime);
    }

    private List<String> parseStringList(final JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        final List<String> result = new ArrayList<>();
        for (final JsonNode element : arrayNode) {
            result.add(element.asText());
        }
        return List.copyOf(result);
    }

    private java.util.Map<String, List<String>> parseByTimeMap(final JsonNode byTimeNode) {
        if (byTimeNode == null || !byTimeNode.isObject()) {
            return java.util.Map.of();
        }
        final java.util.Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        for (final String key : byTimeNode.propertyNames()) {
            final List<String> values = parseStringList(byTimeNode.get(key));
            result.put(key, values);
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private void validate(final List<Npc> npcList) {
        final Set<String> ids = new HashSet<>();
        for (final Npc npc : npcList) {
            if (!ids.add(npc.id())) {
                throw new NpcDataException("NPC id '" + npc.id() + "'이(가) 중복됩니다.");
            }
        }
    }
}
