package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.exception.ItemDataException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.PotionItem;
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
 * 아이템 카탈로그 로딩 및 조회 서비스.
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/item.json}을 1회 파싱하여 불변 {@code List<Item>}을 구성하고, ID별·전체 목록
 * 조회 기능을 제공합니다. 데이터 무결성 위반 시 {@link ItemDataException}을 발생시켜 기동을 실패시킵니다.
 */
@Service
public class ItemCatalogService {

    private static final String ITEM_JSON_PATH = "data/item.json";

    private final ObjectMapper objectMapper;
    private List<Item> items;

    /**
     * ItemCatalogService를 생성합니다.
     *
     * @param objectMapper Jackson 3 ObjectMapper
     */
    public ItemCatalogService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 기동 시 아이템 JSON을 로드하고 검증합니다.
     *
     * @throws ItemDataException JSON 파싱 실패 또는 데이터 무결성 위반 시
     */
    @PostConstruct
    void init() {
        try (InputStream inputStream = new ClassPathResource(ITEM_JSON_PATH).getInputStream()) {
            this.items = loadFromStream(inputStream);
        } catch (final IOException exception) {
            throw new ItemDataException("아이템 JSON 파일 로딩 실패: " + ITEM_JSON_PATH, exception);
        }
    }

    /**
     * 입력 스트림에서 아이템 데이터를 파싱하고 검증하여 불변 목록으로 반환합니다.
     *
     * <p>파싱 로직이 리소스 로딩과 분리되어 있으므로, 프로퍼티 테스트에서 인메모리 데이터를 주입하여 검증할 수 있습니다.
     *
     * @param inputStream 아이템 JSON 데이터 입력 스트림
     * @return 검증 완료된 불변 아이템 목록 (정의 순서 보존)
     * @throws ItemDataException 파싱 실패 또는 데이터 무결성 위반 시
     */
    public List<Item> loadFromStream(final InputStream inputStream) {
        final JsonNode rootArray = parseJson(inputStream);
        validateRootArray(rootArray);

        final Set<String> ids = new HashSet<>();
        final List<Item> result = new ArrayList<>();

        for (final JsonNode itemNode : rootArray) {
            final Item item = parseItemNode(itemNode, ids);
            result.add(item);
        }

        return List.copyOf(result);
    }

    /**
     * 전체 아이템 목록을 정의 순서대로 반환합니다.
     *
     * @return 불변 아이템 목록
     */
    public List<Item> all() {
        return items;
    }

    /**
     * 아이템 ID로 아이템을 조회합니다.
     *
     * @param itemId 조회할 아이템 ID
     * @return 대응하는 아이템을 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<Item> byId(final String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        for (final Item item : items) {
            if (itemId.equals(item.id())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private JsonNode parseJson(final InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (final RuntimeException exception) {
            throw new ItemDataException("아이템 JSON 파싱 실패", exception);
        }
    }

    private void validateRootArray(final JsonNode rootArray) {
        if (rootArray == null || !rootArray.isArray()) {
            throw new ItemDataException("아이템 JSON 최상위 구조가 배열이 아닙니다.");
        }
    }

    private Item parseItemNode(final JsonNode itemNode, final Set<String> ids) {
        final String id = extractRequiredField(itemNode, "id");
        final String name = extractRequiredField(itemNode, "name");
        final String typeString = extractRequiredField(itemNode, "type");

        final ItemType itemType =
                ItemType.fromString(typeString)
                        .orElseThrow(
                                () ->
                                        new ItemDataException(
                                                "아이템 '"
                                                        + id
                                                        + "'의 type '"
                                                        + typeString
                                                        + "'을(를) 변환할 수 없습니다."));

        if (!ids.add(id)) {
            throw new ItemDataException("아이템 id '" + id + "'이(가) 중복됩니다.");
        }

        final Integer buyPrice = extractOptionalInt(itemNode, "buyPrice");

        if (itemType == ItemType.POTION) {
            return parsePotionItem(itemNode, id, name, buyPrice);
        }
        return parseEquipmentItem(itemNode, id, name, itemType, buyPrice);
    }

    private PotionItem parsePotionItem(
            final JsonNode itemNode, final String id, final String name, final Integer buyPrice) {
        final Integer healHpOpt = extractOptionalInt(itemNode, "healHp");
        final Integer healMpOpt = extractOptionalInt(itemNode, "healMp");
        final Integer healStaminaOpt = extractOptionalInt(itemNode, "healStamina");

        final int healHp = healHpOpt != null ? healHpOpt : 0;
        final int healMp = healMpOpt != null ? healMpOpt : 0;
        final int healStamina = healStaminaOpt != null ? healStaminaOpt : 0;

        if (healHp <= 0 && healMp <= 0 && healStamina <= 0) {
            throw new ItemDataException(
                    "포션 아이템 '" + id + "'은(는) healHp, healMp, healStamina 중 적어도 하나가 양수여야 합니다.");
        }

        return new PotionItem(id, name, healHp, healMp, healStamina, buyPrice);
    }

    private EquipmentItem parseEquipmentItem(
            final JsonNode itemNode,
            final String id,
            final String name,
            final ItemType itemType,
            final Integer buyPrice) {
        final String kindString = extractRequiredField(itemNode, "kind");
        final EquipmentKind kind =
                EquipmentKind.fromString(kindString)
                        .orElseThrow(
                                () ->
                                        new ItemDataException(
                                                "아이템 '"
                                                        + id
                                                        + "'의 kind '"
                                                        + kindString
                                                        + "'을(를) 변환할 수 없습니다."));

        if (!itemNode.has("maxDurability")) {
            throw new ItemDataException("장비 아이템 '" + id + "'에 필수 필드 'maxDurability'가 누락되었습니다.");
        }
        final int maxDurability = itemNode.get("maxDurability").asInt();

        final List<EquipBonus> bonuses = parseBonuses(itemNode, id);

        return new EquipmentItem(id, name, itemType, kind, bonuses, buyPrice, maxDurability);
    }

    private List<EquipBonus> parseBonuses(final JsonNode itemNode, final String itemId) {
        final JsonNode bonusesNode = itemNode.get("bonuses");
        if (bonusesNode == null || bonusesNode.isNull()) {
            return List.of();
        }
        if (!bonusesNode.isArray()) {
            throw new ItemDataException("아이템 '" + itemId + "'의 'bonuses' 필드가 배열이 아닙니다.");
        }

        final List<EquipBonus> result = new ArrayList<>();
        for (final JsonNode bonusNode : bonusesNode) {
            final String targetString = extractRequiredField(bonusNode, "target");
            final BonusTarget target = parseBonusTarget(targetString, itemId);
            final int amount = extractRequiredInt(bonusNode, "amount", itemId);
            result.add(new EquipBonus(target, amount));
        }

        return List.copyOf(result);
    }

    private BonusTarget parseBonusTarget(final String targetString, final String itemId) {
        try {
            return BonusTarget.valueOf(targetString);
        } catch (final IllegalArgumentException exception) {
            throw new ItemDataException(
                    "아이템 '" + itemId + "'의 bonuses.target '" + targetString + "'을(를) 변환할 수 없습니다.");
        }
    }

    private String extractRequiredField(final JsonNode node, final String fieldName) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
            final String itemId = node.has("id") ? node.get("id").asText() : "(unknown)";
            throw new ItemDataException(
                    "아이템 '" + itemId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있습니다.");
        }
        return fieldNode.asText();
    }

    private int extractRequiredInt(
            final JsonNode node, final String fieldName, final String itemId) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isNumber()) {
            throw new ItemDataException(
                    "아이템 '" + itemId + "'의 필수 필드 '" + fieldName + "'이(가) 비어있거나 숫자가 아닙니다.");
        }
        return fieldNode.asInt();
    }

    private Integer extractOptionalInt(final JsonNode node, final String fieldName) {
        final JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asInt();
    }
}
