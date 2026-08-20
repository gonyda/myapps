package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.domain.model.OwnedItem;
import java.lang.reflect.Field;

/** 테스트 전용 엔티티 ID 설정 헬퍼. */
public final class IdTestHelper {

    private IdTestHelper() {}

    /**
     * 리플렉션으로 OwnedItem의 id 필드를 설정한다.
     *
     * @param item 대상 OwnedItem
     * @param id 설정할 엔티티 ID
     */
    public static void setId(final OwnedItem item, final long id) {
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem id 설정 실패", exception);
        }
    }
}
