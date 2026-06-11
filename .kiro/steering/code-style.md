---
inclusion: always
---

# Code Style

## Java 코딩 컨벤션

- **언어**: Java 25, 최신 언어 기능 적극 활용 (record, sealed class, pattern matching 등)
- **인코딩**: UTF-8
- **들여쓰기**: 스페이스 4칸 (탭 금지)
- **최대 줄 길이**: 120자
- **클래스명**: PascalCase (예: `MessageSender`)
- **메서드/변수명**: camelCase (예: `sendMessage`, `messageId`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`)
- **패키지명**: 소문자, 점 구분 (예: `com.myapps.web.mysender.domain.service`)

## JavaDoc 주석 규칙

모든 클래스와 메서드에는 반드시 JavaDoc 주석을 작성합니다.

### 클래스 JavaDoc

```java
/**
 * 메시지 전송을 담당하는 서비스 클래스.
 *
 * <p>외부 시스템으로의 메시지 전송 및 재시도 로직을 처리합니다.
 */
@Service
public class MessageSenderService { ... }
```

### 메서드 JavaDoc

```java
/**
 * 지정된 수신자에게 메시지를 전송합니다.
 *
 * @param receiverId 메시지를 받을 수신자 ID
 * @param message    전송할 메시지 내용
 * @return 전송 성공 여부
 * @throws MessageSendException 전송 실패 시
 */
public boolean sendMessage(String receiverId, String message) { ... }
```

### 규칙

- **클래스**: 역할과 책임을 한 줄 요약 + 필요 시 `<p>` 태그로 상세 설명 추가
- **public 메서드**: 반드시 `@param`, `@return`, `@throws` 태그 포함 (해당하는 경우)
- **private 메서드**: 로직이 복잡하거나 비직관적인 경우에만 JavaDoc 작성
- **record/DTO**: 클래스 수준 JavaDoc으로 용도 설명, 필드별 주석은 생략 가능
- 단순히 메서드명을 반복하는 주석 금지 (예: `sendMessage 메서드` → ❌)

## Spring 어노테이션 규칙

- 컨트롤러: `@RestController` + `@RequestMapping`
- 서비스: `@Service`
- 레포지토리: `@Repository`
- 설정: `@Configuration`
- 생성자 주입 방식 사용 (필드 주입 `@Autowired` 금지)

## Lombok 사용 정책

- Lombok **사용하지 않음** (Java 25 record로 대체)
- DTO/VO는 `record` 타입 사용 권장
- 불변 도메인 객체도 `record` 타입 권장

## 예외 처리

- 비즈니스 예외는 커스텀 예외 클래스로 명시적 처리
- `RuntimeException` 직접 사용 금지
- `@ControllerAdvice`를 통한 전역 예외 처리 구성
- 빈 catch 블록 금지 — 최소한 로깅 또는 재throw 필요

## 테스트 작성 기준

- 테스트 프레임워크: JUnit 5 (`spring-boot-starter-test` 내장)
- 테스트 클래스명: `{대상클래스명}Test`
- 테스트 메서드명: `should_{기대동작}_when_{조건}` 형식 권장
- 단위 테스트와 통합 테스트 분리

| 대상 | 테스트 유형 | 어노테이션 |
|---|---|---|
| 서비스 클래스 | 단위 테스트 | `@ExtendWith(MockitoExtension.class)` |
| 컨트롤러 | 슬라이스 테스트 | `@WebMvcTest` |
| 레포지토리 | 슬라이스 테스트 | `@DataJpaTest` |
| 전체 컨텍스트 | 통합 테스트 | `@SpringBootTest` |

> Task 완료 전 테스트 실행 및 빌드 검증 기준은 `task-build-validation.md` 참고

## 소스 수정 Task 완료 전 코드 정리 (필수)

소스 코드(`.java`)를 추가하거나 수정하는 모든 Task는 완료 처리 전에 반드시 아래 항목을 수행합니다.

### 정리 항목

- **미사용 import 제거**: 참조되지 않는 import 문 전부 제거, 와일드카드 import 금지
- **미사용 변수 제거**: 선언 후 읽히지 않는 지역 변수, private 필드 제거
- **명확한 네이밍**: 변수/메서드명이 의도를 충분히 설명하는지 확인
  - 나쁜 예: `d`, `tmp`, `flag`, `data`
  - 좋은 예: `elapsedDays`, `retryCount`, `isMessageSent`
- **매직 넘버 상수화**: 의미 없는 숫자 리터럴은 `private static final` 상수로 추출
- **중복 코드 제거**: 동일 로직이 2회 이상 반복되면 메서드로 추출
- **단일 책임**: 메서드가 하나의 일만 하는지 확인 (50줄 초과 시 분리 검토)
- **불필요한 주석 제거**: 코드가 충분히 명확하면 주석 불필요, `TODO`/`FIXME`는 해결 후 제거

### 완료 전 체크리스트

- [ ] 미사용 import가 없는가?
- [ ] 미사용 지역 변수/필드가 없는가?
- [ ] 매직 넘버가 상수로 치환되었는가?
- [ ] 메서드 길이가 적절한가? (50줄 이하 권장)
- [ ] 의미 없는 주석이 제거되었는가?
- [ ] 빈 catch 블록이 없는가?

### 적용 제외

POM 수정만, 설정 파일(`.yml`, `.properties`)만, 디렉터리 구조 생성만 포함된 Task는 제외합니다.
