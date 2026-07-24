package com.myapps.web.mycalendar.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * 일정을 나타내는 핵심 도메인 엔티티.
 *
 * <p>커플 캘린더의 개별 일정 항목을 표현하며, 카테고리(승권/치원/데이트),
 * 날짜 범위, 시간, 내용을 포함합니다.
 */
@Entity
@Table(name = "schedule")
public class Schedule {

    private static final int MAX_CONTENT_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 전용 기본 생성자.
     */
    protected Schedule() {
    }

    /**
     * 필수 필드를 받아 일정을 생성합니다.
     *
     * @param category  일정 카테고리 (SEUNGKWON, CHIWON, DATE)
     * @param startDate 일정 시작 날짜
     * @param content   일정 내용 (1자 이상 200자 이하)
     */
    public Schedule(final Category category, final LocalDate startDate, final String content) {
        this.category = category;
        this.startDate = startDate;
        this.content = content;
    }

    /**
     * 엔티티가 영속화되기 전 생성 시각과 수정 시각을 설정합니다.
     */
    @PrePersist
    protected void onCreate() {
        final LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 엔티티가 수정되기 전 수정 시각을 갱신합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 일정 ID를 반환합니다.
     *
     * @return 일정 식별자
     */
    public Long getId() {
        return id;
    }

    /**
     * 일정 카테고리를 반환합니다.
     *
     * @return 카테고리 (SEUNGKWON, CHIWON, DATE)
     */
    public Category getCategory() {
        return category;
    }

    /**
     * 일정 시작 날짜를 반환합니다.
     *
     * @return 시작 날짜
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * 일정 종료 날짜를 반환합니다.
     *
     * @return 종료 날짜 (null 가능 — Single_Day_Schedule인 경우)
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * 일정 시간을 반환합니다.
     *
     * @return 일정 시간 (null 가능)
     */
    public LocalTime getScheduleTime() {
        return scheduleTime;
    }

    /**
     * 일정 내용을 반환합니다.
     *
     * @return 일정 내용 (최대 200자)
     */
    public String getContent() {
        return content;
    }

    /**
     * 일정 생성 시각을 반환합니다.
     *
     * @return 생성 시각
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 일정 최종 수정 시각을 반환합니다.
     *
     * @return 수정 시각
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 일정 카테고리를 변경합니다.
     *
     * @param category 새 카테고리
     */
    public void updateCategory(final Category category) {
        this.category = category;
    }

    /**
     * 일정 내용을 변경합니다.
     *
     * @param content 새 내용 (최대 200자)
     */
    public void updateContent(final String content) {
        this.content = content;
    }

    /**
     * 일정 시작 날짜를 변경합니다.
     *
     * @param startDate 새 시작 날짜
     */
    public void updateStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * 일정 종료 날짜를 변경합니다.
     *
     * @param endDate 새 종료 날짜 (null 허용)
     */
    public void updateEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * 일정 시간을 변경합니다.
     *
     * @param scheduleTime 새 일정 시간 (null 허용)
     */
    public void updateScheduleTime(final LocalTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

}
