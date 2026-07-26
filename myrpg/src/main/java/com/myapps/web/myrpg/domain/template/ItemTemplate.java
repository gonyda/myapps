package com.myapps.web.myrpg.domain.template;

import com.myapps.web.myrpg.domain.model.EffectType;
import com.myapps.web.myrpg.domain.model.ItemType;

/**
 * 소모품 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 아이템 정의 레코드이다.
 * 포션 등 소모품의 효과와 구매가를 정의한다.
 */
public record ItemTemplate(long id, String name, ItemType itemType,
                           EffectType effectType, int effectAmount, int buyPrice) {
}
