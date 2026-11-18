package com.myapps.web.mycalendar.interfaces.api;

import com.myapps.web.mycalendar.application.dto.GitHubSyncResult;
import com.myapps.web.mycalendar.application.dto.GitHubSyncStatus;
import com.myapps.web.mycalendar.application.service.GitHubCalendarSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GitHub 캘린더 스냅샷 수동 전체 재동기화를 제공하는 REST 컨트롤러. */
@RestController
@RequestMapping("/schedules")
public class ScheduleSyncController {

    private final GitHubCalendarSyncService gitHubCalendarSyncService;

    /**
     * ScheduleSyncController를 생성합니다.
     *
     * @param gitHubCalendarSyncService GitHub 동기화 서비스
     */
    public ScheduleSyncController(final GitHubCalendarSyncService gitHubCalendarSyncService) {
        this.gitHubCalendarSyncService = gitHubCalendarSyncService;
    }

    /**
     * 전체 일정을 GitHub {@code calendar/mycalendar.json}으로 즉시 수동 재동기화합니다.
     *
     * @return 동기화 성공·스킵 시 200 OK, 외부 GitHub 호출 실패 시 502 Bad Gateway 응답
     */
    @PostMapping("/sync/github")
    public ResponseEntity<GitHubSyncResult> syncToGitHub() {
        final GitHubSyncResult result = gitHubCalendarSyncService.syncNow();
        if (result.status() == GitHubSyncStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
