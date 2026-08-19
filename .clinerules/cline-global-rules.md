# .clinerules — Cline 전역 규칙 (Kiro IDE 워크플로우 적용)

이 파일은 myapps 프로젝트에서 Cline이 따라야 할 규칙들을 정의합니다.
아래 규칙은 `.kiro/` 디렉토리의 Kiro IDE 설정을 기반으로 합니다.

---

## 1. 개발 워크플로우: Spec 문서 3종 → 구현

모든 기능 개발은 아래 순서를 따릅니다:

```
1. Spec 문서 3종 작성 (PLAN MODE)
   ├── requirements.md  (요구사항 정의)
   ├── design.md        (설계)
   └── tasks.md         (구현 태스크 목록)

2. 사용자 검토 및 승인

3. tasks.md 기반 구현 (ACT MODE)
   └── 각 Task 완료 시 mvn test + mvn clean install 검증
```

### Spec 문서 위치 규칙

```
.kiro/specs/
└── {모듈명}/
    └── {순번}-{기능명}/
        ├── requirements.md
        ├── design.md
        └── tasks.md
```

- **모듈명**: Maven 모듈명과 동일 (myrpg, mycalendar, mycrawler, mystudy)
- **순번**: 3자리 0-padded 숫자 (001, 002, ...)
- **기능명**: kebab-case 영문 소문자

---

## 2. Spec 문서 3종 규칙

### requirements.md
- Introduction: 스펙 배경과 해결하려는 문제
- Glossary: 기존 용어 + 신규 용어 정의
- Requirements: User Story + Acceptance Criteria (Given/When/Then)

### design.md
- Overview: 핵심 원칙과 이번 스펙 범위
- Architecture: 변경될 모듈 구조 (신규/확장/유지)
- 데이터 변경: JSON 스키마 등
- 세부 설계: 각 요구사항별 구현 방안

### tasks.md
- Overview: 구현 순서 원칙
- Tasks: 체크박스 리스트 (하위 Task 포함)
- 각 Task 완료 전 테스트 + 빌드 검증

---

## 3. 구현 원칙

### 코딩 컨벤션
- **Java 25**, Spring Boot 4.0
- `var` 사용 금지, `@Autowired` 금지, Lombok 금지
- VO/DTO는 `record` 타입
- `final` 파라미터/지역변수 필수
- 커스텀 예외 사용 (`RuntimeException` 직접 금지)
- 매직 넘버 `private static final` 상수화
- 메서드 50줄 초과 시 분리 필수
- 의존성 주입은 순수 생성자 주입만 사용

### 테스트
- 기능 구현시 테스트 코드 필수 작성
- 서비스: 단위 + PBT (`@ExtendWith(MockitoExtension.class)` + jqwik)
- 컨트롤러: `@WebMvcTest` + `@MockitoBean`
- 레포지토리: `@DataJpaTest`
- jqwik PBT에서는 `@Mock` 대신 `Mockito.mock()` 직접 호출
- Spring Boot 4.0: `@MockBean`/`@SpyBean` 금지 → `@MockitoBean`/`@MockitoSpyBean` 사용
- Jackson 3: `com.fasterxml.jackson` → `tools.jackson` 패키지 사용

### 빌드 검증
- 각 Task 완료 전 반드시:
  1. 밸런스 검증 스크립트(`myrpg` 데이터 변경 시): `cd tools/balance && python3 verify_equipment.py` / `verify_monster.py` / `verify_skill.py` — `data-balance-guide.md §C-6` 참조
  2. `mvn test -pl {모듈명}`
  3. `mvn clean install -pl {모듈명} -am`
- `BUILD SUCCESS` 확인 후에만 Task 완료 처리

### 소스 수정 Task 완료 전 코드 정리 (필수)
- 미사용 import 제거 (와일드카드 import 금지)
- 미사용 변수/필드 제거
- 매직 넘버 상수화
- 중복 코드 메서드 추출
- 메서드 분리 (50줄 초과 시 필수)
- 불필요한 주석 제거 (`TODO`/`FIXME` 해결 후 제거)

---

## 4. 스티어링 파일 참조

중요 결정사항은 아래 스티어링 파일을 우선 참조합니다:

| 영역 | 파일 경로 |
|---|---|
| 코딩 스타일 | `.kiro/steering/coding/code-style.md` |
| 깃 워크플로우 | `.kiro/steering/workflow/git-workflow.md` |
| 빌드 검증 | `.kiro/steering/workflow/task-build-validation.md` |
| codegraph 우선 사용 | `.kiro/steering/workflow/codegraph-first.md` |
| 스펙 문서 규칙 | `.kiro/steering/project/spec-conventions.md` |
| POM 컨벤션 | `.kiro/steering/project/pom-conventions.md` |
| 기술 스택 | `.kiro/steering/project/tech-stack.md` |
| 밸런스 가이드 | `.kiro/steering/myrpg/data-balance-guide.md` |
| 모듈 템플릿 | `.kiro/steering/module/module-template.md` |
| 신규 모듈 가드 | `.kiro/steering/module/new-module-guard.md` |
| 배포 | `.kiro/steering/infra/deployment.md` |

---

## 5. MCP 툴 우선 사용

코드 탐색 시 아래 순서로 툴을 사용합니다:

1. **`codegraph_explore`** (MCP) — 1순위. 심볼 구조, 호출 관계, 의존성 파악 및 코드 읽기
2. `read_file` / `search_files` — codegraph로 부족한 세부 내용 확인 시에만 사용

### 서버 접속 및 DB 조회

- **Oracle Cloud 서버**: `oracle-cloud-ssh` MCP (`read-command`, `run-command`, `sftp-upload`, `sftp-download`)를 우선 사용
- **Oracle DB**: `oracle-db` MCP로 DB 연결 및 조회 (`connect`, `sql_run`, `schema_information`)를 우선 사용
<!-- End of clinerules -->