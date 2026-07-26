package com.myapps.web.myrpg.domain.template;

import com.myapps.web.myrpg.domain.model.ArmorSlot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 방어구 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 방어구 정의 레코드이다.
 * 방어구 인스턴스 생성 시 기본 판매가 참조용으로 사용된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArmorTemplate(long id, String name,
                            @JsonProperty("armorSlot") ArmorSlot slot,
                            int baseDefense, int baseValue) {
}
