package com.myapps.web.mycalendar.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 일정에 대한 댓글 엔티티.
 *
 * <p>각 댓글은 특정 일정에 속하며, 작성자(승권 또는 치원)와
 * 내용, 작성 시각 정보를 포함합니다.
 */
@Entity
@Table(name = "schedule_comment")
public class ScheduleComment {

    private static final int MAX_CONTENT_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Author author;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA 전용 기본 생성자.
     */
    protected ScheduleComment() {
    }

    /**
     * 필수 필드를 받아 댓글을 생성합니다.
     *
     * @param author  댓글 작성자 (SEUNGKWON 또는 CHIWON)
     * @param content 댓글 내용 (1자 이상 200자 이하)
     */
    public ScheduleComment(final Author author, final String content) {
        this.author = author;
        this.content = content;
    }

    /**
     * 엔티티가 영속화되기 전 생성 시각을 설정합니다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 댓글 ID를 반환합니다.
     *
     * @return 댓글 식별자
     */
    public Long getId() {
        return id;
    }

    /**
     * 댓글이 속한 일정을 반환합니다.
     *
     * @return 연관된 일정
     */
    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * 댓글 작성자를 반환합니다.
     *
     * @return 작성자 (SEUNGKWON 또는 CHIWON)
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * 댓글 내용을 반환합니다.
     *
     * @return 댓글 내용
     */
    public String getContent() {
        return content;
    }

    /**
     * 댓글 생성 시각을 반환합니다.
     *
     * @return 생성 시각
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 댓글 내용을 변경합니다.
     *
     * @param content 새 내용 (최대 200자)
     */
    public void updateContent(final String content) {
        this.content = content;
    }

    /**
     * 양방향 연관관계 동기화를 위해 소속 일정을 설정합니다.
     *
     * <p>이 메서드는 {@link Schedule#addComment(ScheduleComment)} 및
     * {@link Schedule#removeComment(ScheduleComment)}에서 호출됩니다.
     *
     * @param schedule 소속 일정 (null 허용 — 연관관계 해제 시)
     */
    void assignSchedule(final Schedule schedule) {
        this.schedule = schedule;
    }
}
