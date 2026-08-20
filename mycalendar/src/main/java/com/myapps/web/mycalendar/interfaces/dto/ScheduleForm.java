package com.myapps.web.mycalendar.interfaces.dto;

import com.myapps.web.mycalendar.domain.model.Category;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 생성/수정 폼 바인딩용 DTO.
 *
 * <p>Thymeleaf 폼에서 사용자가 입력한 일정 데이터를 바인딩하기 위한 record입니다. category, startDate, content는 필수 입력 필드이며,
 * endDate와 scheduleTime은 선택 입력 필드입니다.
 *
 * @param category 일정 카테고리 (SEUNGKWON, CHIWON, DATE)
 * @param startDate 시작 날짜 (필수)
 * @param endDate 종료 날짜 (선택, Multi_Day_Schedule인 경우 입력)
 * @param scheduleTime 일정 시간 (선택)
 * @param content 일정 내용 (필수, 최대 200자)
 */
public record ScheduleForm(
        Category category,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime scheduleTime,
        String content) {}
