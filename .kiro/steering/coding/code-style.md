---
inclusion: always
---

# Code Style

## Java 코딩 컨벤션

- **언어**: Java 25, 최신 언어 기능 적극 활용 (record, sealed class, pattern matching 등)
- **`var` 사용 금지**: 지역 변수 타입 추론(`var`) 사용하지 않음 — 항상 명시적 타입 선언
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
- **`@Autowired` 사용 절대 금지**: 필드 주입, 세터 주입, 생성자 주입 어디에도 `@Autowired` 어노테이션을 사용하지 않음
- 의존성 주입은 반드시 **순수 생성자 주입** 방식만 사용 (Spring은 단일 생성자일 때 자동으로 DI 수행)
- 생성자가 2개 이상인 경우에도 `@Autowired` 대신 Spring이 사용할 생성자를 기본 생성자로 설계하거나 팩토리 메서드 패턴 사용

## final 키워드 사용 정책

- **메서드 파라미터**: 모든 파라미터에 `final` 선언 필수
- **지역 변수**: 재할당하지 않는 지역 변수에 `final` 선언 필수
- **필드**: 별도 강제 없음 (불변 필드는 자연스럽게 `final` 사용)

```java
// 좋은 예
public void sendMessage(final String receiverId, final String message) {
    final MessageResult result = messageClient.send(receiverId, message);
    final long startTime = System.currentTimeMillis();

    // 재할당이 필요한 변수는 final 붙이지 않음
    int retryCount = 0;
    while (!result.isSuccess() && retryCount < MAX_RETRY_COUNT) {
        retryCount++;
    }
}
```

## Lombok 사용 정책

- **Lombok 사용 절대 금지**: `@Getter`, `@Setter`, `@Builder`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` 등 모든 Lombok 어노테이션 사용 불가
- `lombok` 의존성을 pom.xml에 추가하지 않음
- DTO/VO는 `record` 타입 사용 권장 (Java 25 record로 대체)
- 불변 도메인 객체도 `record` 타입 권장
- getter/setter가 필요한 경우 직접 작성

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

### Given-When-Then 패턴 작성 필수 (BDD 스타일)

**모든 테스트 메서드는 반드시 `// given`, `// when`, `// then` 3단계 주석으로 명확히 구분하여 작성합니다.**
이는 정상 동작뿐 아니라 경계값(Edge Case), 비정상 입력, 예외 상황 등 **다양하고 풍부한 테스트 케이스 생성을 유도**하기 위한 필수 규칙입니다.

```java
@Test
@DisplayName("유효한 유저 ID와 메시지가 주어졌을 때 전송에 성공한다")
void should_sendSuccessfully_when_validUserAndMessage() {
    // given (준비: 입력 데이터, Mock 동작 및 사전 조건 설정)
    final String receiverId = "user-123";
    final String message = "Hello World";
    given(messageClient.send(receiverId, message)).willReturn(MessageResult.success());

    // when (실행: 테스트 대상 핵심 단일 행위 호출)
    final boolean isSent = messageSenderService.sendMessage(receiverId, message);

    // then (검증: 결과 단언, 예외 검증, Mock 인터랙션 확인)
    assertThat(isSent).isTrue();
    then(messageClient).should().send(receiverId, message);
}
```

#### Given-When-Then 작성 가이드
1. **Given (사전 조건/준비)**:
   - 정상 케이스뿐 아니라 null/빈 문자열, 음수, 경계값(0, Max), 비인가 상태 등 다양한 상태를 구체적으로 설정
2. **When (행위/실행)**:
   - 검증하려는 단일 작업(메서드 호출)만 깔끔하게 실행
3. **Then (사후 결과/검증)**:
   - `assertThat` (AssertJ) 또는 `assertThatThrownBy` 등을 활용하여 엄격하게 결과 검증

| 대상 | 테스트 유형 | 어노테이션 |
|---|---|---|
| 서비스 클래스 | 단위 테스트 | `@ExtendWith(MockitoExtension.class)` |
| 서비스 클래스 | Property-Based 테스트 | jqwik `@Property` + `Mockito.mock()` |
| 컨트롤러 | 슬라이스 테스트 | `@WebMvcTest` |
| 레포지토리 | 슬라이스 테스트 | `@DataJpaTest` |
| 전체 컨텍스트 | 통합 테스트 | `@SpringBootTest` |

### jqwik Property-Based 테스트에서의 Mock 사용

- jqwik은 자체 테스트 엔진을 사용하므로 `@ExtendWith(MockitoExtension.class)`와 호환되지 않음
- **`@Mock` 어노테이션 사용 금지** — 대신 `Mockito.mock()`을 직접 호출하여 mock 생성
- 인터페이스의 스텁 클래스를 직접 구현하지 않음 (JpaRepository 등 메서드가 많은 인터페이스는 코드가 불필요하게 길어짐)

```java
// ✅ jqwik에서 올바른 mock 사용
@Property(tries = 100)
void myProperty(@ForAll("dataProvider") final List<MyEntity> data) {
    final MyRepository mockRepository = mock(MyRepository.class);
    when(mockRepository.findAll()).thenReturn(data);
    final MyService service = new MyService(mockRepository);
    // ... property 검증
}

// ❌ 스텁 클래스 직접 구현 금지
private static class StubMyRepository implements MyRepository { ... }
```

### Spring Boot 4.0 테스트 변경사항

Spring Boot 4.0에서 테스트 인프라가 모듈화되었습니다. 아래 규칙을 반드시 따릅니다.

#### @MockBean → @MockitoBean 마이그레이션

- `@MockBean`, `@SpyBean`은 **제거됨** — 사용 금지
- 대신 `@MockitoBean`, `@MockitoSpyBean` 사용
- import: `org.springframework.test.context.bean.override.mockito.MockitoBean`

```java
// ❌ Spring Boot 3.x (제거됨)
import org.springframework.boot.test.mock.mockito.MockBean;

// ✅ Spring Boot 4.0
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MyController.class)
class MyControllerTest {
    @MockitoBean
    private MyService myService;
}
```

#### @WebMvcTest 패키지 변경

- 슬라이스 테스트에 `spring-boot-starter-webmvc-test` 의존성 필요 (모듈 pom.xml에 test scope)
- import: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`

```java
// ❌ Spring Boot 3.x (제거됨)
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// ✅ Spring Boot 4.0
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

#### @DataJpaTest 패키지 변경

- 슬라이스 테스트에 `spring-boot-starter-data-jpa-test` 의존성 필요 (모듈 pom.xml에 test scope)
- import: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- `TestEntityManager` import: `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`
- 생성자 주입 사용 시 `@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)` 필요

```java
// ❌ Spring Boot 3.x (제거됨)
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// ✅ Spring Boot 4.0
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
```

```xml
<!-- 모듈 pom.xml에 추가 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Jackson 3 (ObjectMapper)

- Jackson 3에서 패키지가 `com.fasterxml.jackson` → `tools.jackson`으로 변경
- import: `tools.jackson.databind.ObjectMapper`

```java
// ❌ Jackson 2 (더 이상 기본 제공 안 됨)
import com.fasterxml.jackson.databind.ObjectMapper;

// ✅ Jackson 3
import tools.jackson.databind.ObjectMapper;
```

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
- **메서드 분리**: 한 메서드 안에 코드가 길어지면 반드시 리팩토링하여 메서드를 분리한다 (20줄 초과 시 분리 권장, 50줄 초과 시 분리 필수)
- **단일 책임**: 메서드가 하나의 일만 하는지 확인
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
