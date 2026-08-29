package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.MaterialItem;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ItemCatalogServiceMaterialTest {

    private ItemCatalogService itemCatalogService;

    @BeforeEach
    void setUp() {
        itemCatalogService = new ItemCatalogService(new ObjectMapper());
    }

    @Test
    @DisplayName("JSON 스트림에서 type=material인 아이템을 MaterialItem으로 파싱한다")
    void should_parseMaterialItem_fromStream() {
        final String json =
                """
                [
                  {
                    "id": "firewood",
                    "name": "장작",
                    "type": "material",
                    "buyPrice": 20
                  }
                ]
                """;
        final List<Item> items =
                itemCatalogService.loadFromStream(
                        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(items).hasSize(1);
        final Item item = items.get(0);
        assertThat(item).isInstanceOf(MaterialItem.class);
        assertThat(item.id()).isEqualTo("firewood");
        assertThat(item.name()).isEqualTo("장작");
        assertThat(item.type()).isEqualTo(ItemType.MATERIAL);
        assertThat(item.buyPrice()).isEqualTo(20);
    }

    @Test
    @DisplayName("classpath의 item.json에서 firewood 아이템이 정상 로드된다")
    void should_loadFirewood_fromClasspath() {
        itemCatalogService.init();
        final Optional<Item> firewoodOpt = itemCatalogService.byId("firewood");

        assertThat(firewoodOpt).isPresent();
        final Item firewood = firewoodOpt.get();
        assertThat(firewood).isInstanceOf(MaterialItem.class);
        assertThat(firewood.name()).isEqualTo("장작");
        assertThat(firewood.type()).isEqualTo(ItemType.MATERIAL);
        assertThat(firewood.buyPrice()).isEqualTo(20);
        assertThat(firewood.description()).isEqualTo("장작을 소모하여 캠프파이어를 할 수 있습니다.");
    }
}
