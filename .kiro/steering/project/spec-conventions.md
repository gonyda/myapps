---
inclusion: always
---

# Spec 문서 구조 규칙

## 폴더 구조

Spec 문서는 반드시 아래 구조를 따릅니다:

```
.kiro/specs/
└── {모듈명}/
    └── {순번}-{기능명}/
        ├── requirements.md
        ├── design.md
        └── tasks.md
```

### 예시

```
.kiro/specs/
└── myrpg/
    ├── 001-character-progress-and-map-movement/
    │   ├── requirements.md
    │   ├── design.md
    │   └── tasks.md
    └── 002-npc-system/
        ├── requirements.md
        ├── design.md
        └── tasks.md
```

## 네이밍 규칙

- **모듈명**: 기존 Maven 모듈명과 동일 (예: `myrpg`, `mycalendar`, `mystudy`)
  - 모듈에 종속되지 않는 공통 기능은 `common` 사용
- **순번**: 3자리 0-padded 숫자 (예: `001`, `002`, `010`, `099`)
  - 해당 모듈 내에서 스펙이 생성된 순서대로 부여
  - 기존 스펙 폴더를 확인하여 다음 순번 결정
- **기능명**: kebab-case 영문 소문자 (예: `battle-system`, `npc-actions`)

## 순번 결정 방법

새 스펙 생성 시, 아래 순서로 순번을 결정합니다:

1. `.kiro/specs/{모듈명}/` 디렉터리 존재 여부 확인
2. 존재하면 기존 하위 폴더의 최대 순번 파악
3. 최대 순번 + 1을 새 순번으로 사용
4. 디렉터리가 없거나 비어 있으면 `001`부터 시작

## 적용 범위

- 이 규칙은 모든 신규 Spec 생성에 예외 없이 적용됩니다
- 기존에 생성된 스펙 폴더는 소급 적용하지 않습니다
