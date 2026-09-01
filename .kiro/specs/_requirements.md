# Requirements Document: {기능명}

> **폴더 위치 가이드**: `.kiro/specs/{모듈명}/{3자리순번}-{기능명}/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: 현재 시스템의 구조와 왜 이 기능(또는 개선)이 필요한지 배경을 기술합니다.
- **핵심 목표**: 본 스펙을 통해 달성하고자 하는 비즈니스/게임 플레이 가치를 기술합니다.
- **선행 스펙과의 연계**: 이전 스펙(예: `00X-xxx`)에서 구현된 기반 및 본 스펙이 확장하는 지점을 명시합니다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **{주요 기능 1}**: 세부 범위 요약
2. **{주요 기능 2}**: 세부 범위 요약
3. **{주요 기능 3}**: 세부 범위 요약
4. **{데이터 및 카탈로그}**: 추가/변경되는 JSON 데이터 및 정적 리소스
5. **{영속 모델}**: 추가/변경되는 JPA 엔티티 및 DB 상태

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **{이연 항목 1}**: 본 스펙에서 다루지 않고 향후 스펙(예: `0XX`)으로 넘기는 항목 및 이유
- **{이연 항목 2}**: 추후 확장 예정인 인터페이스/훅의 범위 한정

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **{Module_Name}**: `{package.path}` 패키지의 Spring Boot 4.0 모듈.
- **{Existing_Entity}**: 기존 도메인 엔티티 또는 DTO에 대한 간략한 정의.
- **{Existing_Service}**: 기존 핵심 서비스의 역할.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **{New_Domain_Concept}**: 본 스펙에서 새롭게 도입되는 핵심 도메인 개념 정의.
- **{New_Entity_Name}**: DB에 영속 저장되는 신규 엔티티의 정의 및 저장 필드 요약.
- **{New_Catalog_Data}**: `classpath:data/{name}.json`으로 로드되는 정적 카탈로그 데이터.
- **{New_Enum_Type}**: 상태나 분류를 정의하는 열거형 타입 상수들.

---

## 3. Requirements (기능 요구사항)

> **EARS 키워드 가이드** (Acceptance Criteria 작성 시 정확히 구분):
> - **WHEN** {이벤트/트리거} — 특정 사건이 발생했을 때의 동작 (event-driven)
> - **IF ... THEN** {조건} — 원치 않는 상태/오류 조건에 대한 분기 처리 (unwanted behavior)
> - **WHILE** {지속 상태} — 특정 상태가 유지되는 동안의 동작 (state-driven)
> - **WHERE** {기능 포함 조건} — 선택적 기능이 포함된 구성에서만 적용 (optional feature)
> - 모든 문장은 `THE {주체} SHALL {동작}` 형태로 검증 가능한 단일 동작을 기술합니다.

### Requirement 1: {기능 그룹 명칭 1}

**User Story:**  
{사용자/플레이어/시스템}로서, {수행하고자 하는 행동}을 하고 싶다.  
그래야 {얻고자 하는 이익/목적/가치}할 수 있다.

#### Acceptance Criteria

1. **WHEN** {특정 이벤트 또는 요청이 발생하면}, **THE** {주체 시스템} **SHALL** {반드시 수행해야 하는 동작을 실행한다}.
2. **IF** {특정 조건이 참이면}, **THEN THE** {주체 시스템} **SHALL** {해당 분기 동작을 수행한다}.
3. **WHERE** {특정 상태 또는 예외 조건 하에서}, **THE** {주체 시스템} **SHALL** {방어적/관용적 처리를 유지한다}.
4. **THE** {주체 시스템} **SHALL** {유효하지 않은 입력에 대해 명확한 오류 반환 또는 폴백 동작을 보장한다}.
5. **THE** {주체 시스템} **SHALL** {데이터 무결성 및 불변식 조건을 만족한다}.

---

### Requirement 2: {기능 그룹 명칭 2}

**User Story:**  
{사용자/플레이어/시스템}로서, {수행하고자 하는 행동}을 하고 싶다.  
그래야 {얻고자 하는 이익/목적/가치}할 수 있다.

#### Acceptance Criteria

1. **WHEN** {조건}, **THE** {시스템} **SHALL** {결과}.
2. **THE** {시스템} **SHALL** {동작 상세}.
3. **IF** {실패 조건}, **THEN THE** {시스템} **SHALL** {오류 처리 및 롤백 정책}.

---

### Requirement 3: {예외 처리 및 특수 규칙}

**User Story:**  
{사용자/플레이어/시스템}로서, {비정상적이거나 엣지 케이스 상황}에서도 안정적인 동작을 보장받고 싶다.  
그래야 {데이터 유실이나 시스템 중단을 방지}할 수 있다.

#### Acceptance Criteria

1. **IF** {자원 부족 / 타임아웃 / 비정상 접근 상황이 발생하면}, **THEN THE** {시스템} **SHALL** {명시된 예외 규정대로 처리하고 데이터 무결성을 유지한다}.
2. **WHILE** {비정상 상태가 지속되는 동안}, **THE** {시스템} **SHALL** {안전한 폴백 상태를 유지한다}.
3. **THE** {시스템} **SHALL** 불필요한 브라우저 `alert` 팝업을 띄우지 않고 하단 활동 로그에 피드백을 기록한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 아키텍처 규칙 준수.
   - **JaCoCo**: 신규 및 변경 코드 대상 테스트 커버리지 **라인 80% 이상, 브랜치 70% 이상** 달성.
   - **PMD & CPD**: 복잡도(`CognitiveComplexity`), 안티패턴 및 중복 코드 0건.
2. **데이터 무결성 및 밸런스 검증**:
   - JSON 카탈로그 추가/수정 시 밸런스 검증 스크립트(`tools/balance/`) 통과.
3. **CodeGraph 동기화**:
   - 코드베이스 변경 후 `codegraph sync` 필수 수행.
