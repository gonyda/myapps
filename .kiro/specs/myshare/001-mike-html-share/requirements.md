# Requirements Document: mike-html-share

> **폴더 위치**: `.kiro/specs/myshare/001-mike-html-share/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/module/module-template.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: 외부 AI(Mike)가 생성한 단일 HTML 파일이 비공개 GitHub 저장소(`gonyda/mike`)의 `share/{slug}.html` 경로에 저장되어 있으나, 별도 로그인이나 GitHub 접근 권한 없이 카카오톡·모바일 브라우저(iOS Safari 등)로 즉시 열람할 수 있는 웹 서빙 엔드포인트가 존재하지 않는다.
- **핵심 목표**: `myapps` 멀티모듈 프로젝트에 신규 Web 모듈 `myshare`를 추가하여 `gonyda/mike` 저장소(`main` 브랜치)의 `share/{slug}.html`을 GitHub Contents API(읽기 전용)로 조회하고 `GET /share/{slug}` URL로 즉시 서빙한다.
- **선행 스펙과의 연계**: `mycalendar/003-github-calendar-sync`에서 확립한 `RestClient` 기반 GitHub Contents API 연동 패턴과 공통 환경변수(`MIKE_GITHUB_TOKEN`)를 공유·재사용한다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **신규 Web 모듈 `myshare` 신설**: `com.myapps.web.myshare` 패키지(DDD 4계층), Parent POM `<modules>` 등록, 할당 포트 `8084`, `deployment.md` 포트 매핑 갱신.
2. **GitHub Contents API 읽기 전용 클라이언트**: `gonyda/mike` (`main` 브랜치, `share` 폴더) 설정 프로퍼티 분리, `${MIKE_GITHUB_TOKEN:}` 공유 바인딩, Base64 `content` UTF-8 디코딩(대용량 파일 시 `raw+json` 폴백).
3. **HTTP 서빙 및 보안/캐싱**:
   - `GET /share/{slug}` (`200 OK`, `text/html; charset=utf-8`), 인라인 `<style>`/`<script>` 정상 실행 헤더 구성.
   - `slug` 엄격 검증(`^[a-zA-Z0-9_-]{1,200}$`), Path Traversal(`..`, `/` 등) `400 Bad Request` 차단.
   - 미존재 파일 `404 Not Found`, 전체 목록 조회(`/share`, `/share/`) 미노출(404).
   - 짧은 TTL(기본 20초) 바운디드 인메모리 캐시 및 GitHub/브라우저 `ETag`/`If-None-Match` 지원.
4. **단위·슬라이스·아키텍처 테스트 및 5대 품질 가드레일 통과**.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **파일 업로드/수정/삭제 API**: `myshare`는 순수 읽기 전용 서빙만 담당하며 GitHub 쓰기(`PUT`/`DELETE`)는 수행하지 않는다.
- **사용자 인증/목록 대시보드**: 비공개 링크 보호를 위해 로그인 UI나 슬러그 목록 조회 API는 제공하지 않는다.
- **DB 영속화**: 상태 저장이 필요 없는 무상태 프록시 모듈이므로 Oracle/JPA DataSource 자동 구성을 비활성화한다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **GitHub_Contents_API**: `https://api.github.com/repos/{owner}/{repo}/contents/{path}?ref={branch}` 읽기 전용 REST API.
- **MIKE_GITHUB_TOKEN**: `mycalendar` 모듈과 공유하는 `gonyda/mike` 대상 Fine-grained PAT 환경변수 (`myshare`는 최소 `Contents: Read-only` 권한만 요구).

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **Share_Slug**: `GET /share/{slug}` 경로에서 단일 HTML 파일을 식별하는 문자열. 영문·숫자·하이픈(`-`)·언더스코어(`_`)만 허용.
- **Share_Page**: 슬러그에 대응하는 UTF-8 HTML 본문과 GitHub 파일 `sha`(ETag)를 보관하는 불변 도메인 값 객체.
- **TTL_Html_Cache**: GitHub API 호출 부하를 줄이면서 수정 내역을 빠르게 반영하기 위한 짧은 TTL(기본 20초) 기반 인메모리 캐시.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: `myshare` Web 모듈 구성 및 공통 토큰 설정

**User Story:**  
시스템 운영자로서, 기존 `myapps` 모듈(`mystudy`, `mycalendar`, `myrpg`)과 일관된 구조와 포트 규칙으로 `myshare` 모듈을 배포하고 `mycalendar`의 GitHub 토큰을 그대로 공유하고 싶다.  
그래야 별도 토큰 재발급이나 아키텍처 불일치 없이 즉시 로컬/운영 환경에 띄울 수 있다.

#### Acceptance Criteria
1. **THE** 시스템 **SHALL** 루트 `pom.xml`에 `<module>myshare</module>`을 등록하고 `com.myapps.web.myshare` 패키지의 Web 모듈을 제공한다.
2. **THE** 시스템 **SHALL** `myshare` 운영 포트를 `8084`로 설정하고 `rules/infra/deployment.md` 포트 규칙 표에 등록한다.
3. **THE** 시스템 **SHALL** `myshare.github` 프로퍼티로 `owner`(`gonyda`), `repo`(`mike`), `branch`(`main`), `base-path`(`share`), `enabled`, `token`(`${MIKE_GITHUB_TOKEN:}`)을 분리 바인딩하며 토큰 값을 코드·로그·`toString()`에 노출하지 않는다.
4. **THE** 시스템 **SHALL** 데이터베이스를 사용하지 않으므로 DataSource/JPA 자동 구성을 제외하여 `DB_PASSWORD` 없이도 기동 가능하게 유지한다.

---

### Requirement 2: GitHub Contents API 읽기 전용 연동

**User Story:**  
서비스 제공자로서, 비공개 저장소 `gonyda/mike`의 `share/{slug}.html`을 안전하고 정확하게 읽어오고 싶다.  
그래야 한글·이모지가 포함된 단일 HTML이 깨짐 없이 서빙될 수 있다.

#### Acceptance Criteria
1. **WHEN** 캐시 미스 또는 TTL 만료 시, **THE** 클라이언트는 `GET /repos/{owner}/{repo}/contents/{basePath}/{slug}.html?ref={branch}`를 `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28` 헤더로 호출한다.
2. **WHEN** GitHub API가 Base64 `content`를 반환하면, **THE** 클라이언트는 개행이 포함된 MIME Base64(`Base64.getMimeDecoder()`)를 UTF-8 문자열로 디코딩하여 반환한다.
3. **IF** 파일이 1MB를 초과해 `encoding`이 `"none"`이거나 `content`가 비어 있으면, **THEN THE** 클라이언트는 `Accept: application/vnd.github.raw+json` 헤더로 원문 UTF-8 HTML을 재조회한다.
4. **IF** GitHub API가 `404 Not Found`를 반환하거나 파일 타입이 `"file"`이 아니면, **THEN THE** 시스템은 `ShareNotFoundException`을 발생시킨다.
5. **IF** GitHub API가 `5xx`, 인증 오류(`401`/`403`), 네트워크 타임아웃(연결 3초/읽기 5초)을 반환하면, **THEN THE** 시스템은 `GitHubShareException`으로 래핑한다.
6. **WHERE** `myshare.github.enabled`가 `false`이거나 `token`이 blank일 때, **THE** 시스템은 외부 HTTP 호출을 수행하지 않고 `ShareConfigurationException`을 발생시킨다.

---

### Requirement 3: HTML 서빙 엔드포인트, 보안 검증 및 캐싱

**User Story:**  
모바일 사용자(카카오톡/iOS Safari)로서, 공유받은 `/share/{slug}` 링크를 탭하면 로그인 없이 HTML 페이지가 즉시 정상 렌더링·동작하기를 원한다.  
또한 운영자로서, 임의 경로 탐색이나 전체 목록 노출 없이 URL을 아는 사람만 열람하고 GitHub API 호출량도 절감되기를 원한다.

#### Acceptance Criteria
1. **WHEN** 유효한 `slug`로 `GET /share/{slug}` 요청이 들어오면, **THE** 컨트롤러는 `200 OK`, `Content-Type: text/html; charset=utf-8`, `Cache-Control: private, no-cache`, `ETag: "{sha}"`, `X-Robots-Tag: noindex, nofollow`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` 헤더와 함께 HTML 원문을 반환한다.
2. **THE** 시스템은 인라인 `<style>`·`<script>` 실행을 차단하는 `Content-Security-Policy`나 `Content-Disposition: attachment` 헤더를 추가하지 않는다.
3. **IF** `slug`가 `^[a-zA-Z0-9_-]{1,200}$`에 위배되거나(`/share/..`, `/share/a.html`, `/share/a/b` 등 하위 경로 포함) Path Traversal 문자가 포함되면, **THEN THE** 시스템은 외부 API 호출 없이 즉시 `400 Bad Request`를 반환한다.
4. **WHEN** `GET /share` 또는 `GET /share/` 요청이 들어오면, **THE** 시스템은 파일 목록을 노출하지 않고 `404 Not Found`를 반환한다.
5. **WHILE** 동일 `slug`의 캐시 엔트리가 TTL(기본 20초) 이내에 유효한 동안, **THE** 서비스는 외부 GitHub API를 재호출하지 않고 캐시된 HTML을 반환한다.
6. **WHEN** 캐시 TTL 만료 후 재조회 시 이전 `sha`가 존재하면, **THE** 클라이언트는 `If-None-Match: "{sha}"` 헤더를 전송해 `304 Not Modified` 응답 시 본문 재전송 없이 TTL을 연장한다.
7. **WHEN** 클라이언트 요청에 `If-None-Match`가 현재 `sha`와 일치하면, **THE** 컨트롤러는 `304 Not Modified`를 반환한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단.
   - **ArchUnit**: DDD 계층(`interfaces` → `application` → `domain`) 및 컨트롤러 위치 규칙 준수.
   - **JaCoCo**: 명령어/라인 커버리지 **80% 이상**, 브랜치 커버리지 **70% 이상** 달성.
   - **PMD & CPD**: 복잡도, 안티패턴 및 중복 코드 0건.
2. **외부 네트워크 독립 테스트**: 모든 단위·웹 슬라이스·통합 테스트는 `MockRestServiceServer` 및 Mock을 사용하여 실제 GitHub 호출 없이 결정론적으로 실행된다.
3. **CodeGraph 동기화**: 구현 완료 후 `codegraph sync` 실행.
