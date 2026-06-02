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
- **패키지명**: 소문자, 점 구분 (예: `com.myapps.mysender.service`)

## Spring 레이어 구조

모듈 유형에 따라 아래 구조를 따릅니다.

**Web 모듈:**
```
com.myapps.{module}/
├── {Module}Application.java   # 메인 클래스
├── controller/                # REST 컨트롤러 (웹 레이어)
├── service/                   # 비즈니스 로직
├── repository/                # 데이터 접근 레이어
├── domain/                    # 도메인 모델 (Entity, VO 등)
├── dto/                       # 데이터 전송 객체
└── config/                    # 설정 클래스
```

**Batch 모듈:**
```
com.myapps.{module}/
├── {Module}Application.java   # 메인 클래스
├── job/                       # Job 설정 클래스
├── step/                      # Step 설정 클래스
├── tasklet/                   # Tasklet 구현체
├── reader/                    # ItemReader 구현체
├── processor/                 # ItemProcessor 구현체
├── writer/                    # ItemWriter 구현체
└── config/                    # 설정 클래스
```

## Spring 어노테이션 규칙

- 컨트롤러: `@RestController` + `@RequestMapping`
- 서비스: `@Service`
- 레포지토리: `@Repository`
- 설정: `@Configuration`
- 생성자 주입 방식 사용 (필드 주입 `@Autowired` 금지)

## 테스트 작성 기준

- 테스트 프레임워크: JUnit 5 (`spring-boot-starter-test` 내장)
- 테스트 클래스명: `{대상클래스명}Test`
- 테스트 메서드명: `should_{기대동작}_when_{조건}` 형식 권장
- 단위 테스트와 통합 테스트 분리
  - 단위 테스트: `@ExtendWith(MockitoExtension.class)`
  - 통합 테스트: `@SpringBootTest`

## Lombok 사용 정책

- Lombok **사용하지 않음** (Java 25 record로 대체)
- DTO/VO는 `record` 타입 사용 권장
- 불변 도메인 객체도 `record` 타입 권장

## 예외 처리

- 비즈니스 예외는 커스텀 예외 클래스로 명시적 처리
- `RuntimeException` 직접 사용 금지
- `@ControllerAdvice`를 통한 전역 예외 처리 구성
