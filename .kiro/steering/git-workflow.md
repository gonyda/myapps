---
inclusion: always
---

# Git Workflow

## 브랜치 전략

```
main          # 항상 배포 가능한 안정 브랜치
feature/*     # 새 기능 개발 (예: feature/add-mysender-api)
fix/*         # 버그 수정 (예: fix/sender-null-pointer)
hotfix/*      # 긴급 프로덕션 수정
chore/*       # 빌드, 설정, 의존성 업데이트 등
```

- `main` 브랜치에 직접 push 금지
- 코드 변경 작업 시작 전, 브랜치를 새로 생성할지 사용자에게 먼저 물어봅니다
  - 사용자가 브랜치 생성을 원하면: 적절한 브랜치명을 제안하고 생성 후 작업 진행
  - 사용자가 현재 브랜치에서 작업을 원하면: 브랜치 생성 없이 진행
- 브랜치를 생성한 경우, 변경은 PR을 통해 병합

## 커밋 메시지 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 형식 사용, **메시지는 한글로 작성**:

```
<type>(<scope>): <한글 설명>

[optional body - 한글]

[optional footer]
```

**type 목록:**
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `style`: 코드 포맷, 세미콜론 등 (기능 변경 없음)
- `refactor`: 리팩토링
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, 의존성 관련
- `ci`: CI/CD 설정 변경

**scope 예시:** `mysender`, `root-pom`, `deps`, `ci`

**예시:**
```
feat(mysender): 메시지 전송 API 엔드포인트 추가
fix(mysender): 메시지 본문 null 처리 오류 수정
chore(deps): spring-boot 4.0.1로 버전 업그레이드
chore: 프로젝트 초기 설정 및 스펙 문서 추가
```

## PR 규칙

- PR 제목은 커밋 메시지 컨벤션과 동일하게 작성
- PR 설명에 변경 내용 요약, 테스트 방법 포함
- 최소 1명 리뷰 승인 후 병합
- merge 방식: Squash and Merge 권장 (히스토리 정리)

## GitHub 레포지토리 설정

- 레포지토리명: `myapps`
- Visibility: Private (기본)
- Default branch: `main`
- Branch protection: `main` 브랜치에 직접 push 금지 설정 권장
