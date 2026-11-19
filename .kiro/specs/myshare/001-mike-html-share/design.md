# Design Document: mike-html-share

> **폴더 위치**: `.kiro/specs/myshare/001-mike-html-share/design.md`
> **관련 규칙**: `rules/coding/code-style.md`, `rules/workflow/codegraph-first.md`, `rules/module/module-template.md`

---

## 1. Overview (개요)

본 설계는 신규 Web 모듈 `myshare`(`com.myapps.web.myshare`)를 통해 비공개 GitHub 저장소(`gonyda/mike`, `main` 브랜치)의 `share/{slug}.html` 파일을 읽어와 모바일 브라우저 친화적 HTTP 응답으로 서빙하는 아키텍처를 정의한다.

### 1.1. 핵심 설계 결정 및 트레이드오프
| 항목 / 대안 | 선택된 결정 | 근거 및 트레이드오프 | 관련 요구사항 |
|---|---|---|---|
| **HTTP 클라이언트** | Spring `RestClient` + `SimpleClientHttpRequestFactory`(3s/5s) + `GitHubShareClient` 포트 | `mycalendar`와 동일 패턴, `MockRestServiceServer` 기반 무네트워크 테스트 용이 | Req 2.1, 2.5 |
| **GitHub 응답 포맷** | 기본 `application/vnd.github+json` (MIME Base64 디코딩 + `sha` 추출), 1MB 초과 시 `raw+json` 폴백 | `sha` 기반 `If-None-Match`/`ETag` 지원과 대용량 HTML 호환성 동시 확보 | Req 2.2, 2.3, 3.6 |
| **캐시 전략** | `ConcurrentHashMap` 기반 바운디드 TTL 캐시(기본 20초, 최대 200건) + `Clock` 주입 | 별도 Redis/외부 라이브러리 없이 경량 동작, `Clock` 모킹으로 100% 결정론적 테스트 | Req 3.5, 3.6 |
| **비공개 보호 & 헤더** | `GET /share`, `/share/` 404 차단 + `Cache-Control: private, no-cache` + `X-Robots-Tag: noindex, nofollow` + CSP 미적용 | 검색엔진/공유 프록시 캐싱 방지, 인라인 `<style>`/`<script>` 모바일 실행 보장 | Req 3.1~3.4 |
| **DataSource 비활성화** | Spring Boot 4.0 DataSource/Hibernate/JpaRepositories AutoConfiguration 제외 | Parent POM의 `spring-boot-starter-data-jpa` 상속에도 불필요한 DB 풀/지갑 의존 제거 | Req 1.4 |

---

## 2. Architecture (시스템 아키텍처 및 계층 구조)

### 2.1. DDD 4계층 패키지 구조
```
myshare/src/main/java/com/myapps/web/myshare/
├── MyshareApplication.java
├── domain/
│   ├── exception/
│   │   └── InvalidShareSlugException.java       # 유효하지 않은 slug/Path Traversal 예외 (400)
│   └── model/
│       ├── ShareSlug.java                       # slug 정규식/길이 검증 불변 값 객체
│       └── SharePage.java                       # html + sha + slug 불변 레코드
├── application/
│   ├── dto/
│   │   └── ShareFetchResult.java                # sealed interface (Found | NotModified | NotFound)
│   ├── exception/
│   │   ├── GitHubShareException.java            # 외부 GitHub API 통신 실패 예외 (502)
│   │   ├── ShareConfigurationException.java     # 비활성화/토큰 미설정 예외 (503)
│   │   └── ShareNotFoundException.java          # 파일 미존재 예외 (404)
│   ├── port/
│   │   └── GitHubShareClient.java               # GitHub 읽기 포트 인터페이스
│   └── service/
│       ├── InMemoryShareCache.java              # 바운디드 TTL 인메모리 캐시 (Clock 기반)
│       └── SharePageService.java                # slug 검증 -> 캐시 조회 -> GitHub 조회 오케스트레이션
├── infrastructure/
│   ├── config/
│   │   ├── GitHubShareProperties.java           # @ConfigurationProperties(myshare.github)
│   │   ├── ShareCacheProperties.java            # @ConfigurationProperties(myshare.cache)
│   │   └── GitHubShareConfiguration.java        # RestClient(3s/5s), Clock 빈 등록
│   └── github/
│       ├── GitHubContentFileResponse.java       # GET JSON 응답 DTO (type, encoding, content, sha)
│       └── RestClientGitHubShareClient.java     # GitHub Contents API 읽기 구현체
└── interfaces/
    └── api/
        ├── ShareController.java                 # GET /share/{slug}, /share/{slug}/**, /share, /share/
        └── ShareExceptionHandler.java           # @RestControllerAdvice 상태 코드별 안전 HTML 에러 응답
```

### 2.2. 요청 흐름 및 시퀀스 다이어그램
```mermaid
sequenceDiagram
    autonumber
    participant Client as 모바일 브라우저 (Safari/카카오톡)
    participant Controller as ShareController
    participant Service as SharePageService
    participant Cache as InMemoryShareCache
    participant GH as RestClientGitHubShareClient

    Client->>Controller: GET /share/{slug} (optional If-None-Match)
    Controller->>Service: loadPage(slug)
    Service->>Service: ShareSlug.of(slug) 검증 (실패 시 400)
    Service->>Cache: findValid(slug, now)
    alt 캐시 히트 (TTL 이내)
        Cache-->>Service: CachedEntry(html, sha)
    else 캐시 미스 또는 TTL 만료
        Cache-->>Service: Optional(expiredEntry)
        Service->>GH: fetchHtml(slug, expiredSha)
        alt 200 OK (Base64 -> UTF-8 디코딩)
            GH-->>Service: ShareFetchResult.Found(html, sha)
            Service->>Cache: put(slug, html, sha, now)
        else 304 Not Modified (If-None-Match 일치)
            GH-->>Service: ShareFetchResult.NotModified
            Service->>Cache: refreshTtl(slug, now)
        else 404 Not Found
            GH-->>Service: ShareFetchResult.NotFound
            Service-->>Controller: throw ShareNotFoundException (404)
        end
    end
    alt 요청 If-None-Match == sha
        Controller-->>Client: 304 Not Modified (ETag, Cache-Control)
    else 신규/변경 응답
        Controller-->>Client: 200 OK (text/html; charset=utf-8, ETag, HTML)
    end
```

---

## 3. Components and Interfaces (세부 컴포넌트 설계)

### 3.1. Domain Layer (`domain/model`, `domain/exception`)
- **`ShareSlug`**:
  - 허용 패턴: `^[a-zA-Z0-9_-]{1,200}$`.
  - null, blank, `.`, `..`, `/`, `\`, `%`, 확장자 점(`.html`) 포함 시 `InvalidShareSlugException` 발생.
- **`SharePage`**:
  - `record SharePage(String slug, String html, String sha)` — null/blank 방어.

### 3.2. Application Layer (`application/port`, `application/service`, `application/dto`)
- **`GitHubShareClient`**:
  - `ShareFetchResult fetchHtml(String slug, String knownSha)`
- **`ShareFetchResult`**:
  - `public sealed interface ShareFetchResult permits ShareFetchResult.Found, ShareFetchResult.NotModified, ShareFetchResult.NotFound`
- **`InMemoryShareCache`**:
  - `ConcurrentHashMap<String, CacheEntry>` 기반, `ttlSeconds` (`20`), `maxEntries` (`200`).
  - `findFresh(slug)` / `findStaleSha(slug)` / `put(slug, page)` / `refreshTtl(slug)`.
  - `put` 시 `size >= maxEntries`이면 만료 엔트리 우선 제거 후 잔여 초과 시 가장 오래된 `fetchedAt` 엔트리 축출(bounded).
- **`SharePageService`**:
  - `ShareSlug.of(rawSlug)` 검증 → 캐시 fresh 조회 → 미스 시 `staleSha`와 함께 `gitHubShareClient.fetchHtml(...)` 호출 → pattern matching `switch`로 `Found` / `NotModified` / `NotFound` 처리.

### 3.3. Infrastructure Layer (`infrastructure/config`, `infrastructure/github`)
- **`GitHubShareProperties`**:
  - Prefix: `myshare.github`
  - Fields: `enabled` (기본 `true`), `token` (`${MIKE_GITHUB_TOKEN:}`), `owner` (`gonyda`), `repo` (`mike`), `branch` (`main`), `basePath` (`share`).
  - `toString()`에서 `token` 마스킹 (`tokenConfigured=true/false`).
- **`ShareCacheProperties`**:
  - Prefix: `myshare.cache`
  - Fields: `ttlSeconds` (기본 `20`), `maxEntries` (기본 `200`).
- **`RestClientGitHubShareClient`**:
  - `!properties.isActive()` 시 HTTP 호출 없이 `ShareConfigurationException` throw.
  - `UriBuilder` `pathSegment`로 `repos/{owner}/{repo}/contents/{basePathSegments...}/{slug}.html?ref={branch}` 생성.
  - 헤더: `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28`, 조건부 `If-None-Match: "{knownSha}"`.
  - `304 Not Modified` 수신 시 `RestClient` `exchange(...)` 또는 상태 코드 분기를 통해 `ShareFetchResult.NotModified` 반환! (주의: Spring `RestClient`의 `.retrieve()`는 304에서 `HttpStatusCode::is2xxSuccessful` 판단에 따라 처리되므로 `.exchange((req, res) -> ...)`를 사용하면 `200`, `304`, `404` 및 기타 오류 코드를 명확하고 깔끔하게 분기할 수 있다!)
  - Base64 디코딩: `Base64.getMimeDecoder().decode(response.content())` → `new String(..., StandardCharsets.UTF_8)`.
  - 1MB 초과 파일(`encoding="none"` 또는 `content` blank) 시 `Accept: application/vnd.github.raw+json`으로 폴백 조회.

### 3.4. Interface Layer (`interfaces/api`)
- **`ShareController`**:
  - `GET /share/{slug}`: HTML 서빙 (`200 OK` 또는 `304 Not Modified`).
    - 응답 헤더:
      - `Content-Type: text/html; charset=utf-8`
      - `Cache-Control: private, no-cache`
      - `ETag: "{sha}"` (`sha`가 존재할 때)
      - `X-Robots-Tag: noindex, nofollow`
      - `X-Content-Type-Options: nosniff`
      - `Referrer-Policy: no-referrer`
  - `GET /share/{slug}/**`: `InvalidShareSlugException` 발생 → `400 Bad Request`.
  - `GET /share`, `GET /share/`: `ShareNotFoundException` 발생 → `404 Not Found`.
- **`ShareExceptionHandler`**:
  - `@RestControllerAdvice`로 예외별 상태 코드(`400`, `404`, `502`, `503`) 및 정적 안전 UTF-8 HTML 에러 페이지 반환(사용자 입력 `slug` 미반영으로 XSS 차단, 내부 예외/토큰 미노출).

---

## 4. Correctness Properties

### Property 1: Slug 검증 불변식
*For any* `^[a-zA-Z0-9_-]{1,200}$`를 만족하지 않는 입력 문자열(빈 문자열, `.`, `..`, `/`, `\`, 공백, 특수문자 포함)에 대해, `ShareSlug.of`는 항상 `InvalidShareSlugException`을 던져야 한다.
- **Validates: Requirement 3.3**

### Property 2: 유효 Slug 보존 불변식
*For any* `^[a-zA-Z0-9_-]{1,200}$`를 만족하는 입력 문자열에 대해, `ShareSlug.of(input).value()`는 입력값과 정확히 동일해야 한다.
- **Validates: Requirement 3.1, 3.3**

### Property 3: 캐시 바운디드 및 TTL 불변식
*For any* 임의 개수의 서로 다른 슬러그 저장 요청에 대해, `InMemoryShareCache`의 크기는 항상 `maxEntries` 이하를 유지해야 하며 `ttlSeconds` 경과 전 조회는 항상 캐시 적중해야 한다.
- **Validates: Requirement 3.5**

---

## 5. Testing Strategy & Quality Guardrails

1. **`ShareSlugPropertyTest`** (jqwik `@Property`): Property 1, 2 불변식 검증.
2. **`InMemoryShareCacheTest`**: TTL 적중/만료(`Clock` 제어), `refreshTtl`, `maxEntries` 초과 시 만료 우선 축출 및 LRU/최오래된 항목 축출 검증.
3. **`SharePageServiceTest`**: 캐시 히트 시 무호출, 미스 시 `Found` 저장, 만료 시 `If-None-Match` → `NotModified` TTL 갱신, `NotFound` 시 예외(미캐싱) 검증.
4. **`RestClientGitHubShareClientTest`**: `MockRestServiceServer` 기반 Base64(개행·한글·이모지 포함) UTF-8 디코딩, `304 Not Modified`, `404 Not Found`, 1MB 초과 `raw+json` 폴백, `type != file` 처리, `5xx`/네트워크 타임아웃 `GitHubShareException`, `enabled=false`/토큰 누락 시 무호출 검증.
5. **`ShareControllerTest`** (`@WebMvcTest`): `GET /share/{slug}` 200 HTML/UTF-8/보안·캐시 헤더(인라인 `<style>`/`<script>` 포함), `If-None-Match` 304, `400`(`..`, `.html`, `/a/b`), `404`(`/share`, `/share/`, 미존재 슬러그), `502`/`503` 응답 검증.
6. **`MyshareApplicationTest`** (`@SpringBootTest`) & **`ArchitectureRuleTest`** (ArchUnit).
7. **5대 품질 가드레일**:
   ```bash
   mvn -B -q spotless:apply -pl myshare && (mvn -B clean install -pl myshare -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync
   ```
