---
inclusion: always
---

# Task Build Validation & Quality Guardrails

## 규칙: 모든 Task는 5대 가드레일 검증 및 빌드 성공으로 완료해야 합니다

**Task 구현 및 소스 코드 작성이 끝날 때마다, Task 완료 처리 전에 반드시 아래 검증 명령어를 실행하여 성공을 확인해야 합니다.**

### 5대 품질 가드레일 및 검증 파이프라인

1. **Spotless (`spotless-maven-plugin`)**: Java 소스 포맷팅 자동 교정 및 미사용 import 정리
2. **Error Prone (`maven-compiler-plugin`)**: 정적 결함 및 잠재적 버그 컴파일 타임 즉시 차단
3. **ArchUnit (`archunit-junit5`)**: 계층형 아키텍처 규칙 및 의존성 방향 검증
4. **JaCoCo (`jacoco-maven-plugin`)**: 테스트 실행 계측 및 커버리지 80% 기준 검증 (라인 80%, 브랜치 70%)
5. **PMD & CPD (`maven-pmd-plugin`)**: 복잡도/메서드 크기/안티패턴 검증 및 중복 코드 검출
6. **CodeGraph Sync (`codegraph sync`)**: 지식 그래프 인덱스 최신 동기화

---

### 빌드 및 검증 명령 (초경량 스마트 Tail 적용)

> 💡 **토큰 및 터미널 최적화**: 모든 테스트/빌드 로그를 백그라운드로 처리하고 최종 `BUILD SUCCESS` 요약표만 깔끔하게 출력합니다. 만약 빌드가 실패할 경우 실패 에러 로그(30줄)를 즉시 출력하고 중단합니다.

Task가 특정 모듈(예: `myrpg`)에 대한 작업이라면:

```bash
mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync
```

Task가 루트 프로젝트(Parent POM, .gitignore, 전체 공통 등) 설정에 대한 작업이라면:

```bash
mvn -B -q spotless:apply && (mvn -B clean install > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync
```

---

### 성공/실패 처리 기준

| 단계 | 결과 | Task 처리 |
|---|---|---|
| 전체 파이프라인 | `BUILD SUCCESS` & `codegraph sync Done` | Task 완료(completed)로 처리 |
| Spotless / 컴파일 / 테스트 / JaCoCo / PMD | `BUILD FAILURE` | Task 실패 — 터미널에 출력된 에러 원인 분석 후 수정 및 재검증 |

### 빌드 실패 시 행동 지침

1. 터미널에 출력된 실패 에러 메시지 분석
2. Spotless 포맷팅 위반 여부 확인 (`mvn spotless:apply`로 자동 해결)
3. Error Prone 컴파일 경고/오류 메시지 분석 및 버그 수정
4. ArchUnit 아키텍처 위반(계층 침범 등), JaCoCo 80% 커버리지 미달, 또는 단위 테스트 실패 수정
5. PMD 복잡도 초과 또는 CPD 중복 코드 경고 분석 및 리팩토링
6. 검증 명령어 재실행 후 `BUILD SUCCESS` 및 `codegraph sync` 확인 시 완료 처리

---

## 규칙: 기능 추가 시 테스트 코드 필수 작성 (Given-When-Then 패턴 강제)

**기능 구현을 포함하는 Task는 반드시 해당 기능에 대한 테스트 코드를 함께 작성해야 합니다.**

- 새로운 클래스(서비스, 컨트롤러, 레포지토리 등)를 추가할 때는 반드시 테스트 클래스도 함께 작성
- **Given-When-Then 필수 준수**: 모든 테스트는 `// given`, `// when`, `// then` 3단계를 명시하여 작성 (정상값뿐 아니라 경계값, null/빈값, 예외 등 다양한 테스트 케이스 생성 유도)
- 테스트 작성 기준(어노테이션, 네이밍, BDD 스타일 등)은 `code-style.md` 참고
- 테스트 없이 구현만 완료한 Task는 완료 처리 불가

---

## 규칙: 모든 Task는 필수입니다

**tasks.md에 정의된 모든 Task는 예외 없이 필수(mandatory)입니다.**

- Optional Task라는 개념은 존재하지 않습니다
- Task를 건너뛰거나 사용자에게 "optional이니 스킵할까요?"라고 물어보는 것은 허용되지 않습니다
- 모든 Task는 동일한 우선순위로 순서대로 완료해야 합니다
