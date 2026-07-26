package com.myapps.web.myrpg.domain.model.vo;

import java.util.List;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;

/**
 * 드랍 롤로 생성된 방어구 인스턴스를 나타내는 값 객체.
 *
 * <p>템플릿 기반으로 등급·능력치가 랜덤 결정된 개별 방어구이다.
 */
public record RolledArmor(long templateId, ArmorSlot slot, Grade grade, int itemLevel,
                          List<StatRoll> stats, String displayName) {
}
