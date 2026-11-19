# Implementation Plan: mike-html-share

> **폴더 위치**: `.kiro/specs/myshare/001-mike-html-share/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myapps` 프로젝트에 신규 Web 모듈 `myshare`(`com.myapps.web.myshare`)를 신설하고 `gonyda/mike` 저장소의 `share/{slug}.html`을 서빙하는 기능을 점진적으로 구현하기 위한 작업 목록이다.

---

## Tasks

### A. 모듈 뼈대 & 설정 구성
- [x] 1. 루트 `pom.xml` `<modules>`에 `myshare` 추가 및 `myshare/pom.xml` 생성
- [x] 2. `MyshareApplication.java`, `application.yml`, `application-local.yml`, `application-prod.yml`(포트 `8084`), `src/test/resources/application.yml` 생성
- [x] 3. `rules/infra/deployment.md` 포트 규칙 표에 `myshare | 8084` 반영

### B. 도메인 모델 & 예외 정의 (Domain Layer)
- [x] 4. `InvalidShareSlugException.java`, `ShareSlug.java`, `SharePage.java` 구현

### C. 애플리케이션 서비스 & 캐시 & 포트 (Application Layer)
- [x] 5. `ShareFetchResult.java`, `GitHubShareClient.java`, `ShareNotFoundException.java`, `GitHubShareException.java`, `ShareConfigurationException.java` 구현
- [x] 6. `InMemoryShareCache.java` 및 `SharePageService.java` 구현

### D. GitHub Contents API 클라이언트 & 설정 (Infrastructure Layer)
- [x] 7. `GitHubShareProperties.java`, `ShareCacheProperties.java`, `GitHubShareConfiguration.java` 구현
- [x] 8. `GitHubContentFileResponse.java` 및 `RestClientGitHubShareClient.java` 구현 (Base64 UTF-8 디코딩, `If-None-Match`/`304`, `raw+json` 폴백, `404`/`5xx` 처리)

### E. 웹 컨트롤러 & 예외 처리 (Interface Layer)
- [x] 9. `ShareController.java` 및 `ShareExceptionHandler.java` 구현 (`GET /share/{slug}`, `/share/{slug}/**`, `/share`, `/share/`, 보안/캐시 헤더)

### F. 테스트 작성 & 5대 품질 가드레일 검증
- [x] 10. 단위/프로퍼티/슬라이스/통합/ArchUnit 테스트 작성 (`ShareSlugPropertyTest`, `InMemoryShareCacheTest`, `SharePageServiceTest`, `RestClientGitHubShareClientTest`, `ShareControllerTest`, `MyshareApplicationTest`, `ArchitectureRuleTest`)
- [x] 11. `spotless:apply`, `mvn clean verify -pl myshare -am` (Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD), `codegraph sync` 통과
- [x] 12. `memory-bank/activeContext.md` 갱신
