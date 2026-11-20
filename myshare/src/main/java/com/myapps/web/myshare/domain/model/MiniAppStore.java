package com.myapps.web.myshare.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 정적 HTML 미니앱들이 공용으로 사용하는 JSON 상태 저장소({@code MINI_APP_STORE}) JPA 엔티티.
 *
 * <p>각 미니앱의 식별자({@code app_key})를 기본 키로 하여 자유 형식의 JSON 본문({@code data_json})과 최종 수정 시각({@code
 * updated_at})을 영속 저장합니다.
 */
@Entity
@Table(name = "MINI_APP_STORE")
public class MiniAppStore {

    private static final int APP_KEY_MAX_LENGTH = 64;

    @Id
    @Column(name = "app_key", length = APP_KEY_MAX_LENGTH, nullable = false, updatable = false)
    private String appKey;

    @Lob
    @Column(name = "data_json", nullable = false, columnDefinition = "CLOB")
    private String dataJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA 스펙용 기본 생성자. */
    protected MiniAppStore() {}

    /**
     * 신규 미니앱 저장소 엔티티를 생성합니다.
     *
     * @param appKey 검증된 미니앱 키
     * @param dataJson 검증된 UTF-8 JSON 본문
     * @param updatedAt 저장 시각
     */
    public MiniAppStore(final String appKey, final String dataJson, final LocalDateTime updatedAt) {
        this.appKey = appKey;
        this.dataJson = dataJson;
        this.updatedAt = updatedAt;
    }

    /** 영속화 전 수정 시각이 비어 있는 경우 현재 시각으로 채웁니다. */
    @PrePersist
    protected void onCreate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 수정 전 수정 시각이 비어 있는 경우 현재 시각으로 갱신합니다. */
    @PreUpdate
    protected void onUpdate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 저장된 JSON 본문과 수정 시각을 갱신합니다.
     *
     * @param dataJson 새 JSON 본문
     * @param updatedAt 갱신 시각
     */
    public void updateDataJson(final String dataJson, final LocalDateTime updatedAt) {
        this.dataJson = dataJson;
        this.updatedAt = updatedAt;
    }

    /**
     * 미니앱 키를 반환합니다.
     *
     * @return 미니앱 키
     */
    public String getAppKey() {
        return appKey;
    }

    /**
     * 저장된 JSON 본문을 반환합니다.
     *
     * @return JSON 문자열
     */
    public String getDataJson() {
        return dataJson;
    }

    /**
     * 최종 수정 시각을 반환합니다.
     *
     * @return 수정 시각
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
