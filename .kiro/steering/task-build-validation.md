---
inclusion: always
---

# Task Build Validation

## 규칙: 모든 Task는 빌드 성공으로 완료해야 합니다

**Task 구현이 끝날 때마다, Task 완료 처리 전에 반드시 해당 모듈을 빌드하여 성공을 확인해야 합니다.**

### 빌드 명령

Task가 특정 모듈(예: `mysender`)에 대한 작업이라면:

```bash
mvn clean install -pl {modulename} -am
```

- `-pl {modulename}`: 해당 모듈만 빌드
- `-am`: 해당 모듈이 의존하는 upstream 모듈도 함께 빌드 (의존성 해결)

Task가 루트 프로젝트(Parent POM, .gitignore 등) 설정에 대한 작업이라면:

```bash
mvn clean install
```

### 성공/실패 처리 기준

| 빌드 결과 | Task 처리 |
|---|---|
| `BUILD SUCCESS` | Task 완료(completed)로 처리 |
| `BUILD FAILURE` | Task 실패 — 오류 수정 후 재빌드, 성공 전까지 완료 처리 금지 |

### 빌드 실패 시 행동 지침

1. Maven 오류 메시지를 분석하여 원인 파악
2. 오류 수정 (컴파일 오류, 의존성 누락, 설정 오류 등)
3. 빌드 재실행
4. `BUILD SUCCESS` 확인 후 Task 완료 처리

### 적용 범위

- 이 규칙은 모든 Task에 예외 없이 적용됩니다
- 파일 생성, POM 수정, 소스 코드 작성 등 어떤 종류의 Task든 동일하게 적용됩니다
- 빌드 확인 없이 Task를 완료 처리하는 것은 허용되지 않습니다

---

## 규칙: 기능 추가 시 테스트 코드 필수 작성

**기능 구현을 포함하는 Task는 반드시 해당 기능에 대한 테스트 코드를 함께 작성해야 합니다.**

- 새로운 클래스(서비스, 컨트롤러, 레포지토리 등)를 추가할 때는 반드시 테스트 클래스도 함께 작성
- 테스트 작성 기준(어노테이션, 네이밍 등)은 `code-style.md` 참고
- 테스트 없이 구현만 완료한 Task는 완료 처리 불가

### 테스트 + 빌드 실행 순서

Task 완료 전 아래 순서로 반드시 실행:

1. **테스트 실행**:
   ```bash
   mvn test -pl {modulename}
   ```

2. **빌드 실행**:
   ```bash
   mvn clean install -pl {modulename} -am
   ```

| 결과 | Task 처리 |
|---|---|
| 테스트 통과 + 빌드 성공 | Task 완료(completed)로 처리 |
| 테스트 실패 | 테스트 수정 후 재실행, 완료 처리 금지 |
| 테스트 미작성 | 테스트 작성 후 실행, 완료 처리 금지 |
| 빌드 실패 | 오류 수정 후 재빌드, 완료 처리 금지 |

### 예외

아래 경우는 테스트 작성 없이 빌드 검증만으로 완료 처리 가능:
- POM 파일 수정만 포함된 Task (의존성 추가, 플러그인 설정 등)
- `.gitignore`, `application.yml` 등 설정 파일만 수정하는 Task
- 디렉터리 구조 생성만 포함된 Task
